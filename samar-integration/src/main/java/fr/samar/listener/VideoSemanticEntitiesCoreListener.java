package fr.samar.listener;

import java.util.ArrayList;

import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.semanticentities.listener.SemanticEntitiesCoreListener;

public class VideoSemanticEntitiesCoreListener extends
        SemanticEntitiesCoreListener {

    public VideoSemanticEntitiesCoreListener() {
        super();
        eventNames = new ArrayList<String>(eventNames);
        eventNames.add(DocumentEventTypes.DOCUMENT_UPDATED);
        documentTypes.clear();
        documentTypes.add("Video");
    }
}
