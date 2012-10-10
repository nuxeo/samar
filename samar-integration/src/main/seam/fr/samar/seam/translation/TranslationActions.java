package fr.samar.seam.translation;

import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.runtime.api.Framework;

import fr.samar.translation.TranslationService;

@Name("translationActions")
@Scope(ScopeType.EVENT)
public class TranslationActions {

    @In(create = true)
    protected FacesMessages facesMessages;

    @In(create = true, required = false)
    protected ClipboardActions clipboardActions;

    @In(create = true)
    protected Map<String, String> messages;
    
    public void translateCurrentList() {
        if (clipboardActions == null) {
            // no hard runtime dependencies on the clipboardActions seam
            // component
            return;
        }
        List<DocumentModel> docList = clipboardActions.getCurrentSelectedList();
        if (docList != null) {
            for (DocumentModel doc : docList) {
                launchAsyncTranslation(doc);
            }
            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("status.worklist.semanticAnalysis"),
                    docList.size());
        } 
    }

    public void launchAsyncTranslation(DocumentModel doc) {
        TranslationService translationService = Framework.getLocalService(TranslationService.class);
        translationService.launchTranscription(new DocumentLocationImpl(doc));
    }
    
}
