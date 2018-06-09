/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.Serializable;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * The GUI configuration for a raster layer. For the time being just the name, but expect to see
 * bbox extraction and resolution setting (probably ND slicing for ND coverages?)
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class RasterLayerConfiguration implements Serializable {
    String layerName;

    ReferencedEnvelope spatialDomain;

    public ReferencedEnvelope getSpatialDomain() {
        return spatialDomain;
    }

    public void setSpatialDomain(ReferencedEnvelope spatialDomain) {
        this.spatialDomain = spatialDomain;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }
}
