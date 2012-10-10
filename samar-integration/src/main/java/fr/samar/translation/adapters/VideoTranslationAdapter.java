package fr.samar.translation.adapters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.schema.FacetNames;

import fr.samar.translation.BaseTranslationAdapter;
import fr.samar.translation.TranslationTask;

public class VideoTranslationAdapter extends BaseTranslationAdapter implements
        DocumentAdapterFactory {

    public VideoTranslationAdapter() {
        // only for the factory
        super(null);
    }

    public VideoTranslationAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        return new VideoTranslationAdapter(doc);
    }

    @Override
    public TranslationTask getTranslationTask() throws PropertyException,
            ClientException {
        TranslationTask task = super.getTranslationTask();
        if (!doc.hasFacet("HasSpeechTranscription")
                || !doc.hasFacet(FacetNames.HAS_RELATED_TEXT)) {
            // do not translate untranscribed videos
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> resources = doc.getProperty(
                "relatedtext:relatedtextresources").getValue(List.class);
        for (Map<String, String> relatedResource : resources) {
            if (relatedResource.get("relatedtextid").equals("transcription")) {
                Map<String, Object> field = new HashMap<String, Object>();
                field.put(TranslationTask.PROPERTY_PATH,
                        "relatedtext:relatedtextresources_transcription");
                field.put(TranslationTask.IS_FORMATTED, false);
                field.put(TranslationTask.TEXT, false);
                task.addFieldToTranslate(field);
                break;
            }
        }
        return task;
    }
}
