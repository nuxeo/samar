package fr.samar.translation.adapters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

import fr.samar.translation.TranslationAdapter;
import fr.samar.translation.TranslationTask;

public class BaseTranslationAdapter implements TranslationAdapter {

    private static final Log log = LogFactory.getLog(BaseTranslationAdapter.class);

    protected final DocumentModel doc;

    protected final List<Map<String, Object>> fieldsToTranslate = new ArrayList<Map<String, Object>>();

    public BaseTranslationAdapter(DocumentModel doc) {
        this.doc = doc;
        addFieldToTranslate("dc:title", false);
        addFieldToTranslate("dc:description", false);
    }

    public void addFieldToTranslate(String propertyPath, boolean isFormatted) {
        Map<String, Object> field = new HashMap<String, Object>();
        field.put(PROPERTY_PATH, propertyPath);
        field.put(IS_FORMATTED, isFormatted);
        fieldsToTranslate.add(field);
    }

    public List<Map<String, Object>> getFieldsToTranslate() {
        return fieldsToTranslate;
    }

    public TranslationTask getTranslationTask() throws PropertyException,
            ClientException {
        String sourceLanguage = (String) doc.getPropertyValue(DC_LANGUAGE);
        if (sourceLanguage == null) {
            log.warn(String.format(
                    "Cannot translate document '%s' / %s because of unknown source language",
                    doc.getTitle(), doc.getRef()));
            return null;
        }
        TranslationTask task = new TranslationTask();
        task.setSourceLanguage(sourceLanguage);
        // Read all the text to translate in advance to prefetch
        // it in memory and make it possible to perform the
        // actual translation process out of any transactional
        // context.
        for (Map<String, Object> fieldToTranslate : getFieldsToTranslate()) {
            Map<String, Object> fieldSpec = new HashMap<String, Object>();
            fieldSpec.putAll(fieldToTranslate);
            try {
                String sourceText = (String) doc.getPropertyValue((String) fieldToTranslate.get(PROPERTY_PATH));
                fieldSpec.put(TEXT, sourceText);
                if (log.isDebugEnabled()) {
                    if (sourceText != null && !sourceText.isEmpty()) {
                        String snippet = sourceText.substring(0,
                                Math.min(40, sourceText.length()));
                        log.debug("Adding field '"
                                + fieldSpec.get(PROPERTY_PATH)
                                + "' with text: " + snippet + "...");
                        task.addFieldToTranslate(fieldSpec);
                    } else {
                        log.debug("Skipping empty field '"
                                + fieldSpec.get(PROPERTY_PATH) + "'");
                    }
                }
            } catch (PropertyException e) {
                // missing property on this document type: ignore
                continue;
            }
        }
        return task;
    }

    public void setTranslationResults(TranslationTask task)
            throws PropertyException, ClientException {
        if (!doc.hasFacet(HAS_TRANSLATION)) {
            doc.addFacet(HAS_TRANSLATION);
        }
        doc.setPropertyValue(TRANSLATED_FIELDS,
                (Serializable) task.getTranslatedFields());
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getTranslatedField(String language, String propertyPath)
            throws PropertyException, ClientException {
        if (!doc.hasFacet(HAS_TRANSLATION)) {
            return null;
        }
        List<Map<String, Object>> fields = (List<Map<String, Object>>) doc.getPropertyValue(TRANSLATED_FIELDS);
        for (Map<String, Object> field : fields) {
            if (language.equals(field.get(TranslationTask.LANGUAGE))
                    && propertyPath.equals(field.get(PROPERTY_PATH))) {
                return (String) field.get(TEXT);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getTranslatedFields(
            String propertyPath) throws PropertyException, ClientException {
        Map<String, Map<String, Object>> translations = new TreeMap<String, Map<String, Object>>();
        if (!doc.hasFacet(HAS_TRANSLATION)) {
            return translations;
        }
        List<Map<String, Object>> fields = (List<Map<String, Object>>) doc.getPropertyValue(TRANSLATED_FIELDS);
        for (Map<String, Object> field : fields) {
            if (propertyPath.equals(field.get(PROPERTY_PATH))) {
                translations.put((String) field.get(TranslationTask.LANGUAGE),
                        field);
            }
        }
        return translations;
    }
}
