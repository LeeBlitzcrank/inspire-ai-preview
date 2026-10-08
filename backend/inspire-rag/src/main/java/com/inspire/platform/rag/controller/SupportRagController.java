package com.inspire.platform.rag.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.model.SupportModels.*;
import com.inspire.platform.rag.service.SupportAssistantService;
import com.inspire.platform.rag.service.SupportKnowledgeIndexService;
import com.inspire.platform.rag.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rag/support")
@RequiredArgsConstructor
public class SupportRagController {

    private final SupportAssistantService supportAssistantService;
    private final SupportKnowledgeIndexService indexService;
    private final SupportTicketService supportTicketService;
    private final RagProperties ragProperties;

    @Operation(summary = "项目客服问答")
    @PostMapping("/ask")
    public Result<SupportResponse> ask(
            @Valid @RequestBody SupportAskRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(supportAssistantService.ask(
                request.query(), request.topK(), userId));
    }

    @Operation(summary = "项目客服文档检索")
    @PostMapping("/search")
    public Result<SupportResponse> search(
            @Valid @RequestBody SupportAskRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(supportAssistantService.search(
                request.query(), request.topK(), userId));
    }

    @Operation(summary = "项目客服索引状态")
    @GetMapping("/status")
    public Result<SupportIndexState> status() {
        return Result.success(indexService.state());
    }

    @Operation(summary = "提交客服人工工单")
    @PostMapping("/handoff")
    public Result<SupportTicketCreated> handoff(
            @Valid @RequestBody SupportHandoffRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success("人工服务已提交", supportTicketService.create(
                request.issue(), request.contact(), userId, request.attachments()));
    }

    @Operation(summary = "查询客服人工工单")
    @GetMapping("/ticket/{ticketNo}")
    public Result<SupportTicketView> ticket(
            @PathVariable String ticketNo,
            @RequestHeader(value = "X-Support-Ticket-Token", required = false) String accessToken,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(supportTicketService.get(ticketNo, accessToken, userId));
    }

    @Operation(summary = "内部重建项目客服索引")
    @PostMapping("/admin/reindex")
    public Result<SupportIndexState> reindex(
            @RequestHeader(value = "X-Rag-Token", required = false) String token) {
        if (ragProperties.getAdminToken().isBlank()
                || !ragProperties.getAdminToken().equals(token)) {
            return Result.forbidden();
        }
        return Result.success("客服知识库重建完成", indexService.reindex());
    }

    public record SupportAskRequest(
            @Size(max = 500, message = "问题不能超过500个字符")
            String query,
            Integer topK
    ) {
    }

    public record SupportHandoffRequest(
            @NotBlank(message = "请填写需要人工处理的问题")
            @Size(max = 1000, message = "问题不能超过1000个字符")
            String issue,
            @Size(max = 200, message = "联系方式不能超过200个字符")
            String contact,
            @Size(max = 5, message = "最多上传5个附件")
            List<SupportTicketAttachment> attachments
    ) {
    }
}
