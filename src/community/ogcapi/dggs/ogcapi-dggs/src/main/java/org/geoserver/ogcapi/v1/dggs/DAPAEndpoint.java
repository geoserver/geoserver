/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import java.util.List;
import org.geoserver.ogcapi.AbstractDocument;

public class DAPAEndpoint extends AbstractDocument {

    String title;
    String description;
    String inputCollectionId;
    List<String> mediaTypes;
    // TODO: add external docs

    public DAPAEndpoint(String id, String inputCollectionId) {
        this.id = id;
        this.inputCollectionId = inputCollectionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInputCollectionId() {
        return inputCollectionId;
    }

    public void setInputCollectionId(String inputCollectionId) {
        this.inputCollectionId = inputCollectionId;
    }

    public List<String> getMediaTypes() {
        return mediaTypes;
    }

    public void setMediaTypes(List<String> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }
}
