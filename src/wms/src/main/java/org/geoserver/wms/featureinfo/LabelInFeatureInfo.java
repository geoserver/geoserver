/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;

/**
 * This class represents two <RasterSymbolizer></RasterSymbolizer> vendor options used to control
 * the presence of a style a label in a GetFeatureInfo request, namely <VendorOption
 * name="labelInFeatureInfo">add</VendorOption> <VendorOption name="labelAttributeName">custom
 * name</VendorOption> It provides also the necessary functionality to match a pixel value on a
 * ColorMapEntry label
 */
class LabelInFeatureInfo {

    static final String DEFAULT_ATTRIBUTE_NAME = "Label";

    String attributeName;
    ColorMap colorMap;
    String labelInclusion;
    Integer channel;

    LabelInFeatureInfo(
            String attributeName, ColorMap colorMap, String labelInclusion, Integer channel) {

        String labelInclusionUpper = labelInclusion.toUpperCase();
        if (!LabelInFeatureInfoMode.getAsStringList().contains(labelInclusionUpper))
            throw new RuntimeException(
                    "Unsupported labelInFeatureInfo VendorOption value "
                            + labelInclusion
                            + ". It should be one of add, replace, none");

        this.attributeName = attributeName;
        this.colorMap = colorMap;
        this.labelInclusion = labelInclusionUpper;
        this.channel = channel;
    }

    String getAttributeName() {
        return attributeName;
    }

    Integer getChannel() {
        return channel;
    }

    /**
     * Give a pixel value match it to a label of a ColorMapEntry in the ColorMap
     *
     * @param pixel the pixel value
     * @return the label from the matched ColorMapEntry
     */
    String getLabelForPixel(double pixel) {
        int type = colorMap.getType();
        ColorMapEntry[] entries = colorMap.getColorMapEntries();
        String label = null;
        switch (type) {
            case ColorMap.TYPE_RAMP:
                label = getLabelForPixelRamp(pixel, entries);
                break;
            case ColorMap.TYPE_INTERVALS:
                label = getLabelForPixelIntervals(pixel, entries);
                break;
            case ColorMap.TYPE_VALUES:
                label = getLabelForPixelValue(pixel, entries);
                break;
        }
        return label;
    }

    private String getLabelForPixelRamp(double pixel, ColorMapEntry[] entries) {
        String label = null;
        for (int i = 0; i < entries.length; i++) {
            ColorMapEntry current = entries[i];
            double currentVal = current.getQuantity().evaluate(null, Double.class);
            if (i == 0 && pixel <= currentVal) {
                label = current.getLabel();
                break;
            } else if (pixel <= currentVal) {
                ColorMapEntry prev = entries[i - 1];
                double prevValue = prev.getQuantity().evaluate(null, Double.class);
                double diffWithCurr = Math.abs(pixel - currentVal);
                double diffWithPrev = Math.abs(pixel - prevValue);
                if (diffWithCurr < diffWithPrev) label = current.getLabel();
                else label = prev.getLabel();
                break;
            }
        }
        return label;
    }

    private String getLabelForPixelValue(double pixel, ColorMapEntry[] entries) {
        String label = null;
        for (ColorMapEntry entry : entries) {
            double quantity = entry.getQuantity().evaluate(null, Double.class);
            if (pixel == quantity) {
                label = entry.getLabel();
                break;
            }
        }
        return label;
    }

    private String getLabelForPixelIntervals(double pixel, ColorMapEntry[] entries) {
        String label = null;
        for (int i = 1; i < entries.length; i++) {
            ColorMapEntry current = entries[i];
            ColorMapEntry prev = entries[i - 1];
            double currentQuantity = current.getQuantity().evaluate(null, Double.class);
            double prevQuantity = entries[i - 1].getQuantity().evaluate(null, Double.class);
            if (i == 1 && pixel < prevQuantity) {
                label = prev.getLabel();
                break;
            } else if (pixel >= prevQuantity && pixel < currentQuantity) {
                label = current.getLabel();
                break;
            }
        }
        return label;
    }

    String getLabelInclusion() {
        return labelInclusion;
    }

    static int getLabelAttributeNameCount(List<LabelInFeatureInfo> labelInFeatureInfos) {
        return Long.valueOf(
                        labelInFeatureInfos
                                .stream()
                                .map(l -> l.getAttributeName() == "Label")
                                .count())
                .intValue();
    }

    static boolean isLabelReplacingValue(List<LabelInFeatureInfo> labelInFeatureInfos) {
        return !labelInFeatureInfos.isEmpty()
                && labelInFeatureInfos
                        .stream()
                        .allMatch(
                                l ->
                                        l.getLabelInclusion()
                                                .equals(LabelInFeatureInfoMode.REPLACE.name()));
    }

    enum LabelInFeatureInfoMode {
        ADD,
        REPLACE,
        NONE;

        static List<String> getAsStringList() {
            return Stream.of(LabelInFeatureInfoMode.values())
                    .map(v -> v.name())
                    .collect(Collectors.toList());
        }
    }
}
