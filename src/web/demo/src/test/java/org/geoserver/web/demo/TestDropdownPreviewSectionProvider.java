/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.util.List;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.HomePagePreviewSectionProvider;
import org.geoserver.web.PreviewLink;
import org.geoserver.web.PreviewSectionLayout;

public class TestDropdownPreviewSectionProvider implements HomePagePreviewSectionProvider {

    @Override
    public boolean supports(PublishedInfo published) {
        return published != null;
    }

    @Override
    public String getTitleKey() {
        return "mapFormats";
    }

    @Override
    public List<PreviewLink> getLinks(PublishedInfo published) {
        return List.of(new PreviewLink("OpenLayers", "http://example.com/preview", "OpenLayers"));
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public PreviewSectionLayout getLayout() {
        return PreviewSectionLayout.DROPDOWN;
    }
}
