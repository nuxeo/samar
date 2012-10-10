package fr.samar.translation;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class TranslationService extends DefaultComponent {

    public void launchTranscription(DocumentLocation docLoc) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(makeWork(docLoc),
                Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }

    protected Work makeWork(DocumentLocation docLoc) {
        TranslationWork work = new TranslationWork(docLoc);
        // TODO: un-hardcode me
        return work.withTargetLanguage("fr").withTargetLanguage("en");
    }

    public boolean isSupportedLangage(String language) {
        // TODO: un-hardcode me by using an extension point or commandline
        // availability introspection
        return "ar".equals(language);
    }
}
