/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

package org.geoserver.gwc.wmts;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.VectorTimeDimension;

/** Common methods to test ND on vector/time */
abstract class VectorTimeTestSupport extends TestsSupport {

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        dimensionInfo.setAttribute("startTime");
        FeatureTypeInfo rasterInfo = getVectorInfo();
        Dimension dimension = new VectorTimeDimension(wms, getLayerInfo(), dimensionInfo);
        rasterInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(rasterInfo);
        return dimension;
    }

    /** Helper method that just returns the current vector info. */
    protected FeatureTypeInfo getVectorInfo() {
        LayerInfo layerInfo = getLayerInfo();
        assertThat(layerInfo.getResource(), instanceOf(FeatureTypeInfo.class));
        return (FeatureTypeInfo) layerInfo.getResource();
    }

    /** Helper method that just returns the current layer info. */
    protected LayerInfo getLayerInfo() {
        return catalog.getLayerByName(VECTOR_ELEVATION.getLocalPart());
    }
}
