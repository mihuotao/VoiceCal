package com.voicecal.voice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class NluResult {

    private String rawText;
    private String intent;
    private Map<String, Object> entities = new HashMap<>();
    private double confidence;
    private String nluSource;
    private boolean requiresClarify;
    private String clarifyQuestion;
    private String missingField;

    public NluResult() {}

    public NluResult(String rawText, String intent, double confidence, String nluSource) {
        this.rawText = rawText;
        this.intent = intent;
        this.confidence = confidence;
        this.nluSource = nluSource;
    }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public Map<String, Object> getEntities() { return entities; }
    public void setEntities(Map<String, Object> entities) { this.entities = entities; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getNluSource() { return nluSource; }
    public void setNluSource(String nluSource) { this.nluSource = nluSource; }
    public boolean isRequiresClarify() { return requiresClarify; }
    public void setRequiresClarify(boolean requiresClarify) { this.requiresClarify = requiresClarify; }
    public String getClarifyQuestion() { return clarifyQuestion; }
    public void setClarifyQuestion(String clarifyQuestion) { this.clarifyQuestion = clarifyQuestion; }
    public String getMissingField() { return missingField; }
    public void setMissingField(String missingField) { this.missingField = missingField; }

    public NluResult addEntity(String key, Object value) {
        this.entities.put(key, value);
        return this;
    }

}
