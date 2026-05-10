package org.example.timecoinweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(120))
            .writeTimeout(Duration.ofSeconds(30))
            .build();

    @Value("${ai.chat.enabled:false}")
    private boolean enabled;

    @Value("${ai.chat.base-url:https://llmapi.tongji.edu.cn/v1}")
    private String baseUrl;

    @Value("${ai.chat.api-url:}")
    private String apiUrl;

    @Value("${ai.chat.api-key:}")
    private String apiKey;

    @Value("${ai.chat.model:DeepSeek-R1}")
    private String model;

    @Value("${ai.chat.temperature:0.7}")
    private double temperature;

    @Value("${ai.chat.top-p:0.8}")
    private double topP;

    @Value("${ai.chat.repetition-penalty:1.05}")
    private double repetitionPenalty;

    @Value("${ai.chat.max-tokens:2048}")
    private int maxTokens;

    public String chat(String role, String message) {
        String normalized = message == null ? "" : message.trim();
        if (!StringUtils.hasText(normalized)) {
            return "请先输入想咨询的问题。";
        }
        if (isHumanServiceQuestion(normalized)) {
            return humanServiceUnavailableReply();
        }
        if (!enabled || !StringUtils.hasText(apiKey) || "your_api_key".equals(apiKey.trim())) {
            return fallbackReply(role, normalized);
        }
        try {
            return callRemoteWithRetry(role, normalized);
        } catch (Exception e) {
            return "AI服务暂时不可用，请稍后重试。如遇紧急医疗、人身安全或支付风险，请联系家属、社区工作人员或当地应急渠道。";
        }
    }

    public Map<String, Object> generateActivityDraft(String text) {
        String normalized = text == null ? "" : text.trim();
        if (!StringUtils.hasText(normalized)) {
            return fallbackActivityDraft("需要志愿者协助");
        }
        if (!canCallRemote()) {
            return fallbackActivityDraft(normalized);
        }
        String system = "你是时间银行平台的老人端活动发布助手。根据老人自然语言需求，生成可填写到活动发布表单的草稿。"
                + "只输出JSON，不要Markdown。字段必须包含title、quota、description、volunteerReward、suggestions、warnings。"
                + "quota为1到10的整数，volunteerReward为0到20的整数，suggestions和warnings为字符串数组。"
                + "不要编造具体日期、时间、地址或电话；如果原文没有明确说明，在suggestions中提醒老人补充。";
        try {
            Map<String, Object> data = parseJsonObject(callRemoteWithPrompt(system, normalized));
            return normalizeActivityDraft(data, normalized);
        } catch (Exception e) {
            return fallbackActivityDraft(normalized);
        }
    }

    public Map<String, Object> generateVolunteerPreparation(Map<String, Object> activity) {
        Map<String, Object> safeActivity = activity == null ? new HashMap<>() : activity;
        if (!canCallRemote()) {
            return fallbackVolunteerPreparation(safeActivity);
        }
        String system = "你是时间银行平台的志愿者服务准备助手。根据活动信息生成服务前准备建议。"
                + "只输出JSON，不要Markdown。字段必须包含summary、checklist、communicationTips、riskTips。"
                + "checklist、communicationTips、riskTips均为字符串数组，每项简洁可执行。";
        try {
            String user = "活动信息：" + objectMapper.writeValueAsString(safeActivity);
            Map<String, Object> data = parseJsonObject(callRemoteWithPrompt(system, user));
            return normalizePreparation(data, safeActivity);
        } catch (Exception e) {
            return fallbackVolunteerPreparation(safeActivity);
        }
    }

    public Map<String, Object> generateActivityReview(Map<String, Object> activity) {
        Map<String, Object> safeActivity = activity == null ? new HashMap<>() : activity;
        if (!canCallRemote()) {
            return fallbackActivityReview(safeActivity);
        }
        String system = "你是时间银行平台的管理员活动审核辅助助手。根据活动信息做审核风险提示。"
                + "只输出JSON，不要Markdown。字段必须包含summary、recommendation、issues、suggestedMessage。"
                + "recommendation只能是approve、revise或reject之一；issues为字符串数组；suggestedMessage是给发布人的简短审核建议。"
                + "不要替管理员做最终决定，只提供辅助判断。";
        try {
            String user = "活动信息：" + objectMapper.writeValueAsString(safeActivity);
            Map<String, Object> data = parseJsonObject(callRemoteWithPrompt(system, user));
            return normalizeReview(data, safeActivity);
        } catch (Exception e) {
            return fallbackActivityReview(safeActivity);
        }
    }

    private boolean canCallRemote() {
        return enabled && StringUtils.hasText(apiKey) && !"your_api_key".equals(apiKey.trim());
    }

    private boolean isHumanServiceQuestion(String message) {
        return message.contains("转人工")
                || message.contains("人工服务")
                || message.contains("人工客服")
                || message.contains("联系客服")
                || message.contains("客服")
                || message.contains("热线")
                || message.contains("客服电话")
                || message.contains("投诉");
    }

    private String humanServiceUnavailableReply() {
        return "当前版本暂不支持在线转人工服务，也没有内置客服电话入口。请联系线下社区工作人员或项目管理员处理。如遇紧急医疗、人身安全或支付风险，请立即联系家属、社区工作人员或当地应急渠道。";
    }

    private String callRemote(String role, String message) throws IOException {
        return callRemoteWithMessages(buildMessages(role, message));
    }

    private String callRemoteWithRetry(String role, String message) throws IOException {
        IOException firstError;
        try {
            return callRemote(role, message);
        } catch (IOException e) {
            firstError = e;
        }
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw firstError;
        }
        return callRemote(role, message);
    }

    private String callRemoteWithPrompt(String systemPrompt, String userPrompt) throws IOException {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);
        return callRemoteWithMessages(messages);
    }

    private String callRemoteWithMessages(List<Map<String, String>> messages) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("top_p", topP);
        body.put("repetition_penalty", repetitionPenalty);
        body.put("max_tokens", maxTokens);

        Request request = new Request.Builder()
                .url(resolveApiUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("AI request failed: " + response.code());
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || !StringUtils.hasText(content.asText())) {
                throw new IOException("AI response is empty");
            }
            return content.asText().trim();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String text) throws IOException {
        String normalized = text == null ? "" : text.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```json", "").replaceFirst("^```", "").replaceFirst("```$", "").trim();
        }
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            normalized = normalized.substring(start, end + 1);
        }
        return objectMapper.readValue(normalized, Map.class);
    }

    private String resolveApiUrl() {
        if (StringUtils.hasText(apiUrl)) {
            String normalizedApiUrl = apiUrl.trim();
            if (normalizedApiUrl.startsWith("http://") || normalizedApiUrl.startsWith("https://")) {
                return normalizedApiUrl;
            }
            String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String normalizedPath = normalizedApiUrl.startsWith("/") ? normalizedApiUrl : "/" + normalizedApiUrl;
            return normalizedBaseUrl + normalizedPath;
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/chat/completions";
    }

    private List<Map<String, String>> buildMessages(String role, String message) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", buildSystemPrompt(role));
        messages.add(system);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", message);
        messages.add(user);
        return messages;
    }

    private String buildSystemPrompt(String role) {
        String identity = "volunteer".equals(role) ? "志愿者" : "老人";
        return "你是时间银行平台的移动端AI助手，当前用户身份是" + identity
                + "。回答要简洁、温和、可执行，只围绕平台使用、活动报名、发布服务、签到、时间币、常见养老志愿服务咨询。"
                + "当前App不支持在线转人工、人工客服、客服热线、帮助与反馈、在线工单或客服按钮。"
                + "不要编造任何客服电话、400热线、客服入口、按钮、页面路径、工作时间或人工客服流程。"
                + "如果用户要求转人工、联系客服或索要客服电话，只能回答：当前版本暂不支持在线转人工服务，也没有内置客服电话入口，请联系线下社区工作人员或项目管理员处理。"
                + "遇到紧急医疗、人身安全、支付纠纷等高风险问题时，提醒用户联系家属、社区工作人员或当地应急渠道；不要提及平台人工客服。";
    }

    private Map<String, Object> normalizeActivityDraft(Map<String, Object> data, String sourceText) {
        Map<String, Object> result = fallbackActivityDraft(sourceText);
        copyText(data, result, "title");
        copyText(data, result, "description");
        copyNumber(data, result, "quota", 1, 10);
        copyNumber(data, result, "volunteerReward", 0, 20);
        copyList(data, result, "suggestions");
        copyList(data, result, "warnings");
        return result;
    }

    private Map<String, Object> normalizePreparation(Map<String, Object> data, Map<String, Object> activity) {
        Map<String, Object> result = fallbackVolunteerPreparation(activity);
        copyText(data, result, "summary");
        copyList(data, result, "checklist");
        copyList(data, result, "communicationTips");
        copyList(data, result, "riskTips");
        return result;
    }

    private Map<String, Object> normalizeReview(Map<String, Object> data, Map<String, Object> activity) {
        Map<String, Object> result = fallbackActivityReview(activity);
        copyText(data, result, "summary");
        copyText(data, result, "suggestedMessage");
        Object recommendation = data.get("recommendation");
        if (recommendation != null) {
            String value = recommendation.toString().trim();
            if ("approve".equals(value) || "revise".equals(value) || "reject".equals(value)) {
                result.put("recommendation", value);
            }
        }
        copyList(data, result, "issues");
        return result;
    }

    private void copyText(Map<String, Object> from, Map<String, Object> to, String key) {
        Object value = from.get(key);
        if (value != null && StringUtils.hasText(value.toString())) {
            to.put(key, value.toString().trim());
        }
    }

    private void copyNumber(Map<String, Object> from, Map<String, Object> to, String key, int min, int max) {
        Object value = from.get(key);
        if (value == null) {
            return;
        }
        try {
            int n = (int) Math.round(Double.parseDouble(value.toString()));
            if (n < min) n = min;
            if (n > max) n = max;
            to.put(key, n);
        } catch (NumberFormatException ignored) {
        }
    }

    private void copyList(Map<String, Object> from, Map<String, Object> to, String key) {
        Object value = from.get(key);
        if (!(value instanceof List)) {
            return;
        }
        List<String> items = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item != null && StringUtils.hasText(item.toString())) {
                items.add(item.toString().trim());
            }
        }
        if (!items.isEmpty()) {
            to.put(key, items);
        }
    }

    private Map<String, Object> fallbackActivityDraft(String text) {
        String title = "日常陪伴服务";
        String description = "希望志愿者协助完成相关服务，请按约定时间到达并耐心沟通。";
        if (text.contains("医院") || text.contains("陪诊") || text.contains("看病")) {
            title = "陪诊协助";
            description = "陪同老人前往医院，协助取号、候诊、缴费、取药等流程，注意路途安全。";
        } else if (text.contains("手机") || text.contains("微信") || text.contains("挂号")) {
            title = "智慧手机使用指导";
            description = "帮助老人学习手机常用功能，包括微信沟通、线上挂号、扫码和常用生活服务。";
        } else if (text.contains("聊天") || text.contains("陪伴") || text.contains("读报")) {
            title = "陪伴聊天读报";
            description = "为老人读报、交流近期生活和社区信息，提供耐心陪伴。";
        } else if (text.contains("买药") || text.contains("买菜") || text.contains("代买")) {
            title = "生活物品代买协助";
            description = "协助老人购买药品或生活物品，提前确认清单、金额和交接方式。";
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", title);
        data.put("quota", 1);
        data.put("description", description + " 原始需求：" + text);
        data.put("volunteerReward", 0);
        data.put("suggestions", listOf("请补充准确活动地址", "请确认报名截止时间早于活动开始时间", "如涉及外出，请写清集合地点和联系人"));
        data.put("warnings", text.contains("疼") || text.contains("急") ? listOf("如涉及紧急医疗情况，请优先联系家属或急救渠道") : listOf("不要在描述中填写银行卡、验证码等敏感信息"));
        return data;
    }

    private Map<String, Object> fallbackVolunteerPreparation(Map<String, Object> activity) {
        String title = valueText(activity, "title", "该活动");
        String address = valueText(activity, "address", "活动地点");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", "本次服务为“" + title + "”，请提前确认时间、地点和老人需求。");
        data.put("checklist", listOf("提前10到15分钟到达" + address, "出发前确认手机电量和导航", "携带身份证件，按页面要求完成签到"));
        data.put("communicationTips", listOf("见面先自我介绍并确认老人称呼", "服务过程中放慢语速，耐心确认需求", "遇到变更及时联系老人或平台"));
        data.put("riskTips", listOf("不要代管银行卡、密码或验证码", "涉及医疗判断时建议老人咨询医生", "外出活动注意交通和人身安全"));
        return data;
    }

    private Map<String, Object> fallbackActivityReview(Map<String, Object> activity) {
        List<String> issues = new ArrayList<>();
        String title = valueText(activity, "title", "");
        String address = valueText(activity, "address", "");
        String description = valueText(activity, "description", "");
        if (!StringUtils.hasText(title)) issues.add("活动标题为空");
        if (!StringUtils.hasText(address) || address.length() < 6) issues.add("活动地址可能不够明确");
        if (!StringUtils.hasText(description) || description.length() < 10) issues.add("活动描述偏短，建议补充具体服务内容");
        if (description.contains("转账") || description.contains("验证码") || description.contains("银行卡")) {
            issues.add("描述中出现支付或敏感信息相关内容，需要人工重点核查");
        }
        String recommendation = issues.isEmpty() ? "approve" : "revise";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", "活动信息已完成基础检查，可结合平台规则继续审核。");
        data.put("recommendation", recommendation);
        data.put("issues", issues.isEmpty() ? listOf("未发现明显信息缺失") : issues);
        data.put("suggestedMessage", issues.isEmpty() ? "信息较完整，可审核通过。" : "请补充或修改活动信息后再提交审核。");
        return data;
    }

    private String valueText(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value == null || !StringUtils.hasText(value.toString()) ? defaultValue : value.toString().trim();
    }

    private List<String> listOf(String... values) {
        List<String> list = new ArrayList<>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }

    private String fallbackReply(String role, String message) {
        if (message.contains("报名") || message.contains("参加")) {
            return "你可以在活动列表中选择想参加的服务，进入详情页后点击报名。报名后可在个人中心查看已报名活动。";
        }
        if (message.contains("发布") || message.contains("发起")) {
            return "老人端可以在服务中心选择服务类型，填写活动信息、地点和时间后提交，等待平台审核。";
        }
        if (message.contains("签到")) {
            return "志愿者参加活动时可在已报名活动中进入签到页面，按页面提示完成定位签到。";
        }
        if (message.contains("时间币") || message.contains("积分")) {
            return "时间币用于记录服务贡献和互助权益。你可以在时间币详情页面查看余额和相关记录。";
        }
        if ("volunteer".equals(role)) {
            return "我是志愿者端AI助手，可以帮你了解活动报名、签到、服务记录和时间币问题。请描述你遇到的具体情况。";
        }
        return "我是老人端AI助手，可以帮你了解发布服务、查看活动、时间币和平台使用问题。请描述你需要的帮助。";
    }
}
