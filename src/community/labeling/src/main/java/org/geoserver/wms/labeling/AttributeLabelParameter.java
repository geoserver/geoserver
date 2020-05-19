/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import org.opengis.filter.Filter;

/**
 * Data model class for parsing RENDERLABEL vendor parameter on WMS requests. Used by the {@link
 * AttributesGlobeKvpParser} KVP parser.
 */
public class AttributeLabelParameter {

    private final String layerName;
    private final Filter filter;
    private final AttributeGlobeFonts fonts;
    private final Set<String> attributes;

    /**
     * Constructor.
     *
     * @param layerName the layer name
     * @param filter the ECQL filter instance
     * @param attributes the string set of feature attributes to show
     */
    public AttributeLabelParameter(String layerName, Filter filter, Set<String> attributes) {
        this(layerName, filter, AttributeGlobeFonts.getDefault(), attributes);
    }

    /**
     * Constructor.
     *
     * @param layerName the layer name
     * @param filter the ECQL filter instance
     * @param fonts the fonts properties, nullable
     * @param attributes the string set of feature attributes to show
     */
    public AttributeLabelParameter(
            String layerName, Filter filter, AttributeGlobeFonts fonts, Set<String> attributes) {
        this.layerName = requireNonNull(layerName);
        this.filter = requireNonNull(filter);
        this.fonts = fonts;
        this.attributes = requireNonNull(attributes);
    }

    public Filter getFilter() {
        return filter;
    }

    public String getLayerName() {
        return layerName;
    }

    public AttributeGlobeFonts getFonts() {
        return fonts;
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "AttributeLabelParameter [layerName="
                + layerName
                + ", filter="
                + filter
                + ", fonts="
                + fonts
                + ", attributes="
                + attributes
                + "]";
    }
}
