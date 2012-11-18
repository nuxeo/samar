/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Olivier Grisel
 */
package fr.samar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Small DTO to precompute the thumbnail URLs for JSF and convert the timcode to
 * millisencodes
 */
public class StoryboardItem {

    public static final Log log = LogFactory.getLog(StoryboardItem.class);

    protected final DocumentModel doc;

    protected final int position;

    protected final String blobPropertyName;

    protected final String filename;

    protected final String url;

    protected String timecode = "0";

    public StoryboardItem(DocumentModel doc, String basePropertyPath,
            int position, String baseURL) {
        this.doc = doc;
        this.position = position;
        String propertyPath = basePropertyPath + "/" + position;
        blobPropertyName = propertyPath + "/content";
        filename = String.format("storyboard-%03d.jpeg", position);
        try {
            Double tc = doc.getProperty(propertyPath + "/timecode").getValue(
                    Double.class);
            if (tc != null) {
                timecode = String.format("%f", Math.floor(tc));
            }
            // TODO: read filename from blob too
        } catch (Exception e) {
            log.warn(e);
        }
        url = AnnotatedResult.bigFileUrl(doc, baseURL, blobPropertyName,
                filename);
    }

    public String getUrl() {
        return url;
    }

    public String getTimecode() {
        return timecode;
    }
}
