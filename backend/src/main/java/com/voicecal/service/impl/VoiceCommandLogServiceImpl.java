package com.voicecal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voicecal.entity.VoiceCommandLog;
import com.voicecal.mapper.VoiceCommandLogMapper;
import com.voicecal.service.VoiceCommandLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VoiceCommandLogServiceImpl implements VoiceCommandLogService {

    private final VoiceCommandLogMapper voiceCommandLogMapper;

    public VoiceCommandLogServiceImpl(VoiceCommandLogMapper voiceCommandLogMapper) {
        this.voiceCommandLogMapper = voiceCommandLogMapper;
    }

    @Override
    public void log(VoiceCommandLog logEntry) {
        voiceCommandLogMapper.insert(logEntry);
    }

    @Override
    public Page<VoiceCommandLog> listByQuery(Long userId, String intent, String result, int page, int size) {
        Page<VoiceCommandLog> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<VoiceCommandLog> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(VoiceCommandLog::getUserId, userId);
        }
        if (StringUtils.hasText(intent)) {
            wrapper.eq(VoiceCommandLog::getIntent, intent);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(VoiceCommandLog::getCommandResult, result);
        }

        wrapper.orderByDesc(VoiceCommandLog::getCreatedAt);
        return voiceCommandLogMapper.selectPage(mpPage, wrapper);
    }

}
