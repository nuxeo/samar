package fr.samar.translation.adapters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.schema.FacetNames;

import fr.samar.translation.TranslationTask;

public class SamarTranslationAdapter extends BaseTranslationAdapter implements
        DocumentAdapterFactory {

    public static final String RELATEDTEXT_TRANSCRIPTION = "relatedtext:relatedtextresources_transcription";

    private static final Log log = LogFactory.getLog(SamarTranslationAdapter.class);

    public SamarTranslationAdapter() {
        // only for the factory
        super(null);
    }

    public SamarTranslationAdapter(DocumentModel doc) {
        super(doc);
        addFieldToTranslate("note:note", true);
    }

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        return new SamarTranslationAdapter(doc);
    }

    @Override
    public TranslationTask getTranslationTask() throws PropertyException,
            ClientException {
        TranslationTask task = super.getTranslationTask();
        if (doc.hasFacet("HasSpeechTranscription")
                && doc.hasFacet(FacetNames.HAS_RELATED_TEXT)) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> resources = doc.getProperty(
                    "relatedtext:relatedtextresources").getValue(List.class);
            for (Map<String, String> relatedResource : resources) {
                if (relatedResource.get("relatedtextid").equals("transcription")) {
                    String text = relatedResource.get("relatedtext");
                    Map<String, Object> field = new HashMap<String, Object>();
                    field.put(PROPERTY_PATH, RELATEDTEXT_TRANSCRIPTION);
                    field.put(IS_FORMATTED, false);
                    field.put(TEXT, text);
                    if (log.isDebugEnabled()) {
                        if (text != null && !text.isEmpty()) {
                            String snippet = text.substring(0,
                                    Math.min(40, text.length()));
                            log.debug("Adding field '"
                                    + RELATEDTEXT_TRANSCRIPTION
                                    + "' with text: " + snippet + "...");
                            task.addFieldToTranslate(field);
                        } else {
                            log.debug("Skipping empty field '"
                                    + RELATEDTEXT_TRANSCRIPTION + "'");
                        }
                    }
                    break;
                }
            }
        }
        return task;
    }
}
