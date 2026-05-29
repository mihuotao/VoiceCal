package com.voicecal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voicecal.entity.VoiceCommandLog;

public interface VoiceCommandLogService {

    void log(VoiceCommandLog logEntry);

    Page<VoiceCommandLog> listByQuery(Long userId, String intent, String result, int page, int size);

}
