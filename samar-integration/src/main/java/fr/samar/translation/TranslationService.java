package fr.samar.translation;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;


public class TranslationService extends DefaultComponent {

    // TODO: unhardcode me
    public List<String> targetLanguages = Arrays.asList("fr", "en");

    public void launchTranslation(DocumentLocation docLoc) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        for (String lang : targetLanguages) {
            workManager.schedule(
                    new TranslationWork(docLoc).withTargetLanguage(lang),
                    Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        }
    }

    public boolean isSupportedLangage(String language) {
        // TODO: un-hardcode me by using an extension point or commandline
        // availability introspection
        return "ar".equals(language);
    }
}
