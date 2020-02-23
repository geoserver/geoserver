/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.NumberRange;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Provides utility methods required to build the capabilities document.
 *
 * @author Mauricio Pazos
 */
public final class CapabilityUtil {

    private CapabilityUtil() {
        // utility class
    }

    /** Helper method: aggregates min/max scale denominators of a set of styles. */
    public static NumberRange<Double> searchMinMaxScaleDenominator(Set<StyleInfo> styles)
            throws IOException {
        // searches the maximum and minimum denominator in the style's rules that are contained in
        // the style set.
        double minScaleDenominator = Double.POSITIVE_INFINITY;
        double maxScaleDenominator = Double.NEGATIVE_INFINITY;

        for (StyleInfo styleInfo : styles) {
            Style style = styleInfo.getStyle();
            for (FeatureTypeStyle fts : style.featureTypeStyles()) {

                for (Rule rule : fts.rules()) {

                    if (rule.getMinScaleDenominator() < minScaleDenominator) {
                        minScaleDenominator = rule.getMinScaleDenominator();
                    }
                    if (rule.getMaxScaleDenominator() > maxScaleDenominator) {
                        maxScaleDenominator = rule.getMaxScaleDenominator();
                    }
                }
            }
        }

        // If the initial values weren't changed by any rule in the previous step,
        // then the default values, Min=0.0 and Max=infinity, are set.
        if (minScaleDenominator == Double.POSITIVE_INFINITY) {
            minScaleDenominator = 0.0;
        }
        if (maxScaleDenominator == Double.NEGATIVE_INFINITY) {
            maxScaleDenominator = Double.POSITIVE_INFINITY;
        }
        assert minScaleDenominator <= maxScaleDenominator : "Min <= Max scale is expected";

        return new NumberRange<Double>(Double.class, minScaleDenominator, maxScaleDenominator);
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
    public static NumberRange<Double> searchMinMaxScaleDenominator(final LayerInfo layer)
            throws IOException {

        Set<StyleInfo> stylesCopy;
        StyleInfo defaultStyle;
        synchronized (layer) {
            stylesCopy = new HashSet<StyleInfo>(layer.getStyles());
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
    public static NumberRange<Double> searchMinMaxScaleDenominator(final LayerGroupInfo layerGroup)
            throws IOException {

        Set<StyleInfo> stylesCopy = new HashSet<StyleInfo>();
        findLayerGroupStyles(layerGroup, stylesCopy);

        return searchMinMaxScaleDenominator(stylesCopy);
    }

    /**
     * Searches the Max and Min scale denominators for a Published (delegates to Layer orrLayerGroup
     * methods)
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
    public static NumberRange<Double> searchMinMaxScaleDenominator(
            final PublishedInfo publishedInfo) throws IOException {
        if (publishedInfo instanceof LayerInfo) {
            return searchMinMaxScaleDenominator((LayerInfo) publishedInfo);
        } else if (publishedInfo instanceof LayerGroupInfo) {
            return searchMinMaxScaleDenominator((LayerGroupInfo) publishedInfo);
        }
        throw new UnsupportedOperationException("PublishedInfo must be either Layer or Layergroup");
    }

    /**
     * Computes the rendering scale taking into account the standard pixel size and the real world
     * scale denominator.
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
        return legend != null
                && legend.getOnlineResource() != null
                && legend.getHeight() > 0
                && legend.getWidth() > 0;
    }

    /**
     * A Utility method to populate legend url href attribute
     *
     * @param attrs AttributesImpl to be populated with Legend URL href
     * @param legendURL URL String
     * @param XLINK_NS Namsepace like (e.g http://www.w3.org/1999/xlink)
     * @return attrs with Legend URL attributes
     */
    public static AttributesImpl addGetLegendAttributes(
            AttributesImpl attrs, String legendURL, String XLINK_NS) {

        attrs.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
        attrs.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
        attrs.addAttribute(XLINK_NS, "href", "xlink:href", "", legendURL);

        return attrs;
    }
}
