/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.ows.util.ResponseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Contains the list of styles for a given collection */
@JsonPropertyOrder({"styles", "links"})
public class StylesDocument extends AbstractDocument {
    private final PublishedInfo published;

    public StylesDocument(PublishedInfo published) {
        this.published = published;

        addSelfLinks(
                "ogc/maps/collections"
                        + ResponseUtils.urlEncode(published.prefixedName())
                        + "/styles");
    }

    public List<StyleDocument> getStyles() {
        return getStyleInfos().stream().map(this::toDocument).collect(Collectors.toList());
    }

    private StyleDocument toDocument(StyleInfo s) {
        if (s != null) return new StyleDocument(s);
        // layer group case
        return new StyleDocument(
                StyleDocument.DEFAULT_STYLE_NAME, "Default style for " + published.prefixedName());
    }

    private List<StyleInfo> getStyleInfos() {
        List<StyleInfo> result = new ArrayList<>();
        if (published instanceof LayerInfo) {
            LayerInfo layer = (LayerInfo) this.published;
            result.addAll(layer.getStyles());
            StyleInfo defaultStyle = layer.getDefaultStyle();
            if (!result.contains(defaultStyle)) result.add(defaultStyle);
        } else if (published instanceof LayerGroupInfo) {
            // layer groups do not have a named style right now
            result.add(null);
        } else {
            throw new RuntimeException("Cannot extract styles from " + published);
        }
        return result;
    }
}
