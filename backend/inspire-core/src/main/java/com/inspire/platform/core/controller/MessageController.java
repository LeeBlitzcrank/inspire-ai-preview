package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.dto.MessageSendRequest;
import com.inspire.platform.core.entity.Message;
import com.inspire.platform.core.entity.MessageConversation;
import com.inspire.platform.core.service.MessageService;
import com.inspire.platform.core.service.MessageStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Tag(name = "私信")
public class MessageController {

    private final MessageService messageService;
    private final MessageStreamService messageStreamService;
    private final JdbcTemplate jdbcTemplate;

    private Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        return userId != null ? Long.parseLong(userId) : null;
    }

    @PostMapping("/send")
    @Operation(summary = "发送私信")
    public Result<Message> send(@Valid @RequestBody MessageSendRequest payload,
                                 HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        Message message = messageService.sendMessage(
                userId, payload.getToUserId(), payload.getContent(),
                payload.getType(), payload.getExtraJson());
        messageStreamService.emitToUsers(List.of(userId, payload.getToUserId()), "message", message);
        return Result.success(message);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "私信实时事件流")
    public SseEmitter stream(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return messageStreamService.connect(userId);
    }

    @GetMapping("/conversations")
    @Operation(summary = "会话列表")
    public Result<List<MessageConversation>> conversations(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(messageService.getConversations(userId));
    }

    @GetMapping("/list")
    @Operation(summary = "消息列表")
    public Result<List<Message>> list(@RequestParam Long conversationId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(messageService.getMessages(userId, conversationId, page, size));
    }

    @PostMapping("/read")
    @Operation(summary = "标记已读")
    public Result<Void> markRead(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        Long conversationId = body.get("conversationId") != null ? Long.valueOf(body.get("conversationId").toString()) : null;
        if (conversationId == null) {
            return Result.error(400, "参数缺失");
        }
        messageService.markAsRead(userId, conversationId);
        messageStreamService.emitToUsers(
                jdbcTemplate.queryForList(
                        "SELECT user_id FROM conversation_member WHERE conversation_id = ?",
                        Long.class, conversationId),
                "read",
                Map.of("conversationId", String.valueOf(conversationId), "userId", String.valueOf(userId)));
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

    @PostMapping("/{conversationId}/{id}/recall")
    @Operation(summary = "撤回私信")
    public Result<Void> recall(@PathVariable Long conversationId,
                               @PathVariable Long id,
                               HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        messageService.recallMessage(userId, conversationId, id);
        messageStreamService.emitToUsers(
                jdbcTemplate.queryForList(
                        "SELECT user_id FROM conversation_member WHERE conversation_id = ?",
                        Long.class, conversationId),
                "recall",
                Map.of("conversationId", String.valueOf(conversationId), "messageId", String.valueOf(id)));
        return Result.success("已撤回", null);
    }

    @DeleteMapping("/conversation/{id}")
    @Operation(summary = "删除会话")
    public Result<Void> deleteConversation(@PathVariable("id") Long conversationId,
                                            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        messageService.deleteConversation(userId, conversationId);
        messageStreamService.emit(userId, "conversation", Map.of("conversationId", String.valueOf(conversationId)));
        return Result.success();
    }


    @PostMapping("/start")
    @Operation(summary = "创建或获取会话（按userId）")
    public Result<MessageConversation> startConversation(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        Long toUserId = body.get("toUserId") != null ? Long.valueOf(body.get("toUserId").toString()) : null;
        if (toUserId == null || userId.equals(toUserId)) {
            return Result.error(400, "无效的用户");
        }
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
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        messageService.deleteAllConversations(userId);
        messageStreamService.emit(userId, "conversation", Map.of("all", true));
        return Result.success();
    }

}
