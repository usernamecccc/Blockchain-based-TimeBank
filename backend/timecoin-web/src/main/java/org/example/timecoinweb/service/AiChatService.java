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
        String guardedReply = guardrailReply(role, normalized);
        if (guardedReply != null) {
            return guardedReply;
        }
        String supportedReply = supportedFeatureReply(role, normalized);
        if (supportedReply != null) {
            return supportedReply;
        }
        if (!canCallRemote()) {
            return fallbackReply(role, normalized);
        }
        try {
            String reply = callRemoteWithRetry(role, normalized);
            String replyGuard = guardrailReplyForModelOutput(role, reply);
            return replyGuard == null ? reply : replyGuard;
        } catch (Exception e) {
            return "AI服务暂时不可用，请稍后重试。若遇到紧急医疗、人身安全或支付风险，请联系家属、社区工作人员或当地应急渠道。";
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
        String system = "你是“时光颐养”时间银行管理系统的老人端活动发布助手。"
                + "你只能根据老人提供的自然语言需求生成活动发布表单草稿。"
                + "当前表单支持字段：活动标题、活动名额、活动描述、每人答谢时间币。"
                + "不要编造具体日期、时间、地址、联系人、客服电话、客服入口或系统未实现功能。"
                + "如果原文没有说明日期、时间、地点，请放到suggestions中提醒用户补充。"
                + "只输出JSON，不要Markdown。字段必须包含title、quota、description、volunteerReward、suggestions、warnings。"
                + "quota为1到10的整数，volunteerReward为0到20的整数，suggestions和warnings为字符串数组。";
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
        String system = "你是“时光颐养”时间银行管理系统的志愿者端活动服务准备助手。"
                + "你只能根据传入的活动信息生成服务前准备建议。"
                + "不要编造活动页面没有提供的联系人、电话、客服入口、保险、派单、实时聊天或平台工作人员流程。"
                + "只输出JSON，不要Markdown。字段必须包含summary、checklist、communicationTips、riskTips。"
                + "checklist、communicationTips、riskTips均为字符串数组，每项简洁、可执行。";
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
        String system = "你是“时光颐养”时间银行管理系统的管理员端活动审核辅助助手。"
                + "你只能根据活动信息做审核风险提示，不替管理员做最终决定。"
                + "当前系统支持管理员查看活动信息、审核活动、填写管理员建议；不要编造自动审核通过、自动联系用户、短信通知、客服电话或工单流程。"
                + "只输出JSON，不要Markdown。字段必须包含summary、recommendation、issues、suggestedMessage。"
                + "recommendation只能是approve、revise或reject之一，issues为字符串数组，suggestedMessage是给发布人的简短审核建议。";
        try {
            String user = "活动信息：" + objectMapper.writeValueAsString(safeActivity);
            Map<String, Object> data = parseJsonObject(callRemoteWithPrompt(system, user));
            return normalizeReview(data, safeActivity);
        } catch (Exception e) {
            return fallbackActivityReview(safeActivity);
        }
    }

    private boolean canCallRemote() {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            return false;
        }
        String key = apiKey.trim();
        return !"your_api_key".equalsIgnoreCase(key) && !"YOUR_API_KEY".equals(key);
    }

    private String guardrailReply(String role, String message) {
        if (isHumanServiceQuestion(message)) {
            return humanServiceUnavailableReply();
        }
        if (isThirdPartyLoginQuestion(message)) {
            return "当前版本还没有实现微信、QQ、支付宝等外部账号登录。现在请使用系统内置账号登录/注册；如果后续要接入第三方登录，需要先完成开放平台应用申请、回调地址配置、后端换取openid/unionid和账号绑定表设计。";
        }
        if (isUnsupportedFeatureQuestion(message)) {
            return "当前版本暂未实现这个功能。已实现的移动端能力包括：账号登录注册、老人发布活动、志愿者浏览和报名活动、活动签到、个人信息查看、时间币余额/转账/转账历史、老人端和志愿者端AI服务。你可以告诉我你想完成的具体操作，我会按当前App已有页面给你指引。";
        }
        if (isEmergencyQuestion(message)) {
            return "如果涉及紧急医疗、人身安全或突发危险，请优先联系家属、社区工作人员或当地应急渠道。本AI只能说明App使用方法，不能替代医疗、法律或应急处置。";
        }
        return null;
    }

    private String guardrailReplyForModelOutput(String role, String reply) {
        if (!StringUtils.hasText(reply)) {
            return fallbackReply(role, "");
        }
        if (containsHumanServiceHallucination(reply)) {
            return humanServiceUnavailableReply();
        }
        if (containsUnsupportedLoginHallucination(reply)) {
            return "当前版本还没有实现微信、QQ、支付宝等外部账号登录。请使用系统内置账号登录/注册。";
        }
        return null;
    }

    private boolean isHumanServiceQuestion(String message) {
        return containsAny(message, "转人工", "人工服务", "人工客服", "联系客服", "客服", "热线", "客服电话", "投诉", "工单");
    }

    private boolean isThirdPartyLoginQuestion(String message) {
        return containsAny(message, "微信登录", "qq登录", "QQ登录", "支付宝登录", "第三方登录", "外部账号登录", "openid", "unionid");
    }

    private boolean isUnsupportedFeatureQuestion(String message) {
        return containsAny(message,
                "语音助手", "语音输入", "语音交互", "语音播报",
                "智能推荐", "自动推荐", "推荐算法",
                "在线聊天", "实时聊天", "视频", "视频通话",
                "短信验证码", "短信提醒", "消息推送",
                "实名认证", "人脸识别", "身份证认证",
                "社会捐赠", "捐赠点",
                "失信分", "信用分",
                "流通率预警", "预警功能",
                "自动派单", "派单",
                "退款", "提现");
    }

    private boolean isEmergencyQuestion(String message) {
        return containsAny(message, "急救", "晕倒", "胸痛", "摔倒", "报警", "危险", "受伤", "自杀", "中毒");
    }

    private boolean containsHumanServiceHallucination(String text) {
        return containsAny(text, "400-", "客服热线", "联系客服", "帮助与反馈", "在线工单", "客服按钮", "转人工");
    }

    private boolean containsUnsupportedLoginHallucination(String text) {
        return containsAny(text, "微信授权登录", "QQ授权登录", "支付宝授权登录", "第三方账号登录");
    }

    private boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String humanServiceUnavailableReply() {
        return "当前版本暂不支持在线转人工服务，也没有内置客服电话、客服热线、帮助与反馈或在线工单入口。请联系线下社区工作人员或项目管理员处理。若遇到紧急医疗、人身安全或支付风险，请立即联系家属、社区工作人员或当地应急渠道。";
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
        String roleCapabilities = "volunteer".equals(role)
                ? "志愿者端已实现：账号登录注册、首页查看公告和活动列表、搜索活动、查看活动详情、报名活动、取消报名、查看已报名/已结束活动、活动时间内签到、查看个人信息、查看时间币余额、发起时间币转账、查看转账历史、AI服务问答、活动服务准备建议。"
                : "老人端已实现：账号登录、服务中心选择服务类型、发布活动、AI生成活动草稿、选择活动地点、提交活动等待审核、查看自己发布的活动、删除活动、查看个人信息、查看时间币余额、发起时间币转账、查看转账历史、AI服务问答。";
        return "你是“时光颐养”时间银行管理系统移动端AI助手，当前用户身份是" + identity + "。"
                + roleCapabilities
                + "回答必须基于当前已实现功能，不确定时要说“当前版本未实现或页面中未看到该能力”，不要猜测。"
                + "不要编造客服电话、400热线、客服入口、帮助与反馈、在线工单、微信/QQ/支付宝登录、语音助手、智能推荐、短信提醒、实名认证、人脸识别、自动派单、提现退款等未实现功能。"
                + "如果用户询问未实现功能，直接说明暂未实现，并给出当前可用替代操作。"
                + "回答要简洁、温和、可执行，优先给出页面路径和操作步骤。"
                + "高风险医疗、人身安全或支付风险问题，只提醒联系家属、社区工作人员或当地应急渠道，不要提平台人工客服。";
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
        if (containsAny(text, "医院", "陪诊", "看病", "取药")) {
            title = "陪诊协助";
            description = "陪同老人前往医院，协助取号、候诊、缴费、取药等流程，注意路途安全。";
        } else if (containsAny(text, "手机", "微信", "挂号", "扫码")) {
            title = "智慧手机使用指导";
            description = "帮助老人学习手机常用功能，包括微信沟通、线上挂号、扫码和常用生活服务。";
        } else if (containsAny(text, "聊天", "陪伴", "读报")) {
            title = "陪伴聊天读报";
            description = "为老人读报、交流近期生活和社区信息，提供耐心陪伴。";
        } else if (containsAny(text, "买药", "买菜", "代买", "购物")) {
            title = "生活物品代买协助";
            description = "协助老人购买药品或生活物品，提前确认清单、金额和交接方式。";
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", title);
        data.put("quota", 1);
        data.put("description", description + " 原始需求：" + text);
        data.put("volunteerReward", 0);
        data.put("suggestions", listOf("请补充准确活动地址", "请确认报名截止时间早于活动开始时间", "如涉及外出，请写清集合地点和注意事项"));
        data.put("warnings", containsAny(text, "急", "病", "摔倒") ? listOf("如涉及紧急医疗情况，请优先联系家属或应急渠道") : listOf("不要在描述中填写银行卡、密码、验证码等敏感信息"));
        return data;
    }

    private Map<String, Object> fallbackVolunteerPreparation(Map<String, Object> activity) {
        String title = valueText(activity, "title", "该活动");
        String address = valueText(activity, "address", "活动地点");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", "本次服务为“" + title + "”，请提前确认时间、地点和老人需求。");
        data.put("checklist", listOf("提前10到15分钟到达" + address, "出发前确认手机电量和导航", "携带身份证件，按页面要求完成签到"));
        data.put("communicationTips", listOf("见面先自我介绍并确认老人称呼", "服务过程中放慢语速，耐心确认需求", "遇到活动变更时，优先与老人或社区工作人员确认"));
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
        if (containsAny(description, "转账", "验证码", "银行卡", "密码")) {
            issues.add("描述中出现支付或敏感信息相关内容，需要管理员重点核查");
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
        String supported = supportedFeatureReply(role, message);
        if (supported != null) {
            return supported;
        }
        if ("volunteer".equals(role)) {
            return "我是志愿者端AI助手，可以帮助你了解活动浏览、报名、取消报名、签到、参与活动记录、时间币余额、转账和服务准备建议。请描述你要完成的操作。";
        }
        return "我是老人端AI助手，可以帮助你了解服务发布、AI活动草稿、活动地点选择、已发布活动查看、时间币余额、转账和平台使用问题。请描述你需要的帮助。";
    }

    private String supportedFeatureReply(String role, String message) {
        if (containsAny(message, "报名", "参加", "接单")) {
            return "志愿者报名活动：进入“首页”，选择想参加的活动，打开详情页后点击“点击报名”。报名后可在“个人中心-参与活动”查看已报名活动。";
        }
        if (containsAny(message, "取消报名", "退出活动")) {
            return "取消报名：进入“个人中心-参与活动”，打开对应活动详情，在报名截止前点击“取消报名”。如果活动已结束或已过报名截止时间，页面可能不再允许取消。";
        }
        if (containsAny(message, "签到", "定位")) {
            return "志愿者签到：进入“个人中心-参与活动”，打开已报名活动详情。到活动时间后，页面会出现签到入口，按页面提示完成定位签到。";
        }
        if (containsAny(message, "发布", "发起", "服务需求")) {
            return "老人发布活动：进入“服务中心”，选择服务类型，填写活动标题、名额、报名截止时间、活动时间、描述和答谢时间币，再选择地点并提交。提交后需要等待管理员审核。";
        }
        if (containsAny(message, "AI怎么帮我填活动", "AI发布", "生成活动", "活动草稿", "自动填表")) {
            return "老人端已支持AI发布活动助手：在发布活动信息页面展开“AI发布助手”，用一句话描述需求，生成草稿后点击“填入表单”，再补充时间和地点。";
        }
        if (containsAny(message, "服务前要准备什么", "准备建议", "服务准备", "风险提醒")) {
            return "志愿者端已支持活动服务准备助手：进入活动详情页，点击生成服务准备建议，可查看准备清单、沟通建议和风险提醒。";
        }
        if (containsAny(message, "时间币", "余额", "积分")) {
            return "查看时间币：进入个人中心中的时间币详情页，可以查看链上余额。页面还支持刷新余额、发起时间币转账和查看转账历史。";
        }
        if (containsAny(message, "转账", "转时间币")) {
            return "时间币转账：进入时间币详情页，点击“转账时间币”，输入收款用户ID、转账数量和登录密码后提交。请确认收款用户ID和金额，当前页面不支持提现或退款。";
        }
        if (containsAny(message, "转账历史", "交易记录", "流水")) {
            return "查看转账历史：进入时间币详情页，点击“查看转账历史”，可以分页查看平台发放和用户转账记录。";
        }
        if (containsAny(message, "活动状态", "我的活动", "审核")) {
            return "老人查看已发布活动：进入“个人中心-我的活动”，可按待审核、审核通过、进行中、拒绝、已过期等状态查看活动。管理员审核通过后，活动才会在志愿者端展示。";
        }
        if (containsAny(message, "公告", "通知")) {
            return "志愿者首页已显示平台公告。当前AI只能说明公告查看位置，不能主动推送消息或发送短信提醒。";
        }
        return null;
    }
}
