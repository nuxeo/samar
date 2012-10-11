package fr.samar.translation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

public class BaseTranslationAdapter implements TranslationAdapter {

    protected final DocumentModel doc;

    protected final List<Map<String, Object>> fieldsToTranslate = new ArrayList<Map<String, Object>>();

    public BaseTranslationAdapter(DocumentModel doc) {
        this.doc = doc;
        addFieldToTranslate("dc:title", false);
        addFieldToTranslate("dc:description", false);
    }

    public void addFieldToTranslate(String propertyPath, boolean isFormatted) {
        Map<String, Object> field = new HashMap<String, Object>();
        field.put(TranslationTask.PROPERTY_PATH, propertyPath);
        field.put(TranslationTask.IS_FORMATTED, isFormatted);
        fieldsToTranslate.add(field);
    }

    public List<Map<String, Object>> getFieldsToTranslate() {
        return fieldsToTranslate;
    }

    public TranslationTask getTranslationTask() throws PropertyException,
            ClientException {
        String sourceLanguage = (String) doc.getPropertyValue(DC_LANGUAGE);
        if (sourceLanguage == null) {
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
                String sourceText = (String) doc.getPropertyValue((String) fieldToTranslate.get(TranslationTask.PROPERTY_PATH));
                fieldSpec.put(TranslationTask.TEXT, sourceText);
                task.addFieldToTranslate(fieldSpec);
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

}