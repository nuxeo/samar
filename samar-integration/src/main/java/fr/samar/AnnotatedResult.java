package fr.samar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.semanticentities.adapter.OccurrenceRelation;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.VideoDocument;

public class AnnotatedResult {

    protected final DocumentModel doc;

    protected final List<OccurrenceRelation> occurrences = new ArrayList<OccurrenceRelation>();

    protected final UriInfo uriInfo;

    protected final VideoDocument videoDocument;

    protected final TranscodedVideo transcodedVideo;

    public AnnotatedResult(DocumentModel doc, UriInfo info) {
        this.doc = doc;
        this.uriInfo = info;
        if (doc.getType().equals("Video")) {
            videoDocument = doc.getAdapter(VideoDocument.class);
            transcodedVideo = videoDocument.getTranscodedVideo("WebM 480p");
        } else {
            videoDocument = null;
            transcodedVideo = null;
        }
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

    public boolean isVideoPlayerReady() {
        return transcodedVideo != null;
    }

    public String getVideoPosterLink() throws PropertyException,
            ClientException {
        String lastModification = ""
                + (((Calendar) doc.getPropertyValue("dc:modified")).getTimeInMillis());
        String url = uriInfo.getBaseUri().toASCIIString().replace("/site/", "/");
        url += DocumentModelFunctions.fileUrl("downloadPicture", doc,
                "StaticPlayerView:content", lastModification);
        return url;
    }

    public String getVideoWebmLink() {
        if (transcodedVideo == null) {
            return null;
        }
        String blobPropertyName = transcodedVideo.getBlobPropertyName();
        String url = uriInfo.getBaseUri().toASCIIString().replace("/site/", "/");
        url += DocumentModelFunctions.bigFileUrl(doc, blobPropertyName,
                transcodedVideo.getBlob().getFilename()).replace("nullnxbigfile/", "nxbigfile/");
        return url;
    }

}
