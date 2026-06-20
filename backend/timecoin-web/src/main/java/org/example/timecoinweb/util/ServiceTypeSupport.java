package org.example.timecoinweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.pojo.Activity;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 老人端动态服务类型规范化与校验工具。
 */
public final class ServiceTypeSupport {
    public static final String OTHER_SERVICE = "other_service";

    private static final Set<String> ALLOWED_SERVICE_TYPES = Set.of(
            "medical_rehab",
            "health_manage",
            "cleaning",
            "shopping_companion",
            "clinic_companion",
            "purchase",
            OTHER_SERVICE
    );

    private static final Map<String, List<String>> REQUIRED_EXTRA_FIELDS = Map.of(
            "medical_rehab", List.of("hospitalAddress", "department", "appointmentTime"),
            "health_manage", List.of("healthTaskType", "frequency"),
            "cleaning", List.of("cleaningScope", "homeArea"),
            "shopping_companion", List.of("destination", "budgetRange"),
            "clinic_companion", List.of("hospitalAddress", "visitType", "registrationNeeded"),
            "purchase", List.of("shoppingList", "maxBudget"),
            OTHER_SERVICE, List.of("customCategory", "serviceDetails")
    );

    private ServiceTypeSupport() {
    }

    public static void normalizeForCreate(Activity activity, ObjectMapper objectMapper) {
        String serviceType = normalizeServiceType(activity.getServiceType());
        String extraJson = normalizeAndValidateExtraJson(serviceType, activity.getExtraJson(), objectMapper);
        activity.setServiceType(serviceType);
        activity.setExtraJson(extraJson);
    }

    public static void normalizeForUpdate(Activity activity, ObjectMapper objectMapper) {
        String rawType = activity.getServiceType();
        String rawExtra = activity.getExtraJson();
        boolean hasType = StringUtils.hasText(rawType);
        boolean hasExtra = StringUtils.hasText(rawExtra);

        if (!hasType && !hasExtra) {
            return;
        }
        if (!hasType) {
            throw new IllegalArgumentException("更新服务扩展字段时必须同时传入 serviceType");
        }

        String serviceType = normalizeServiceType(rawType);
        String extraJson = normalizeAndValidateExtraJson(serviceType, rawExtra, objectMapper);
        activity.setServiceType(serviceType);
        activity.setExtraJson(extraJson);
    }

    public static String normalizeServiceType(String rawType) {
        String normalized = StringUtils.hasText(rawType) ? rawType.trim().toLowerCase() : OTHER_SERVICE;
        if (!ALLOWED_SERVICE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的服务类型: " + rawType);
        }
        return normalized;
    }

    public static String normalizeAndValidateExtraJson(String serviceType, String rawExtraJson, ObjectMapper objectMapper) {
        String payload = StringUtils.hasText(rawExtraJson) ? rawExtraJson.trim() : "{}";
        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("extraJson 必须是合法 JSON 对象");
        }

        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("extraJson 必须是 JSON 对象");
        }

        List<String> requiredKeys = REQUIRED_EXTRA_FIELDS.getOrDefault(serviceType, List.of());
        for (String key : requiredKeys) {
            JsonNode field = node.get(key);
            if (field == null || field.isNull()) {
                throw new IllegalArgumentException("缺少服务扩展字段: " + key);
            }
            if (field.isTextual() && !StringUtils.hasText(field.asText())) {
                throw new IllegalArgumentException("服务扩展字段不能为空: " + key);
            }
        }

        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalArgumentException("extraJson 序列化失败");
        }
    }
}
