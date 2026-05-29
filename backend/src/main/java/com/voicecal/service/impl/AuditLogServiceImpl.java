package com.voicecal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voicecal.entity.AuditLog;
import com.voicecal.mapper.AuditLogMapper;
import com.voicecal.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void log(AuditLog auditLog) {
        auditLogMapper.insert(auditLog);
    }

    @Override
    public void log(Long userId, String action, String entityType, Long entityId, String details, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        auditLogMapper.insert(log);
    }

    @Override
    public Page<AuditLog> listByQuery(Long userId, String action, String entityType, int page, int size) {
        Page<AuditLog> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(AuditLog::getUserId, userId);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (StringUtils.hasText(entityType)) {
            wrapper.eq(AuditLog::getEntityType, entityType);
        }

        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(mpPage, wrapper);
    }

    @Override
    @Transactional
    public void deleteOlderThan(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        auditLogMapper.delete(
                new LambdaQueryWrapper<AuditLog>()
                        .lt(AuditLog::getCreatedAt, cutoff));
    }

}
