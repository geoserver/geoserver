/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.List;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.platform.ExtensionPriority;

/** Contributes a preview section to the GeoServer home page. */
public interface HomePagePreviewSectionProvider extends ExtensionPriority {

    /** Returns whether this provider applies to the current published object. */
    default boolean supports(PublishedInfo published) {
        return true;
    }

    /** Resource key for the section title. */
    String getTitleKey();

    /** Builds the links to render in the section. */
    List<PreviewLink> getLinks(PublishedInfo published);

    /** Priority of the section in the preview panel. */
    @Override
    default int getPriority() {
        return ExtensionPriority.LOWEST;
    }
}
