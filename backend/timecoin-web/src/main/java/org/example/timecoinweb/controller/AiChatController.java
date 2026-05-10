package org.example.timecoinweb.controller;

import org.example.pojo.Result;
import org.example.timecoinweb.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class AiChatController {
    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/old/ai/chat")
    public Result oldChat(@RequestBody AiChatRequest request) {
        return Result.success(buildResponse("old", request));
    }

    @PostMapping("/vol/ai/chat")
    public Result volunteerChat(@RequestBody AiChatRequest request) {
        return Result.success(buildResponse("volunteer", request));
    }

    @PostMapping("/old/ai/activity-draft")
    public Result oldActivityDraft(@RequestBody AiActivityRequest request) {
        return Result.success(aiChatService.generateActivityDraft(request == null ? null : request.getText()));
    }

    @PostMapping("/vol/ai/activity-prepare")
    public Result volunteerActivityPrepare(@RequestBody AiActivityRequest request) {
        return Result.success(aiChatService.generateVolunteerPreparation(request == null ? null : request.getActivity()));
    }

    @PostMapping("/administrator/ai/activity-review")
    public Result administratorActivityReview(@RequestBody AiActivityRequest request) {
        return Result.success(aiChatService.generateActivityReview(request == null ? null : request.getActivity()));
    }

    private Map<String, String> buildResponse(String role, AiChatRequest request) {
        String reply = aiChatService.chat(role, request == null ? null : request.getMessage());
        Map<String, String> data = new HashMap<>();
        data.put("reply", reply);
        return data;
    }

    public static class AiChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class AiActivityRequest {
        private String text;
        private Map<String, Object> activity;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Map<String, Object> getActivity() {
            return activity;
        }

        public void setActivity(Map<String, Object> activity) {
            this.activity = activity;
        }
    }
}
