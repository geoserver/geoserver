/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.context.ApplicationContext;

/**
 * Various Utilities for Download Services.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
final class DownloadUtilities {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(DownloadUtilities.class);

    /** Singleton */
    private DownloadUtilities() {}

    /**
     * This method checks whether or not the provided geometry is valid {@link Polygon} or not.
     *
     * <p>In case the egometry is not a valid polygon, it throws an {@link IllegalStateException};
     *
     * @param roi the {@link Geometry} to check.
     */
    static void checkPolygonROI(Geometry roi) throws IllegalStateException {
        // Null check
        if (roi == null) {
            throw new NullPointerException("The provided ROI is null!");
        }
        // Check that the Geometry is only Polygon or MultiPolygon
        if (roi instanceof Point
                || roi instanceof MultiPoint
                || roi instanceof LineString
                || roi instanceof MultiLineString) {
            throw new IllegalStateException(
                    "The Region of Interest is not a Polygon or Multipolygon!");
        }
        // Empty check and validity check
        if (roi.isEmpty() || !roi.isValid()) {
            throw new IllegalStateException("The Region of Interest is empyt or invalid!");
        }
    }

    /**
     * Looks for a valid PPIO given the provided mime type and process parameter.
     *
     * <p>The lenient approach makes this method try harder to send back a result but it is
     * preferrable to be non-lenient since otherwise we might get s a PPIO which is not really what
     * we need.
     *
     * @param mime the mime-type for which we are searching for a {@link ProcessParameterIO}
     * @param lenient whether or not trying to be lenient when returning a suitable {@link
     *     ProcessParameterIO}.
     * @return either <code>null</code> or the found
     */
    static final ProcessParameterIO find(
            Parameter<?> p, ApplicationContext context, String mime, boolean lenient) {
        //
        // lenient approach, try to give something back in any case
        //
        if (lenient) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Lenient approach used");
            }
            return ProcessParameterIO.find(p, context, mime);
        }

        //
        // Strict match case. If we don't find a match we return null
        //
        // enum special treatment
        if (p.type.isEnum()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Trying to find the PPIO for the Enum = " + p.type);
            }
            return new LiteralPPIO(p.type);
        }

        // TODO: come up with some way to flag one as "default"
        List<ProcessParameterIO> all = ProcessParameterIO.findAll(p, context);
        if (all.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "No PPIO found for the parameter " + p.getName());
            }
            return null;
        }

        // Get the PPIO for the mimetype
        if (mime != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE, "Trying to search for a PPIO for the parameter " + p.getName());
            }
            for (ProcessParameterIO ppio : all) {
                if (ppio instanceof ComplexPPIO
                        && ((ComplexPPIO) ppio).getMimeType().equals(mime)) {
                    return ppio;
                }
            }
        }

        // unable to find a match
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Unable to find a PPIO for the parameter " + p.getName());
        }
        return null;
    }

    /**
     * This methods checks if the provided {@link FeatureCollection} is empty or not.
     *
     * <p>In case the provided feature collection is empty it throws an {@link
     * IllegalStateException};
     *
     * @param features the {@link SimpleFeatureCollection} to check
     */
    static final void checkIsEmptyFeatureCollection(SimpleFeatureCollection features)
            throws IllegalStateException {
        if (features == null || features.isEmpty()) {
            throw new IllegalStateException("Got an empty feature collection.");
        }
    }

    /**
     * Retrieves the native {@link CoordinateReferenceSystem} for the provided {@link ResourceInfo}.
     *
     * @return the native {@link CoordinateReferenceSystem} for the provided {@link ResourceInfo}.
     * @throws IOException in case something bad happems!
     */
    static CoordinateReferenceSystem getNativeCRS(ResourceInfo resourceInfo) throws IOException {
        // prepare native CRS
        ProjectionPolicy pp = resourceInfo.getProjectionPolicy();
        if (pp == null || pp == ProjectionPolicy.FORCE_DECLARED) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "PropjectionPolicy null or FORCE_DECLARED");
            }
            return resourceInfo.getCRS();
        } else {
            return resourceInfo.getNativeCRS();
        }
    }

    /**
     * Reprojects the input Geometry from its CRS to the defined CRS.
     *
     * @param geometry Geometry to transform
     * @param crs target CRS for the transformation
     * @return a transformed Geometry object
     */
    static Geometry transformGeometry(Geometry geometry, CoordinateReferenceSystem crs)
            throws IOException {
        final CoordinateReferenceSystem geometryCRS =
                (CoordinateReferenceSystem) geometry.getUserData();
        // find math transform between the two coordinate reference systems
        MathTransform targetTX = null;
        if (!CRS.equalsIgnoreMetadata(geometryCRS, crs)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Geometry CRS is not equal to the target CRS, we might have to reproject");
            }
            // we MIGHT have to reproject
            try {
                targetTX = CRS.findMathTransform(geometryCRS, crs, true);
            } catch (Exception e) {
                throw new IOException(e);
            }
            // reproject
            if (!targetTX.isIdentity()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "CRS transform is not an identity, we have to reproject the Geometry");
                }
                try {
                    geometry = JTS.transform(geometry, targetTX);
                } catch (Exception e) {
                    throw new IOException(e);
                }

                // checks
                if (geometry == null) {
                    throw new IllegalStateException(
                            "The Region of Interest is null after going back to native CRS!");
                }
                geometry.setUserData(crs); // set new CRS
                DownloadUtilities.checkPolygonROI(
                        geometry); // Check if the geometry is a Polygon or MultiPolygon
            }
        }
        return geometry;
    }

    /**
     * Retrieves the underlying SLD {@link File} for the provided GeoSerevr Style.
     *
     * @param style the underlying SLD {@link File} for the provided GeoSerevr Style.
     * @return the underlying SLD {@link File} for the provided GeoSerevr Style.
     */
    static Resource findStyle(StyleInfo style) throws IOException {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource styleFile = loader.get(Paths.path("styles", style.getFilename()));
        if (styleFile != null
                && styleFile.getType() == Resource.Type.RESOURCE
                && Resources.canRead(styleFile)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Style " + style.getName() + " found");
            }
            // the SLD file is public and avaialble, we can attach it to the download.
            return styleFile;
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Style "
                                + style.getName()
                                + " not found. Trying to search in the layer workspace");
            }
            // the SLD file is not public, most probably it is located under a workspace.
            // lets try to search for the file inside the same layer workspace folder ...
            styleFile =
                    loader.get(
                            Paths.path(
                                    "workspaces",
                                    style.getWorkspace().getName(),
                                    "styles",
                                    style.getFilename()));

            if (styleFile != null
                    && styleFile.getType() == Resource.Type.RESOURCE
                    && Resources.canRead(styleFile)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "The style file cannot be found anywhere. We need to skip the SLD file");
                }
                // unfortunately the style file cannot be found anywhere. We need to skip the SLD
                // file!
                return null;
            }
            return styleFile;
        }
    }

    /**
     * Collect all the underlying SLD {@link File}s for the provided GeoServer layer.
     *
     * @param layerInfo the provided GeoServer layer.
     * @return all the underlying SLD {@link File}s for the provided GeoServer layer.
     */
    static List<Resource> collectStyles(LayerInfo layerInfo) throws IOException {
        final List<Resource> styleFiles = new ArrayList<Resource>();

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Searching for default style");
        }

        // collect in a set to avoid duplicates (the styles can contain a copy of the
        // default style)
        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<>();
        styles.add(layerInfo.getDefaultStyle());
        if (layerInfo.getStyles() != null) {
            styles.addAll(layerInfo.getStyles());
        }

        for (StyleInfo si : styles) {
            Resource styleFile = findStyle(si);
            if (styleFile != null) {
                styleFiles.add(styleFile);
            }
        }
        return styleFiles;
    }
}
