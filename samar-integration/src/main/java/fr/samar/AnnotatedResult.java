package fr.samar;

import static org.nuxeo.ecm.platform.video.VideoConstants.STORYBOARD_PROPERTY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import org.nuxeo.ecm.platform.video.VideoConstants;
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

    protected final String baseURL;

    protected final String backofficeURL;

    public AnnotatedResult(DocumentModel doc, UriInfo info, String baseURL) {
        this.doc = doc;
        this.uriInfo = info;
        this.baseURL = baseURL;
        this.backofficeURL = baseURL + "nxpath/" + doc.getRepositoryName() + doc.getPathAsString() + "@view_documents";
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
        String FILE_CONTENT = "file:content";
        Blob mainBlob = (Blob) doc.getPropertyValue(FILE_CONTENT);
        if (mainBlob != null && mimetype.equals(mainBlob.getMimeType())) {
            return bigFileUrl(FILE_CONTENT, mainBlob.getFilename());
        } else if (transcodedVideo != null) {
            String blobPropertyName = transcodedVideo.getBlobPropertyName();
            return bigFileUrl(blobPropertyName,
                    transcodedVideo.getBlob().getFilename());
        } else {
            return null;
        }
    }

    public TranslationAdapter getTranslation() {
        return translation;
    }

    public String getTranslatedField(String propertyPath, String language)
            throws PropertyException, ClientException {
        Map<String, Map<String, Object>> translations = translation.getTranslatedFields(propertyPath);
        Map<String, Object> translation = translations.get(language);
        if (translation == null) {
            return "";
        } else {
            return (String) translation.get(TranslationAdapter.TEXT);
        }
    }

    public List<StoryboardItem> getStoryboard() throws PropertyException,
            ClientException {
        if (!doc.hasFacet(VideoConstants.HAS_STORYBOARD_FACET)) {
            return Collections.emptyList();
        }
        int size = doc.getProperty(STORYBOARD_PROPERTY).getValue(List.class).size();
        List<StoryboardItem> items = new ArrayList<StoryboardItem>(size);
        StoryboardItem previous = null;
        for (int i = 0; i < size; i++) {
            StoryboardItem next = new StoryboardItem(doc, STORYBOARD_PROPERTY, i, baseURL);
            next.setEndTimecode(String.valueOf(videoDocument.getVideo().getDuration()));
            if (previous != null) {
                previous.setEndTimecode(next.startTimecode);
            }
            items.add(next);
            previous = next;
        }
        return items;
    }

    public static String bigFileUrl(DocumentModel doc, String baseURL, String blobPropertyName, String filename) {
        StringBuffer bigDownloadURL = new StringBuffer(baseURL);
        bigDownloadURL.append("nxbigfile").append("/");
        bigDownloadURL.append(doc.getRepositoryName()).append("/");
        bigDownloadURL.append(doc.getRef().toString()).append("/");
        bigDownloadURL.append(blobPropertyName).append("/");
        bigDownloadURL.append(filename);
        return bigDownloadURL.toString();
    }

    public String bigFileUrl(String blobPropertyName, String filename) {
        return bigFileUrl(doc, baseURL, blobPropertyName, filename);
    }

    public String getBackofficeURL() {
        return backofficeURL;
    }
}
