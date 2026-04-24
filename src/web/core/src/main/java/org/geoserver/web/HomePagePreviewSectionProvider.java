/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.List;
import org.geoserver.catalog.PublishedInfo;

/** Contributes a preview section to the GeoServer home page. */
public interface HomePagePreviewSectionProvider {

    /** Returns whether this provider applies to the current published object. */
    default boolean supports(PublishedInfo published) {
        return true;
    }

    /** Resource key for the section title. */
    String getTitleKey();

    /** Builds the links to render in the section. */
    List<PreviewLink> getLinks(PublishedInfo published);

    /** Order of the section in the preview panel. */
    default int getOrder() {
        return 0;
    }
}
