package fr.samar.translation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object that is used to prefetch the text of all the fields to
 * translate and store the resulting translations.
 * 
 * This DTO is useful to pass hold data in memory in a transaction-less
 * environment while the actual translation process is happening in the
 * background.
 */
public class TranslationTask {

    public static final String LANGUAGE = "language";

    protected String sourceLanguage;

    protected final List<Map<String, Object>> fieldsToTranslate = new ArrayList<Map<String, Object>>();

    protected final List<Map<String, Object>> translatedFields = new ArrayList<Map<String, Object>>();

    public void setSourceLanguage(String language) {
        this.sourceLanguage = language;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void addFieldToTranslate(Map<String, Object> fieldSpec) {
        fieldsToTranslate.add(fieldSpec);
    }

    public List<Map<String, Object>> getFieldsToTranslate() {
        return fieldsToTranslate;
    }

    public List<Map<String, Object>> getTranslatedFields() {
        return translatedFields;
    }

    public void addTranslationResult(Map<String, Object> translationResult) {
        translatedFields.add(translationResult);
    }

}
