/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles.gs.wps;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.mbtiles.MBTilesGetMapOutputFormat;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@DescribeProcess(title = "MBTiles", description = "MBTiles Process")
public class MBTilesProcess implements GeoServerProcess {

    public static final String GRIDSET_NAME = "gridset";

    public static final String EPSG_900913 = "EPSG:900913";

    private static final int TEMP_DIR_ATTEMPTS = 10000;

    private static final Logger LOGGER = Logging.getLogger(MBTilesProcess.class);

    /** GeoServer catalog */
    private Catalog catalog;

    /** {@link WPSResourceManager} used for cleaning temporary files */
    private WPSResourceManager resources;

    /** {@link MBTilesGetMapOutputFormat} instance used for creating the MBTiles file */
    private MBTilesGetMapOutputFormat mapOutput;

    public MBTilesProcess(
            Catalog catalog, MBTilesGetMapOutputFormat mapOutput, WPSResourceManager storage) {
        this.resources = storage;
        this.mapOutput = mapOutput;
        this.catalog = catalog;
    }

    public static File createTempDir(File baseDir) {
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException(
                "Failed to create directory within "
                        + TEMP_DIR_ATTEMPTS
                        + " attempts (tried "
                        + baseName
                        + "0 to "
                        + baseName
                        + (TEMP_DIR_ATTEMPTS - 1)
                        + ')');
    }

    @DescribeResult(name = "mbtile", description = "Link to Compiled MBTiles File")
    public URL execute(
            @DescribeParameter(
                        name = "layers",
                        description = "Name of the input layer",
                        collectionType = String.class
                    )
                    Collection<String> layerz,
            @DescribeParameter(name = "format", description = "Tiles format") String format,
            @DescribeParameter(
                        name = "boundingbox",
                        description = "Bounding Box of the final MBTile",
                        min = 0
                    )
                    ReferencedEnvelope boundingbox,
            @DescribeParameter(name = "filename", description = "Name of the .mbtile file", min = 0)
                    String filename,
            @DescribeParameter(
                        name = "path",
                        description = "Path to the directory where the .mbtile file can be stored ",
                        min = 0
                    )
                    URL path,
            @DescribeParameter(
                        name = "minZoom",
                        description = "Minimum Zoom level to generate",
                        min = 0
                    )
                    Integer minZoom,
            @DescribeParameter(
                        name = "maxZoom",
                        description = "Maximum Zoom level to generate",
                        min = 0
                    )
                    Integer maxZoom,
            @DescribeParameter(name = "minRow", description = "Minimum Row to generate", min = 0)
                    Integer minRow,
            @DescribeParameter(name = "maxRow", description = "Maximum Row to generate", min = 0)
                    Integer maxRow,
            @DescribeParameter(
                        name = "minColumn",
                        description = "Minimum Column to generate",
                        min = 0
                    )
                    Integer minColumn,
            @DescribeParameter(
                        name = "maxColumn",
                        description = "Maximum Column to generate",
                        min = 0
                    )
                    Integer maxColumn,
            @DescribeParameter(name = "bgColor", description = "Background color", min = 0)
                    String bgColor,
            @DescribeParameter(
                        name = "transparency",
                        description = "Transparency enabled or not",
                        min = 0,
                        defaultValue = "false"
                    )
                    Boolean transparency,
            @DescribeParameter(
                        name = "styleNames",
                        description = "Name of the styles to use",
                        min = 0,
                        collectionType = String.class
                    )
                    Collection<String> styleNames,
            @DescribeParameter(
                        name = "stylePath",
                        description = "Path of the style to use",
                        min = 0
                    )
                    URL stylePath,
            @DescribeParameter(
                        name = "styleBody",
                        description = "Body of the style to use",
                        min = 0
                    )
                    String styleBody)
            throws IOException {

        // Extract the filename if present
        String name;

        if (filename != null && !filename.isEmpty()) {
            name = filename;
        } else if (!layerz.isEmpty()) {
            String firstLayer = layerz.iterator().next();
            name = firstLayer.substring(firstLayer.lastIndexOf(":") + 1);
        } else {
            throw new ProcessException("Layers parameter is empty");
        }

        // Initial check on the layers and styleNames size
        if (styleNames != null && styleNames.size() != layerz.size()) {
            throw new ProcessException("Layers and styleNames must have the same size");
        }

        // Extract the file path if present
        final File file;

        String outputResourceName = name + ".mbtiles";
        if (path != null) {
            File urlToFile = URLs.urlToFile(path);
            urlToFile.mkdirs();
            file = new File(urlToFile, outputResourceName);
        } else {
            final Resource resource = resources.getOutputResource(null, outputResourceName);
            file = resource.file();
        }

        // Create the MBTile file
        MBTilesFile mbtile = new MBTilesFile(file, true);
        try {
            // Initialize the MBTile file in order to avoid exceptions when accessing the geoPackage
            // file
            mbtile.init();

            // Create the GetMap request to use
            GetMapRequest request = new GetMapRequest();

            // Create the layers map for the request
            ArrayList<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();

            // Get the layers from the catalog
            for (String layername : layerz) {
                LayerInfo layerInfo = catalog.getLayerByName(layername);
                // Ensure the Layer is present
                if (layerInfo == null) {
                    throw new ServiceException("Layer not found: " + layername);
                }

                layers.add(new MapLayerInfo(layerInfo));
            }

            request.setLayers(layers);

            // Generate the bounding box if not present
            if (boundingbox == null) {
                try {

                    // generate one from requests layers

                    ReferencedEnvelope bbox = null;
                    for (MapLayerInfo l : request.getLayers()) {
                        ResourceInfo r = l.getResource();
                        // use native bbox
                        ReferencedEnvelope b = r.getNativeBoundingBox();
                        if (bbox != null) {
                            // transform
                            b = b.transform(bbox.getCoordinateReferenceSystem(), true);
                        }

                        if (bbox != null) {
                            bbox.include(b);
                        } else {
                            bbox = b;
                        }
                    }

                    request.setBbox(bbox);
                } catch (Exception e) {
                    String msg = "Must specify bbox, unable to derive from requested layers";
                    throw new RuntimeException(msg, e);
                }
            } else {
                request.setBbox(boundingbox);
            }

            // Extract CRS
            CoordinateReferenceSystem crs = boundingbox.getCoordinateReferenceSystem();

            // Set the request CRS
            if (crs == null) {
                // use crs of the layer
                ResourceInfo r = request.getLayers().iterator().next().getResource();
                crs = r.getCRS();
                request.setCrs(crs);
            } else {
                request.setCrs(crs);
            }

            // Set the request SRS
            request.setSRS(CRS.toSRS(crs));

            // Set Background color and Transparency
            if (bgColor != null && !bgColor.isEmpty()) {
                request.setBgColor(Color.decode(bgColor));
            }
            request.setTransparent(transparency == null ? false : transparency);

            // Add a style
            if (stylePath != null) {
                request.setStyleUrl(stylePath);
            } else if (styleBody != null && !styleBody.isEmpty()) {
                request.setStyleBody(styleBody);
            } else {
                request.setStyles(new ArrayList<Style>());
                if (styleNames != null && !styleNames.isEmpty()) {
                    for (String styleName : styleNames) {
                        StyleInfo info = catalog.getStyleByName(styleName);
                        if (info != null) {
                            request.getStyles().add(info.getStyle());
                        } else {
                            request.getStyles().add(null);
                        }
                    }
                }
                if (request.getStyles().isEmpty()) {
                    for (MapLayerInfo info : request.getLayers()) {
                        request.getStyles().add(info.getDefaultStyle());
                    }
                }
            }
            // Set the format of the mbtiles images
            request.setFormat("none");
            Map formatOptions = new HashMap();
            formatOptions.put("format", format);
            // Configure zoom levels if present
            if (minZoom != null) {
                formatOptions.put("min_zoom", minZoom);
            }
            if (maxZoom != null) {
                formatOptions.put("max_zoom", maxZoom);
            }
            if (minColumn != null) {
                formatOptions.put("min_column", minColumn);
            }
            if (maxColumn != null) {
                formatOptions.put("max_column", maxColumn);
            }
            if (minRow != null) {
                formatOptions.put("min_row", minRow);
            }
            if (maxRow != null) {
                formatOptions.put("max_row", maxRow);
            }
            // Set the gridSet name
            formatOptions.put(GRIDSET_NAME, EPSG_900913);
            // Add the format options to the request
            request.setFormatOptions(formatOptions);

            // Execute the requests
            mapOutput.addTiles(mbtile, request, name);
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            throw new ProcessException(e);
        } finally {
            // Close the connection
            if (mbtile != null) {
                try {
                    mbtile.close();
                } catch (Exception e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }

        // Add to storage only if it is a temporary file
        if (path != null) {
            return URLs.fileToUrl(file);
        } else {
            return new URL(
                    resources.getOutputResourceUrl(outputResourceName, "application/x-mbtiles"));
        }
    }
}
