package fr.samar;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.semanticentities.adapter.OccurrenceRelation;

public class AnnotatedResult {

    protected final List<DocumentModel> relatedEntities;

    protected final DocumentModel doc;

    protected final List<OccurrenceRelation> occurrences = new ArrayList<OccurrenceRelation>();

    public AnnotatedResult(DocumentModel doc,
            List<DocumentModel> relatedEntities) {
        this.doc = doc;
        this.relatedEntities = relatedEntities;
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

    public List<DocumentModel> getRelatedEntities() {
        return relatedEntities;
    }

}
