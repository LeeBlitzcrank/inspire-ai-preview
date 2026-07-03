package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.entity.Message;
import com.inspire.platform.core.entity.MessageConversation;
import com.inspire.platform.core.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Tag(name = "私信")
public class MessageController {

    private final MessageService messageService;
    private final JdbcTemplate jdbcTemplate;

    private Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-Inspire-UserId");
        return userId != null ? Long.parseLong(userId) : null;
    }

    @PostMapping("/send")
    @Operation(summary = "发送私信")
    public Result<Message> send(@RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        Long toUserId = body.get("toUserId") != null ? Long.valueOf(body.get("toUserId").toString()) : null;
        String content = (String) body.get("content");
        if (toUserId == null || content == null) return Result.error(400, "参数缺失");
        return Result.success(messageService.sendMessage(userId, toUserId, content));
    }

    @GetMapping("/conversations")
    @Operation(summary = "会话列表")
    public Result<List<MessageConversation>> conversations(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(messageService.getConversations(userId));
    }

    @GetMapping("/list")
    @Operation(summary = "消息列表")
    public Result<List<Message>> list(@RequestParam Long conversationId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(messageService.getMessages(userId, conversationId, page, size));
    }

    @PostMapping("/read")
    @Operation(summary = "标记已读")
    public Result<Void> markRead(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        Long conversationId = body.get("conversationId") != null ? Long.valueOf(body.get("conversationId").toString()) : null;
        if (conversationId == null) return Result.error(400, "参数缺失");
        messageService.markAsRead(userId, conversationId);
        return Result.success();
    }

    @GetMapping("/unread")
    @Operation(summary = "未读数")
    public Result<Map<String, Object>> unread(HttpServletRequest request) {
        Long userId = getUserId(request);
        Map<String, Object> data = new HashMap<>();
        if (userId == null) { data.put("count", 0); return Result.success(data); }
        data.put("count", messageService.unreadCount(userId));
        return Result.success(data);
    }

    @PostMapping("/send-by-username")
    @Operation(summary = "根据用户名发送私信")
    public Result<Message> sendByUsername(@RequestBody Map<String, String> body,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        String username = body.get("username");
        String content = body.get("content");
        if (username == null || content == null) return Result.error(400, "参数缺失");
        try {
            Long toUserId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username=?", Long.class, username);
            return Result.success(messageService.sendMessage(userId, toUserId, content));
        } catch (Exception e) {
            return Result.error(404, "用户不存在");
        }
    }

    @DeleteMapping("/conversation/{id}")
    @Operation(summary = "删除会话")
    public Result<Void> deleteConversation(@PathVariable("id") Long conversationId,
                                            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        messageService.deleteConversation(userId, conversationId);
        return Result.success();
    }


    @PostMapping("/start")
    @Operation(summary = "创建或获取会话（按userId）")
    public Result<MessageConversation> startConversation(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        Long toUserId = body.get("toUserId") != null ? Long.valueOf(body.get("toUserId").toString()) : null;
        if (toUserId == null || userId.equals(toUserId)) return Result.error(400, "无效的用户");
        MessageConversation conv = messageService.startConversation(userId, toUserId);
        try {
            String nickname = jdbcTemplate.queryForObject("SELECT nickname FROM user WHERE id=?", String.class, toUserId);
            conv.setTargetNickname(nickname);
        } catch (Exception ignored) {}
        return Result.success(conv);
    }


    @DeleteMapping("/conversations")
    @Operation(summary = "清空所有会话")
    public Result<Void> deleteAllConversations(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        messageService.deleteAllConversations(userId);
        return Result.success();
    }

}