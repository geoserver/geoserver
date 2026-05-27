/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.HomePagePreviewSectionProvider;
import org.geoserver.web.PreviewCatalogLinkSupport;
import org.geoserver.web.PreviewLink;

/** Contributes the common preview links defined in the demo module. */
public class CommonPreviewSectionProvider implements HomePagePreviewSectionProvider {

    @Override
    public String getTitleKey() {
        return PreviewCatalogLinkSupport.COMMON_FORMATS;
    }

    @Override
    public List<PreviewLink> getLinks(PublishedInfo published) {
        List<PreviewLink> links = new ArrayList<>();
        List<CommonFormatLink> formats = GeoServerApplication.get().getBeansOfType(CommonFormatLink.class);
        Collections.sort(formats);
        PreviewLayer layer = new PreviewLayer(published);
        for (CommonFormatLink link : formats) {
            PreviewLink previewLink = link.getFormatLink(layer);
            if (previewLink != null) links.add(previewLink);
        }
        return links;
    }

    @Override
    public int getPriority() {
        return PreviewCatalogLinkSupport.COMMON_FORMATS_PRIORITY;
    }
}
