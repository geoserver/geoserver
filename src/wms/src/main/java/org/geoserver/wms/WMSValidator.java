/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.AbstractCatalogValidator;
import org.geotools.factory.GeoTools;

import com.vividsolutions.jts.geom.Geometry;

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

        if (
            lyr.getResource() == null ||
            (hasGeometry(lyr) && 
                (lyr.getResource().getSRS() == null ||
                 lyr.getResource().getLatLonBoundingBox() == null))
        ) throw new RuntimeException( "Layer's resource is not fully configured");
      
        // Resource-dependent checks
        if (lyr.getType() == LayerInfo.Type.RASTER) {
            if (!(lyr.getResource() instanceof CoverageInfo))
                throw new RuntimeException("Layer with type RASTER doesn't have a coverage associated");
            CoverageInfo cvinfo = (CoverageInfo) lyr.getResource();
            try {
                cvinfo.getCatalog().getResourcePool()
                        .getGridCoverageReader(cvinfo, GeoTools.getDefaultHints());
            } catch (Throwable t) {
                throw new RuntimeException("Couldn't connect to raster layer's resource");
            }
        } else if (lyr.getType() == LayerInfo.Type.VECTOR) {
            if (!(lyr.getResource() instanceof FeatureTypeInfo))
                throw new RuntimeException("Layer with type VECTOR doesn't have a featuretype associated");
            FeatureTypeInfo ftinfo = (FeatureTypeInfo) lyr.getResource();
        } else throw new RuntimeException("Layer is neither RASTER nor VECTOR type");

        // Style-dependent checks
        if (hasGeometry(lyr) &&
           (lyr.getDefaultStyle() == null || lyr.getStyles().contains(null))
        ) throw new RuntimeException("Layer has null styles!");
    }

    private static boolean hasGeometry(LayerInfo lyr) {
        if (lyr.getResource() instanceof CoverageInfo) return true;

        if (lyr.getResource() instanceof FeatureTypeInfo) {
            for (AttributeTypeInfo att : ((FeatureTypeInfo)lyr.getResource()).getAttributes()) {
                if (Geometry.class.isAssignableFrom(att.getBinding())) {
                    return true;
                }
            }
        }

        return false;
    }
}
