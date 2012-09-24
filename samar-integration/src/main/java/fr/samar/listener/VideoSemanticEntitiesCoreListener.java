package fr.samar.listener;

import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.semanticentities.listener.SemanticEntitiesCoreListener;

public class VideoSemanticEntitiesCoreListener extends SemanticEntitiesCoreListener {

    public VideoSemanticEntitiesCoreListener() {
        super();
        eventNames.clear();
        eventNames.add(DocumentEventTypes.DOCUMENT_CREATED);
        eventNames.add(DocumentEventTypes.DOCUMENT_UPDATED);
        documentTypes.clear();
        documentTypes.add("Video");
    }
}
