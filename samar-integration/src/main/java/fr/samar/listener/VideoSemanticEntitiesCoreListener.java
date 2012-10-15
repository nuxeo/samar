package fr.samar.listener;

import java.util.ArrayList;

import org.nuxeo.ecm.platform.semanticentities.listener.SemanticEntitiesCoreListener;
import org.nuxeo.vocapia.service.TranscriptionWork;

public class VideoSemanticEntitiesCoreListener extends
        SemanticEntitiesCoreListener {

    public VideoSemanticEntitiesCoreListener() {
        super();
        eventNames = new ArrayList<String>();
        eventNames.add(TranscriptionWork.EVENT_TRANSCRIPTION_COMPLETE);
        documentTypes.clear();
        documentTypes.add("Video");
    }
}
