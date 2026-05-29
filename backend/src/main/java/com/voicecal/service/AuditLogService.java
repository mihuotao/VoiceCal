package com.voicecal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voicecal.entity.AuditLog;

public interface AuditLogService {

    void log(AuditLog auditLog);

    void log(Long userId, String action, String entityType, Long entityId, String details, String ipAddress, String userAgent);

    Page<AuditLog> listByQuery(Long userId, String action, String entityType, int page, int size);

    void deleteOlderThan(int days);

}
