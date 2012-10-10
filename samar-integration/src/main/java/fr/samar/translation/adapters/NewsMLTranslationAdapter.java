package fr.samar.translation.adapters;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

import fr.samar.translation.BaseTranslationAdapter;

public class NewsMLTranslationAdapter extends BaseTranslationAdapter implements
        DocumentAdapterFactory {

    public NewsMLTranslationAdapter() {
        // only for the factory
        super(null);
    }

    public NewsMLTranslationAdapter(DocumentModel doc) {
        super(doc);
        addFieldToTranslate("note:note", true);
    }

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        return new NewsMLTranslationAdapter(doc);
    }

}
