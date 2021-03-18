/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.geoserver.wms.featureinfo.LabelInFeatureInfo.DEFAULT_ATTRIBUTE_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geoserver.wms.map.RasterSymbolizerVisitor;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.RasterSymbolizer;

/**
 * A visitor able to produce LabelInFeatureInfo object from a Style having vendor options
 * <VendorOption name="labelInFeatureInfo">add</VendorOption> <VendorOption
 * name="labelAttributeName">custom name</VendorOption>
 */
class LabelInFeatureInfoExtractor extends RasterSymbolizerVisitor {

    static final String LABEL_IN_FEATURE_INFO = "labelInFeatureInfo";
    static final String LABEL_ATTRIBUTE_NAME = "labelAttributeName";

    List<LabelInFeatureInfo> labelInFeatureInfoList;

    LabelInFeatureInfoExtractor(double scaleDenominator) {
        super(scaleDenominator, null);
        this.labelInFeatureInfoList = new ArrayList<>();
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
                        .equals(LabelInFeatureInfo.LabelInFeatureInfoMode.NONE.name())) {
            Integer channelName = extractChannelSelectionName(raster.getChannelSelection());
            LabelInFeatureInfo labelInFeatureInfo =
                    new LabelInFeatureInfo(targetAttributeName, cm, labelIncluded, channelName);
            labelInFeatureInfoList.add(labelInFeatureInfo);
        }
    }

    public List<LabelInFeatureInfo> getLabelInFeatureInfoList() {
        return labelInFeatureInfoList;
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
