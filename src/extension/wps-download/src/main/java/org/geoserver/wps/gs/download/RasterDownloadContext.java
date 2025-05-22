/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.gs.download;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;
import javax.media.jai.Interpolation;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * Represents the context for downloading raster data, managing parameters and processing details for raster coverage
 * extraction and transformation.
 */
class RasterDownloadContext {

    private static final Logger LOGGER = Logging.getLogger(RasterDownloadContext.class);
    final Map<String, Serializable> coverageParameters;
    final CRSRequestHandler crsRequestHandler;
    public Integer[] targetSize;
    public double[] backgroundValues;
    public GridGeometry2D requestedGridGeometry;
    public Geometry roi;
    public boolean clip;
    public int[] bandIndices;
    public Interpolation interpolation;
    public CoverageInfo coverageInfo;
    public Filter filter;
    public CoordinateReferenceSystem targetVerticalCRS;

    public RasterDownloadContext(CoverageInfo coverageInfo, Integer[] targetSize, CRSRequestHandler crsRequestHandler) {
        this.coverageInfo = coverageInfo;
        this.targetSize = targetSize;
        this.coverageParameters = coverageInfo.getParameters();
        this.crsRequestHandler = crsRequestHandler;
    }

    public boolean isImposedTargetSize() {
        return targetSize[0] != null || targetSize[1] != null;
    }

    /**
     * Computes the grid geometry for raster download based on target size and reader configuration.
     *
     * <p>If no target size is specified, automatically computes grid geometry using the CRS request handler. If partial
     * target size is provided, calculates the missing dimension using scaling. Creates a new GridGeometry2D with the
     * specified or computed target size and target envelope.
     *
     * @param reader The GridCoverage2DReader used for source raster data
     * @param crsRequestHandler Handler for coordinate reference system transformations
     * @throws FactoryException If coordinate reference system factory operations fail
     * @throws TransformException If coordinate transformation fails
     * @throws IOException If reading grid geometry encounters I/O issues
     */
    public void computeGridGeometry(GridCoverage2DReader reader, CRSRequestHandler crsRequestHandler)
            throws FactoryException, TransformException, IOException {
        if (targetSize[0] == null && targetSize[1] == null) {
            LOGGER.fine("No Target size has been specified. Automatically computing GridGeometry.");
            GridGeometryProvider provider = new GridGeometryProvider(crsRequestHandler);
            GridGeometry2D gridGeometry = provider.getGridGeometry();
            LOGGER.fine("Computed requested GridGeometry: " + gridGeometry.toString());
            requestedGridGeometry = gridGeometry;
            return;
        }

        // Compute missing dimension if needed
        if (targetSize[0] == null || targetSize[1] == null) {
            ScaleToTarget scaling = new ScaleToTarget(reader);
            scaling.setTargetSize(targetSize[0], targetSize[1]);
            Integer[] computedSizes = scaling.getTargetSize();
            targetSize[0] = computedSizes[0];
            targetSize[1] = computedSizes[1];
        }

        GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, targetSize[0], targetSize[1]);
        GridGeometry2D gridGeometry = new GridGeometry2D(gridRange, crsRequestHandler.getTargetEnvelope());
        LOGGER.fine("Target size specified. Computed GridGeometry: " + gridGeometry);
        requestedGridGeometry = gridGeometry;
    }

    /** Extract the backgroundValues which will be used for mosaicking operations. */
    public void computeBackgroundValues(GeneralParameterValue[] readParameters) {
        if (coverageParameters != null && coverageParameters.containsKey("BackgroundValues")) {
            for (GeneralParameterValue readParameter : readParameters) {
                if ("BackgroundValues"
                        .equalsIgnoreCase(
                                readParameter.getDescriptor().getName().toString())) {
                    Object bgValue = ((ParameterValue) readParameter).getValue();
                    if (bgValue != null && bgValue instanceof double[]) {
                        backgroundValues = ((double[]) bgValue);
                    }
                    break;
                }
            }
        }
    }
}
