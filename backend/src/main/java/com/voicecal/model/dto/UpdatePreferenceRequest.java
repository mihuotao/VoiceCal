package com.voicecal.model.dto;

public class UpdatePreferenceRequest {

    private String defaultView;
    private String defaultCategory;
    private Integer defaultReminder;
    private String language;
    private Integer weekStartDay;
    private String workingHoursStart;
    private String workingHoursEnd;
    private Boolean ttsEnabled;
    private String ttsVoiceType;
    private Integer ttsSpeed;
    private Boolean notificationEnabled;
    private String theme;

    public String getDefaultView() { return defaultView; }
    public void setDefaultView(String defaultView) { this.defaultView = defaultView; }
    public String getDefaultCategory() { return defaultCategory; }
    public void setDefaultCategory(String defaultCategory) { this.defaultCategory = defaultCategory; }
    public Integer getDefaultReminder() { return defaultReminder; }
    public void setDefaultReminder(Integer defaultReminder) { this.defaultReminder = defaultReminder; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getWeekStartDay() { return weekStartDay; }
    public void setWeekStartDay(Integer weekStartDay) { this.weekStartDay = weekStartDay; }
    public String getWorkingHoursStart() { return workingHoursStart; }
    public void setWorkingHoursStart(String workingHoursStart) { this.workingHoursStart = workingHoursStart; }
    public String getWorkingHoursEnd() { return workingHoursEnd; }
    public void setWorkingHoursEnd(String workingHoursEnd) { this.workingHoursEnd = workingHoursEnd; }
    public Boolean getTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(Boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
    public String getTtsVoiceType() { return ttsVoiceType; }
    public void setTtsVoiceType(String ttsVoiceType) { this.ttsVoiceType = ttsVoiceType; }
    public Integer getTtsSpeed() { return ttsSpeed; }
    public void setTtsSpeed(Integer ttsSpeed) { this.ttsSpeed = ttsSpeed; }
    public Boolean getNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(Boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

}
