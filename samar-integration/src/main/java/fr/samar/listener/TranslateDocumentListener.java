package fr.samar.listener;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.semanticentities.LocalEntityService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.vocapia.service.TranscriptionWork;

import fr.samar.translation.TranslationService;

/**
 * Post commit event-listener to launch translation on newly created documents
 * or documents that have been transcribed from a video.
 */
public class TranslateDocumentListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(TranslateDocumentListener.class);

    protected List<String> eventNames = Arrays.asList(
            DocumentEventTypes.DOCUMENT_CREATED,
            TranscriptionWork.EVENT_TRANSCRIPTION_COMPLETE);

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        Set<String> typesToIgnore = new HashSet<String>(Arrays.asList(
                "Occurrence", "Entity"));
        try {
            LocalEntityService leService = Framework.getService(LocalEntityService.class);
            typesToIgnore.addAll(leService.getEntityTypeNames());
        } catch (Exception e) {
            log.error(e, e);
            return;
        }

        CoreSession session = null;
        Set<Serializable> ids = new HashSet<Serializable>();
        for (Event event : events) {
            if (!eventNames.isEmpty() && !eventNames.contains(event.getName())) {
                continue;
            }
            EventContext eventContext = event.getContext();
            CoreSession s = eventContext.getCoreSession();
            DocumentModel dm = (DocumentModel) eventContext.getArguments()[0];
            if (dm.isVersion() || dm.isProxy() || dm.isFolder()) {
                // do not perform translation on readonly or folderish documents
                continue;
            }
            if (typesToIgnore.contains(dm.getType())) {
                continue;
            }
            ids.add(dm.getId());
            if (session == null) {
                session = s;
            } else if (session != s) {
                // cannot happen given current ReconnectedEventBundleImpl
                throw new ClientException(
                        "Several CoreSessions in one EventBundle");
            }
        }
        if (session == null) {
            if (ids.isEmpty()) {
                return;
            }
            throw new ClientException("Missing CoreSession");
        }
        TranslationService translationService = Framework.getLocalService(TranslationService.class);
        for (Serializable id : ids) {
            IdRef docRef = new IdRef((String) id);
            // perform the entity extraction and linking operation
            try {
                DocumentLocationImpl docLoc = new DocumentLocationImpl(
                        session.getRepositoryName(), docRef);
                translationService.launchTranslation(docLoc);
            } catch (Exception e) {
                log.error(e, e);
            }
        }
    }

}
