/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.clip.ClippedFeatureSource;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.locationtech.jts.geom.Geometry;

abstract class AbstractVectorLayerIdentifier
        implements LayerIdentifier<FeatureSource<? extends FeatureType, ? extends Feature>> {

    private static final double TOLERANCE = 1e-6;

    @Override
    public boolean canHandle(MapLayerInfo layer) {
        int type = layer.getType();
        return type == MapLayerInfo.TYPE_VECTOR || type == MapLayerInfo.TYPE_REMOTE_VECTOR;
    }

    /** Selects the rules active at this zoom level */
    protected List<Rule> getActiveRules(Style style, double scaleDenominator) {
        List<Rule> result = new ArrayList<>();

        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule r : fts.rules()) {
                if ((r.getMinScaleDenominator() - TOLERANCE <= scaleDenominator)
                        && (r.getMaxScaleDenominator() + TOLERANCE > scaleDenominator)
                        && r.symbolizers() != null
                        && r.symbolizers().size() > 0) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    @Override
    public FeatureSource<? extends FeatureType, ? extends Feature> handleClipParam(
            FeatureInfoRequestParameters params,
            FeatureSource<? extends FeatureType, ? extends Feature> featureSource) {
        Geometry clipGeom = params.getGetMapRequest().getClip();
        if (clipGeom == null) return featureSource;
        return new ClippedFeatureSource<>(featureSource, clipGeom);
    }
}
