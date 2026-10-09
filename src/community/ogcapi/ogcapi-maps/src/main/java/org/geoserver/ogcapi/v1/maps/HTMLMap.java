/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import java.util.List;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.springframework.http.MediaType;

/**
 * Support object to encode HTML maps (needed to respect Spring class oriented wiring. The object is encoded by
 * {@link HTMLMapMessageConverter}
 */
public class HTMLMap extends WebMap {

    private List<String> collections = List.of();
    private List<String> selectedCollections = List.of();
    private boolean collectionsCut = false;

    /** @param context the map context, can be {@code null} is there's _really_ no context around */
    public HTMLMap(WMSMapContent context) {
        super(context);
        setMimeType(MediaType.TEXT_HTML_VALUE);
    }

    /**
     * The collections the preview offers to choose from, empty for the map of a single collection, which has nothing to
     * choose.
     */
    public List<String> getCollections() {
        return collections;
    }

    /** The collections the preview draws, a subset of {@link #getCollections()}. */
    public List<String> getSelectedCollections() {
        return selectedCollections;
    }

    /** Whether {@link #getCollections()} is only part of what the dataset holds. */
    public boolean isCollectionsCut() {
        return collectionsCut;
    }

    /** Sets the layer chooser contents of a dataset map preview, and whether they are all of them. */
    public void setCollections(List<String> collections, List<String> selected, boolean cut) {
        this.collections = collections;
        this.selectedCollections = selected;
        this.collectionsCut = cut;
    }
}
