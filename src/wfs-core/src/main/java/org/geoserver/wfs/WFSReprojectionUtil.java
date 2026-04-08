/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.catalog.ResourcePool;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;

/**
 * Utility class used to handle common WFS reprojection issues
 *
 * @author Andrea Aime, TOPP
 */
class WFSReprojectionUtil {

    static FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());

    /** Returns the declared CRS given the native CRS and the request WFS version */
    public static CoordinateReferenceSystem getDeclaredCrs(CoordinateReferenceSystem nativeCRS, String wfsVersion) {
        try {
            if (nativeCRS == null) return null;

            if (wfsVersion.equals("1.0.0")) {
                return nativeCRS;
            } else {
                String code = GML2EncodingUtils.toURI(nativeCRS, SrsSyntax.OGC_URN_EXPERIMENTAL, false);
                // it's possible that we can't do the CRS -> code -> CRS conversion...so we'll just
                // return what we have
                try {
                    return code == null ? nativeCRS : CRS.decode(code);
                } catch (NoSuchAuthorityCodeException e) {
                    String identifier = ResourcePool.lookupIdentifier(nativeCRS, true);
                    if (identifier != null) {
                        CoordinateReferenceSystem equivalentCRS = CRS.decode(identifier);
                        return getDeclaredCrs(equivalentCRS, wfsVersion);
                    } else {
                        // we can only use the CRS as is, not part of the EPSG database, so we
                        // cannot determine the EPSG native axis order
                        return nativeCRS;
                    }
                }
            }
        } catch (Exception e) {
            throw new WFSException("We have had issues trying to flip axis of " + nativeCRS, e);
        }
    }

    /** Returns the declared CRS given a feature type and the request WFS version */
    public static CoordinateReferenceSystem getDeclaredCrs(FeatureType schema, String wfsVersion) {
        if (schema == null) return null;

        CoordinateReferenceSystem crs = (schema.getGeometryDescriptor() != null)
                ? schema.getGeometryDescriptor().getCoordinateReferenceSystem()
                : null;

        return getDeclaredCrs(crs, wfsVersion);
    }

    /** Applies a default CRS to all geometric filter elements that do not already have one */
    public static Filter applyDefaultCRS(Filter filter, CoordinateReferenceSystem defaultCRS) {
        DefaultCRSFilterVisitor defaultVisitor = new DefaultCRSFilterVisitor(ff, defaultCRS);
        return (Filter) filter.accept(defaultVisitor, null);
    }

    /** Reprojects all geometric filter elements to the native CRS of the provided schema */
    public static Filter reprojectFilter(Filter filter, FeatureType schema) {
        ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(ff, schema);
        return (Filter) filter.accept(visitor, null);
    }

    /**
     * Reprojects all geometric filter elements to the native CRS of the provided schema or to the target CRS if not
     * NULL.
     */
    public static Filter reprojectFilter(Filter filter, FeatureType schema, CoordinateReferenceSystem targetCrs) {
        ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(ff, schema, targetCrs);
        return (Filter) filter.accept(visitor, null);
    }

    /**
     * Convenience method, same as calling {@link #applyDefaultCRS} and then {@link #reprojectFilter(Filter,
     * FeatureType)} in a row
     */
    public static Filter normalizeFilterCRS(Filter filter, FeatureType schema, CoordinateReferenceSystem defaultCRS) {
        Filter defaulted = applyDefaultCRS(filter, defaultCRS);
        return reprojectFilter(defaulted, schema);
    }

    /**
     * Convenience method, same as calling {@link #applyDefaultCRS} and then {@link #reprojectFilter(Filter,
     * FeatureType, CoordinateReferenceSystem)} in a row. If a non NULL target CRS is provided it will be used as the
     * target CRS overriding the native CRS.
     */
    public static Filter normalizeFilterCRS(
            Filter filter,
            FeatureType schema,
            CoordinateReferenceSystem defaultCRS,
            CoordinateReferenceSystem targetCRS) {
        Filter defaulted = applyDefaultCRS(filter, defaultCRS);
        return reprojectFilter(defaulted, schema, targetCRS);
    }
}
