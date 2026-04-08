/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.publish.LayerEditTabPanelInfo;

/** This class represent a tab for FeatureTypeInfo only */
public class SchemaTypeTabPanelInfo extends LayerEditTabPanelInfo {
    @Override
    public boolean supports(PublishedInfo pi) {
        boolean result = false;
        if (super.supports(pi)) {
            LayerInfo layerInfo = (LayerInfo) pi;
            if (layerInfo.getResource() instanceof FeatureTypeInfo) result = true;
        }
        return result;
    }
}
