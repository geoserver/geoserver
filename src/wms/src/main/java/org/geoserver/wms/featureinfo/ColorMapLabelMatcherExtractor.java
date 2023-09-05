/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.geoserver.wms.featureinfo.ColorMapLabelMatcher.DEFAULT_ATTRIBUTE_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geoserver.wms.map.RasterSymbolizerVisitor;
import org.geotools.api.style.ChannelSelection;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;

/**
 * A visitor able to produce ColorMapLabelMatcher objects from a Style having vendor options
 * <VendorOption name="labelInFeatureInfo">add</VendorOption> <VendorOption
 * name="labelAttributeName">custom name</VendorOption>
 */
class ColorMapLabelMatcherExtractor extends RasterSymbolizerVisitor {

    static final String LABEL_IN_FEATURE_INFO = "labelInFeatureInfo";
    static final String LABEL_ATTRIBUTE_NAME = "labelAttributeName";

    List<ColorMapLabelMatcher> colorMapLabelMatcherList;

    ColorMapLabelMatcherExtractor(double scaleDenominator) {
        super(scaleDenominator, null);
        this.colorMapLabelMatcherList = new ArrayList<>();
    }

    @Override
    public void visit(RasterSymbolizer raster) {
        super.visit(raster);
        ColorMap cm = raster.getColorMap();
        Map<String, String> vendorOptions = raster.getOptions();
        String labelIncluded = vendorOptions.get(LABEL_IN_FEATURE_INFO);
        String targetAttributeName = vendorOptions.get(LABEL_ATTRIBUTE_NAME);

        if (targetAttributeName == null) targetAttributeName = DEFAULT_ATTRIBUTE_NAME;

        if (labelIncluded != null
                && !labelIncluded
                        .toUpperCase()
                        .equals(ColorMapLabelMatcher.LabelInFeatureInfoMode.NONE.name())) {
            Integer channelName = extractChannelSelectionName(raster.getChannelSelection());
            ColorMapLabelMatcher colorMapLabelMatcher =
                    new ColorMapLabelMatcher(targetAttributeName, cm, labelIncluded, channelName);
            colorMapLabelMatcherList.add(colorMapLabelMatcher);
        }
    }

    public List<ColorMapLabelMatcher> getColorMapLabelMatcherList() {
        return colorMapLabelMatcherList;
    }

    private Integer extractChannelSelectionName(ChannelSelection channelSelection) {
        Integer channelName = null;
        if (channelSelection != null && channelSelection.getGrayChannel() != null)
            channelName =
                    channelSelection
                            .getGrayChannel()
                            .getChannelName()
                            .evaluate(null, Integer.class);
        return channelName;
    }
}
