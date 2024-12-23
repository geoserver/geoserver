/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.wms.WMS;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.style.UserLayer;
import org.geotools.util.NumberRange;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Provides utility methods required to build the capabilities document.
 *
 * @author Mauricio Pazos
 */
public final class CapabilityUtil {

    public static final String LAYER_GROUP_STYLE_NAME = "default-style";
    protected static final String LAYER_GROUP_STYLE_TITLE_PREFIX = "";
    protected static final String LAYER_GROUP_STYLE_TITLE_SUFFIX = " style";
    protected static final String LAYER_GROUP_STYLE_ABSTRACT_PREFIX = "Default style for ";
    protected static final String LAYER_GROUP_STYLE_ABSTRACT_SUFFIX = " layer";

    private CapabilityUtil() {
        // utility class
    }

    /** Helper method: aggregates min/max scale denominators of a set of styles. */
    public static NumberRange<Double> searchMinMaxScaleDenominator(Set<StyleInfo> styles) throws IOException {
        // searches the maximum and minimum denominator in the style's rules that are contained in
        // the style set.
        MinMaxDenominator minMaxDenominator = new MinMaxDenominator(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

        for (StyleInfo styleInfo : styles) {
            Optional<StyledLayer[]> styledLayers =
                    Optional.ofNullable(styleInfo.getSLD()).map(StyledLayerDescriptor::getStyledLayers);
            if (styledLayers.isPresent()) {
                for (StyledLayer styledLayer : styledLayers.get()) {
                    List<Style> stylesList = Collections.emptyList();
                    if (styledLayer instanceof NamedLayer) {
                        NamedLayer namedLayer = (NamedLayer) styledLayer;
                        stylesList = namedLayer.styles();
                    } else if (styledLayer instanceof UserLayer) {
                        UserLayer userLayer = (UserLayer) styledLayer;
                        stylesList = userLayer.userStyles();
                    }
                    for (Style style : stylesList) {
                        populateMinMaxScaleDenominator(style, minMaxDenominator);
                    }
                }
            } else {
                if (styleInfo.getStyle() != null) {
                    populateMinMaxScaleDenominator(styleInfo.getStyle(), minMaxDenominator);
                }
            }
        }

        // If the initial values weren't changed by any rule in the previous step,
        // then the default values, Min=0.0 and Max=infinity, are set.
        if (minMaxDenominator.getMin() == Double.POSITIVE_INFINITY) {
            minMaxDenominator.setMin(0.0);
        }
        if (minMaxDenominator.getMax() == Double.NEGATIVE_INFINITY) {
            minMaxDenominator.setMax(Double.POSITIVE_INFINITY);
        }
        assert minMaxDenominator.getMin() <= minMaxDenominator.getMax() : "Min <= Max scale is expected";

        return new NumberRange<>(Double.class, minMaxDenominator.getMin(), minMaxDenominator.getMax());
    }

    /**
     * Populates the Min and Max scale denominators from the style's rules.
     *
     * @param style Style to be analyzed
     * @param minMaxDenominator Min and Max scale denominators
     */
    private static void populateMinMaxScaleDenominator(Style style, MinMaxDenominator minMaxDenominator) {
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule rule : fts.rules()) {
                if (rule.getMinScaleDenominator() < minMaxDenominator.getMin()) {
                    minMaxDenominator.setMin(rule.getMinScaleDenominator());
                }
                if (rule.getMaxScaleDenominator() > minMaxDenominator.getMax()) {
                    minMaxDenominator.setMax(rule.getMaxScaleDenominator());
                }
            }
        }
    }

    /**
     * Searches the Max and Min scale denominators in the layer's styles.
     *
     * <pre>
     * If the Min or Max values aren't present, the following default are assumed:
     *
     * Min Scale: 0.0
     * Max Scale: infinity
     * </pre>
     *
     * @return Max and Min denominator
     */
    public static NumberRange<Double> searchMinMaxScaleDenominator(final LayerInfo layer) throws IOException {

        Set<StyleInfo> stylesCopy;
        StyleInfo defaultStyle;
        synchronized (layer) {
            stylesCopy = new HashSet<>(layer.getStyles());
            defaultStyle = layer.getDefaultStyle();
        }
        if (!stylesCopy.contains(defaultStyle)) {
            stylesCopy.add(defaultStyle);
        }

        return searchMinMaxScaleDenominator(stylesCopy);
    }

    /** Helper method: recursively collects all styles in a layergroup */
    private static void findLayerGroupStyles(LayerGroupInfo layerGroup, Set<StyleInfo> stylesCopy) {
        synchronized (layerGroup) {
            for (int i = 0; i < layerGroup.getLayers().size(); i++) {
                StyleInfo styleInfo = layerGroup.getStyles().get(i);
                if (styleInfo == null) {
                    PublishedInfo publishedInfo = layerGroup.getLayers().get(i);
                    if (publishedInfo instanceof LayerInfo) {
                        styleInfo = ((LayerInfo) publishedInfo).getDefaultStyle();
                        stylesCopy.add(styleInfo);
                    } else if (publishedInfo instanceof LayerGroupInfo) {
                        findLayerGroupStyles((LayerGroupInfo) publishedInfo, stylesCopy);
                    }
                } else {
                    stylesCopy.add(styleInfo);
                }
            }
        }
    }

    /**
     * Searches the Max and Min scale denominators in the layergroup's layers
     *
     * <pre>
     * If the Min or Max values aren't present, the following default are assumed:
     *
     * Min Scale: 0.0
     * Max Scale: infinity
     * </pre>
     *
     * @return Max and Min denominator
     */
    public static NumberRange<Double> searchMinMaxScaleDenominator(final LayerGroupInfo layerGroup) throws IOException {

        Set<StyleInfo> stylesCopy = new HashSet<>();
        findLayerGroupStyles(layerGroup, stylesCopy);

        return searchMinMaxScaleDenominator(stylesCopy);
    }

    /**
     * Searches the Max and Min scale denominators for a Published (delegates to Layer orrLayerGroup methods)
     *
     * <pre>
     * If the Min or Max values aren't present, the following default are assumed:
     *
     * Min Scale: 0.0
     * Max Scale: infinity
     * </pre>
     *
     * @return Max and Min denominator
     */
    public static NumberRange<Double> searchMinMaxScaleDenominator(final PublishedInfo publishedInfo)
            throws IOException {
        if (publishedInfo instanceof LayerInfo) {
            return searchMinMaxScaleDenominator((LayerInfo) publishedInfo);
        } else if (publishedInfo instanceof LayerGroupInfo) {
            return searchMinMaxScaleDenominator((LayerGroupInfo) publishedInfo);
        }
        throw new UnsupportedOperationException("PublishedInfo must be either Layer or Layergroup");
    }

    /**
     * Computes the rendering scale taking into account the standard pixel size and the real world scale denominator.
     *
     * @return the rendering scale.
     */
    public static Double computeScaleHint(final Double scaleDenominator) {

        // According to OGC SLD 1.0 specification: The "standardized rendering pixel size" is
        // defined to be 0.28mm × 0.28mm (millimeters).
        final Double sizeStandardRenderPixel = 0.00028; // (meters)

        Double scaleHint = Math.sqrt(Math.pow((scaleDenominator * sizeStandardRenderPixel), 2) * 2);

        return scaleHint;
    }

    /** Returns true if legend accomplish some rules to be a valid one. */
    public static boolean validateLegendInfo(LegendInfo legend) {
        return legend != null && legend.getOnlineResource() != null && legend.getHeight() > 0 && legend.getWidth() > 0;
    }

    /**
     * A Utility method to populate legend url href attribute
     *
     * @param attrs AttributesImpl to be populated with Legend URL href
     * @param legendURL URL String
     * @param XLINK_NS Namsepace like (e.g http://www.w3.org/1999/xlink)
     * @return attrs with Legend URL attributes
     */
    public static AttributesImpl addGetLegendAttributes(AttributesImpl attrs, String legendURL, String XLINK_NS) {

        attrs.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
        attrs.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
        attrs.addAttribute(XLINK_NS, "href", "xlink:href", "", legendURL);

        return attrs;
    }

    /** Checks if a default style name for layer groups should be used, or not */
    public static boolean encodeGroupDefaultStyle(WMS wms, LayerGroupInfo lgi) {
        boolean opaqueOrSingle = LayerGroupHelper.isSingleOrOpaque(lgi);
        return opaqueOrSingle || wms.isDefaultGroupStyleEnabled();
    }

    /**
     * Returns the layer group default style name (to be used when {@link #encodeGroupDefaultStyle(WMS, LayerGroupInfo)}
     * returns true)
     */
    public static String getGroupDefaultStyleName(String groupName) {
        return LAYER_GROUP_STYLE_NAME.concat("-").concat(groupName);
    }

    /**
     * Returns the layer group default style name (to be used when {@link #encodeGroupDefaultStyle(WMS, LayerGroupInfo)}
     * returns true)
     */
    public static String getGroupDefaultStyleName(LayerGroupInfo groupName) {
        return getGroupDefaultStyleName(groupName.prefixedName());
    }

    /** Stores the Min and Max scale denominators for a set of styles. */
    private static class MinMaxDenominator {
        private Double min;
        private Double max;

        /**
         * Default constructor.
         *
         * @param minScaleDenominator the minimum scale denominator
         * @param maxScaleDenominator the maximum scale denominator
         */
        public MinMaxDenominator(Double minScaleDenominator, Double maxScaleDenominator) {
            this.min = minScaleDenominator;
            this.max = maxScaleDenominator;
        }

        /**
         * Returns the maximum scale denominator.
         *
         * @return the maximum scale denominator.
         */
        public Double getMax() {
            return max;
        }

        /**
         * Returns the minimum scale denominator.
         *
         * @return the minimum scale denominator.
         */
        public Double getMin() {
            return min;
        }

        /**
         * Sets the maximum scale denominator.
         *
         * @param max the maximum scale denominator.
         */
        public void setMax(Double max) {
            this.max = max;
        }

        /**
         * Sets the minimum scale denominator.
         *
         * @param min the minimum scale denominator.
         */
        public void setMin(Double min) {
            this.min = min;
        }
    }
}
