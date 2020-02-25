/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.AbstractCatalogValidator;
import org.geotools.util.factory.GeoTools;

/**
 * Configuration validator for Web Map Service.
 *
 * @author David Winslow, OpenGeo
 */
public class WMSValidator extends AbstractCatalogValidator {

    public void validate(LayerInfo lyr, boolean isNew) {
        if (lyr.isEnabled() == false) {
            // short-circuit - for disabled layers we don't need to validate
            // anything because it won't cause service exceptions for anyone
            return;
        }

        if (lyr.getResource() == null
                || ((lyr.getResource().getSRS() == null
                                || lyr.getResource().getLatLonBoundingBox() == null)
                        && WMS.isWmsExposable(lyr))) {
            throw new RuntimeException("Layer's resource is not fully configured");
        }

        // Resource-dependent checks
        if (lyr.getType() == PublishedType.RASTER) {
            if (!(lyr.getResource() instanceof CoverageInfo))
                throw new RuntimeException(
                        "Layer with type RASTER doesn't have a coverage associated");
            CoverageInfo cvinfo = (CoverageInfo) lyr.getResource();
            try {
                cvinfo.getCatalog()
                        .getResourcePool()
                        .getGridCoverageReader(cvinfo, GeoTools.getDefaultHints());
            } catch (Throwable t) {
                throw new RuntimeException("Couldn't connect to raster layer's resource");
            }
        } else if (lyr.getType() == PublishedType.VECTOR) {
            if (!(lyr.getResource() instanceof FeatureTypeInfo))
                throw new RuntimeException(
                        "Layer with type VECTOR doesn't have a featuretype associated");
        } else if (lyr.getType()
                == PublishedType.WMTS) { // this is mostly to avoid throwing a not RASTER nor VECTOR
            // exception
            if (!(lyr.getResource() instanceof WMTSLayerInfo)) {
                throw new RuntimeException("WMTS Layer doesn't have the correct resource");
            }
        } else if (lyr.getType()
                == PublishedType.WMS) { // this is mostly to avoid throwing a not RASTER nor VECTOR
            // exception
            if (!(lyr.getResource() instanceof WMSLayerInfo)) {
                throw new RuntimeException("WMS Layer doesn't have the correct resource");
            }
        } else throw new RuntimeException("Layer is neither RASTER nor VECTOR type");

        // Style-dependent checks
        if ((lyr.getDefaultStyle() == null || lyr.getStyles().contains(null))
                && WMS.isWmsExposable(lyr)) {
            throw new RuntimeException("Layer has null styles!");
        }
    }
}
