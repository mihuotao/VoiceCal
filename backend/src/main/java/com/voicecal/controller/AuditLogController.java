package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.entity.AuditLog;
import com.voicecal.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@CurrentUser LoginUser loginUser,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String action,
                                                      @RequestParam(required = false) String entityType) {
        var pageResult = auditLogService.listByQuery(loginUser.getUserId(), action, entityType, page, size);

        List<Map<String, Object>> list = pageResult.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ApiResult.page(list, pageResult.getTotal(), page, size);
    }

    private Map<String, Object> toMap(AuditLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUserId());
        map.put("action", log.getAction());
        map.put("entityType", log.getEntityType());
        map.put("entityId", log.getEntityId());
        map.put("details", log.getDetails());
        map.put("ipAddress", log.getIpAddress());
        map.put("createdAt", log.getCreatedAt() != null
                ? log.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        return map;
    }

}
