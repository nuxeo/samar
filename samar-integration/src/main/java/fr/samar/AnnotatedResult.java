package fr.samar;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.semanticentities.adapter.OccurrenceRelation;

public class AnnotatedResult {

    protected final DocumentModel doc;

    protected final List<OccurrenceRelation> occurrences = new ArrayList<OccurrenceRelation>();

    public AnnotatedResult(DocumentModel doc) {
        this.doc = doc;
    }

    public void addOccurrence(OccurrenceRelation occurrence) {
        occurrences.add(occurrence);
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public List<OccurrenceRelation> getOccurrences() {
        return occurrences;
    }

    public boolean hasSpeechTranscription() {
        return doc.hasFacet("HasSpeechTranscription");
    }

}
