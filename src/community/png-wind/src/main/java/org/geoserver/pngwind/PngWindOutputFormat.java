/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.crs.WrappingProjectionHandler;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageReaderHelper;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;

/** Handles a GetMap getRequest to produce a Png Wind output format */
public class PngWindOutputFormat extends RenderedImageMapOutputFormat {

    private static final GridCoverageFactory GC_FACTORY = new GridCoverageFactory();

    /**
     * Constructor
     *
     * @param wms the WMS
     */
    public PngWindOutputFormat(WMS wms) {
        super(PngWindConstants.MIME_TYPE, wms);
    }

    @Override
    public RenderedImageMap produceMap(WMSMapContent mapContent, boolean tiled) throws ServiceException {
        final GetMapRequest request = mapContent.getRequest();
        PngWindRequestContext ctx = PngWindRequestContext.from(request);
        CoverageInfo coverageInfo = ctx.getCoverageInfo();
        final int width = request.getWidth();
        final int height = request.getHeight();

        final CoordinateReferenceSystem targetCRS = request.getCrs();
        final ReferencedEnvelope targetEnvelope = new ReferencedEnvelope(request.getBbox(), targetCRS);

        // Read coverage
        GridCoverage2DReader reader;
        RenderedImage image;
        try {
            reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
            CoordinateReferenceSystem sourceCRS = reader.getCoordinateReferenceSystem();
            final boolean projectionHandlingEnabled = wms.isAdvancedProjectionHandlingEnabled();
            final boolean wrapEnabled = wms.isContinuousMapWrappingEnabled();
            Rectangle destinationSize = new Rectangle(0, 0, width, height);
            GridCoverageReaderHelper helper = new GridCoverageReaderHelper(
                    reader, destinationSize, ReferencedEnvelope.reference(targetEnvelope), null, null);

            ProjectionHandler handler = null;
            if (projectionHandlingEnabled) {
                handler = ProjectionHandlerFinder.getHandler(helper.getReadEnvelope(), sourceCRS, wrapEnabled);
                if (handler instanceof WrappingProjectionHandler projectionHandler) {
                    // raster data is monolithic and can cover the whole world, disable
                    // the geometry wrapping heuristic
                    projectionHandler.setDatelineWrappingCheckEnabled(false);
                }
            }

            final MapLayerInfo layerInfo = request.getLayers().get(0);
            List<Object> times = request.getTime();
            List<Object> elevations = request.getElevation();
            List<Filter> layerFilters = request.getFilter();
            List<SortBy[]> layerSorts = request.getSortByArrays();
            Filter layerFilter = (layerFilters != null && !layerFilters.isEmpty()) ? layerFilters.get(0) : null;

            SortBy[] layerSort = (layerSorts != null && !layerSorts.isEmpty()) ? layerSorts.get(0) : null;

            GeneralParameterValue[] readParams = wms.getWMSReadParameters(
                    request, layerInfo, layerFilter, layerSort, times, elevations, reader, false);

            // Read the coverage
            GridCoverage2D coverage =
                    helper.readCoverages(readParams, handler, GC_FACTORY).get(0);

            // Render (resample/reproject/scale) onto the getRequest grid
            GridCoverageRenderer renderer =
                    new GridCoverageRenderer(targetCRS, targetEnvelope, new Rectangle(width, height), null);
            renderer.setAdvancedProjectionHandlingEnabled(projectionHandlingEnabled);

            // Note that we are passing a null symbolizer.
            // Rendering without style retains the original data (that we need to quantize),
            // going through only the reprojecting/scaling/cropping parts.
            image = renderer.renderImage(coverage, null, null);
        } catch (Exception e) {
            String serviceError =
                    "Exception occurred while reading the coverage for layer '" + coverageInfo.getName() + "'";
            throw new ServiceException(serviceError, e);
        }

        if (image == null) {
            throw new ServiceException("No data to render for image/vnd.png-wind getRequest");
        }

        // Defensive: ensure the rendered image is still 2 bands
        if (image.getSampleModel().getNumBands() != 2) {
            throw unsupported("Rendered image is not 2-band (got "
                    + image.getSampleModel().getNumBands() + ")");
        }

        // Set the context and wrap the image as RenderedImageMap
        ctx.setBounds(targetEnvelope);
        mapContent.getMetadata().put(PngWindConstants.METADATA_CTX_KEY, ctx);
        return new RenderedImageMap(mapContent, image, PngWindConstants.MIME_TYPE);
    }

    private ServiceException unsupported(String message) {
        message = "Unsupported getRequest for " + PngWindConstants.MIME_TYPE + ": " + message;
        ServiceException se = new ServiceException(message);
        se.setCode("InvalidParameterValue");
        return se;
    }
}
