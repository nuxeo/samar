package fr.samar;

import org.nuxeo.ecm.core.api.DocumentModel;

public class URLHelper {

    public static String bigFileUrl(DocumentModel doc, String baseURL, String blobPropertyName, String filename) {
        StringBuffer bigDownloadURL = new StringBuffer(baseURL);
        bigDownloadURL.append("nxbigfile").append("/");
        bigDownloadURL.append(doc.getRepositoryName()).append("/");
        bigDownloadURL.append(doc.getRef().toString()).append("/");
        bigDownloadURL.append(blobPropertyName).append("/");
        bigDownloadURL.append(filename);
        return bigDownloadURL.toString();
    }
    
    public static String documentUrl(DocumentModel doc, String baseURL) {
        return baseURL + "nxpath/" + doc.getRepositoryName()
                + doc.getPathAsString() + "@view_documents";
    }

}
