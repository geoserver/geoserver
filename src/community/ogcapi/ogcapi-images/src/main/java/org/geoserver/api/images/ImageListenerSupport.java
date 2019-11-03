/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.geotools.gce.imagemosaic.Utils.FF;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

/** Propagates image listener events to all listeners */
class ImageListenerSupport {

    private List<ImageListener> imageListeners;

    public ImageListenerSupport(List<ImageListener> imageListeners) {
        this.imageListeners = imageListeners;
    }

    void imageAdded(CoverageInfo coverageInfo, GranuleSource granules, String featureId)
            throws IOException {
        SimpleFeatureCollection fc =
                granules.getGranules(
                        new Query(
                                coverageInfo.getNativeCoverageName(),
                                FF.id(FF.featureId(featureId))));
        SimpleFeature feature = DataUtilities.first(fc);

        if (feature != null && imageListeners != null) {
            for (ImageListener listener : imageListeners) {
                listener.imageAdded(coverageInfo, feature);
            }
        }
    }

    void imageRemoved(CoverageInfo coverageInfo, SimpleFeature feature) {
        if (feature != null && imageListeners != null) {
            for (ImageListener listener : imageListeners) {
                listener.imageRemoved(coverageInfo, feature);
            }
        }
    }
}
