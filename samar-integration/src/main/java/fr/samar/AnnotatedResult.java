package fr.samar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.semanticentities.adapter.OccurrenceRelation;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.VideoDocument;

import fr.samar.translation.TranslationAdapter;
import fr.samar.translation.adapters.SamarTranslationAdapter;

public class AnnotatedResult {

    protected final DocumentModel doc;

    protected final List<OccurrenceRelation> occurrences = new ArrayList<OccurrenceRelation>();

    protected final UriInfo uriInfo;

    protected final VideoDocument videoDocument;

    protected final TranscodedVideo webmTranscodedVideo;

    protected final TranscodedVideo mp4TranscodedVideo;

    protected final SamarTranslationAdapter translation;

    public AnnotatedResult(DocumentModel doc, UriInfo info) {
        this.doc = doc;
        this.uriInfo = info;
        this.translation = new SamarTranslationAdapter(doc);
        if (doc.getType().equals("Video")) {
            videoDocument = doc.getAdapter(VideoDocument.class);
            webmTranscodedVideo = videoDocument.getTranscodedVideo("WebM 480p");
            mp4TranscodedVideo = videoDocument.getTranscodedVideo("MP4 480p");
        } else {
            videoDocument = null;
            webmTranscodedVideo = null;
            mp4TranscodedVideo = null;
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

    public boolean isVideoPlayerReady() throws PropertyException,
            ClientException {
        String FILE_CONTENT = "file:content";
        Blob mainBlob = (Blob) doc.getPropertyValue(FILE_CONTENT);
        if (mainBlob != null
                && ("video/mp4".equals(mainBlob.getMimeType()) || "video/webm".equals(mainBlob.getMimeType()))) {
            return true;
        }
        return webmTranscodedVideo != null || mp4TranscodedVideo != null;
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

    public String getVideoWebmLink() throws PropertyException, ClientException {
        return getVideoLink("video/webm", webmTranscodedVideo);
    }

    public String getVideoMP4Link() throws PropertyException, ClientException {
        return getVideoLink("video/mp4", mp4TranscodedVideo);
    }

    protected String getVideoLink(String mimetype,
            TranscodedVideo transcodedVideo) throws PropertyException,
            ClientException {
        String url = uriInfo.getBaseUri().toASCIIString().replace("/site/", "/");
        String FILE_CONTENT = "file:content";
        Blob mainBlob = (Blob) doc.getPropertyValue(FILE_CONTENT);
        if (mainBlob != null && mimetype.equals(mainBlob.getMimeType())) {
            url += DocumentModelFunctions.bigFileUrl(doc, FILE_CONTENT,
                    mainBlob.getFilename()).replace("nullnxbigfile/",
                    "nxbigfile/");
        } else if (transcodedVideo != null) {
            String blobPropertyName = transcodedVideo.getBlobPropertyName();
            url += DocumentModelFunctions.bigFileUrl(doc, blobPropertyName,
                    transcodedVideo.getBlob().getFilename()).replace(
                    "nullnxbigfile/", "nxbigfile/");
        } else {
            return null;
        }
        return url;
    }

    public TranslationAdapter getTranslation() {
        return translation;
    }

    public String getTranslatedField(String propertyPath, String language) throws PropertyException, ClientException {
        Map<String, Map<String, Object>> translations = translation.getTranslatedFields(propertyPath);
        Map<String, Object> translation = translations.get(language);
        if (translation == null) {
            return "";
        } else {
            return (String) translation.get(TranslationAdapter.TEXT);
        }
    }
}
