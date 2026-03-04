/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.stream.Collectors;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.parameter.GeneralParameterDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.crs.WrappingProjectionHandler;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageReaderHelper;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.styling.RasterSymbolizerImpl;

/** Handles a GetMap getRequest to produce a Png Wind output format */
public class PngWindOutputFormat extends RenderedImageMapOutputFormat {

    private static final GridCoverageFactory GC_FACTORY = new GridCoverageFactory();

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

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
        // Fail fast before doing heavier work
        final GetMapRequest request = mapContent.getRequest();
        PngWindRequestContext ctx = PngWindRequestContext.from(request);
        CoverageInfo coverageInfo = ctx.getCoverageInfo();

        // 2) Target rendering grid from getRequest
        final int width = request.getWidth();
        final int height = request.getHeight();

        final CoordinateReferenceSystem targetCRS = request.getCrs(); // or req.getSRS() -> decode
        final ReferencedEnvelope targetEnvelope =
                new ReferencedEnvelope(request.getBbox(), targetCRS);

        // 3) Read coverage (best: getRequest-aligned GridGeometry2D)
        GridCoverage2DReader reader;
        RenderedImage image;
        try {
            reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
            CoordinateReferenceSystem sourceCRS = reader.getCoordinateReferenceSystem();
            final boolean projectionHandlingEnabled  = wms.isAdvancedProjectionHandlingEnabled();
            final boolean wrapEnabled = wms.isContinuousMapWrappingEnabled();
            Rectangle destinationSize = new Rectangle(0, 0, width, height);
            GridCoverageReaderHelper helper = new GridCoverageReaderHelper(
                    reader, destinationSize, ReferencedEnvelope.reference(targetEnvelope), null,null);

            ProjectionHandler handler = null;
            if (projectionHandlingEnabled) {
                handler = ProjectionHandlerFinder.getHandler(helper.getReadEnvelope(), sourceCRS, wrapEnabled);
                if (handler instanceof WrappingProjectionHandler projectionHandler) {
                    // raster data is monolithic and can cover the whole world, disable
                    // the geometry wrapping heuristic
                    projectionHandler.setDatelineWrappingCheckEnabled(false);
                }
            }
            GeneralParameterValue[] readParams = buildReadParams(reader, coverageInfo, request);


            GridCoverage2D coverage = helper.readCoverages(readParams,handler, GC_FACTORY).get(0);
/*            // Build a GridGeometry2D describing the requested output grid
            // (world envelope + raster getDimensions). Exact constructors vary by GT version.
            GridGeometry2D targetGridGeometry = buildTargetGridGeometry(targetEnvelope, width, height);

            // Build reader params (store-specific), at minimum a GridGeometry2D param:
            GeneralParameterValue[] readParams = buildReadParams(reader, targetGridGeometry);

            GridCoverage2D coverage = reader.read(getCoverageInfo.getNativeCoverageName(), readParams);

            // Defensive: ensure still 2 bands after read
            int bands = coverage.getNumSampleDimensions();
            if (bands != 2) {
                throw unsupported("image/vnd.png-wind requires a 2-band raster. Found " + bands + ".");
            }*/


        // 4) Render (resample/reproject/scale) onto the getRequest grid
        GridCoverageRenderer renderer = new GridCoverageRenderer(targetCRS, targetEnvelope, new Rectangle(width, height), null);

        // If you manage to keep the renderer “reader-based”, you can enable advanced projection handling:
        renderer.setAdvancedProjectionHandlingEnabled(true);


        image = renderer.renderImage(coverage, null, null);
        } catch (Exception e) {
            String serviceError = "Exception occurred while reading the coverage for layer '" + coverageInfo.getName() + "'";
            throw new ServiceException(serviceError, e);
        }


        if (image == null) {
            // WMS convention: empty image / no data; decide if you return blank or exception
            throw new ServiceException("No data to render for image/vnd.png-wind getRequest");
        }

        // Defensive: ensure the rendered image is still 2 bands
        if (image.getSampleModel().getNumBands() != 2) {
            throw unsupported("Rendered image is not 2-band (got " + image.getSampleModel().getNumBands()
                    + "). Ensure the raster symbolizer does not force RGB(A) output.");
        }

        // 5) Set the context and wrap the image as RenderedImageMap
        ctx.setBounds(targetEnvelope);
        mapContent.getMetadata().put(PngWindConstants.METADATA_CTX_KEY, ctx);
        return new RenderedImageMap(mapContent, image, PngWindConstants.MIME_TYPE);
    }

    /*
     * Build reader params:
     * - READ_GRIDGEOMETRY2D (always)
     * - TIME / ELEVATION (if present + supported)
     * - FILTER / SORT_BY (if present + supported)  <-- important for ImageMosaic, etc.
     * - featureIds (converted to a Filter Id / IN filter when supported)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected GeneralParameterValue[] buildReadParams(
            GridCoverage2DReader reader,
            CoverageInfo coverageInfo,
            GetMapRequest req) {

        // Start with coverage stored params (as WPS download does)
        GeneralParameterValue[] readParams =
                CoverageUtils.getParameters(reader.getFormat().getReadParameters(),
                        coverageInfo.getParameters(), false);
        final List<GeneralParameterDescriptor> descriptors =  reader.getFormat()
                        .getReadParameters()
                        .getDescriptor()
                        .descriptors();
        final var supportedCodes = descriptors.stream()
                .map(p -> p.getName().getCode())
                .collect(Collectors.toSet());

        // Since PNG WIND format only supports SINGLE LAYER, we always use index 0 when
        // dealing with params for multiple layers.

        List<Object> time = req.getTime();
        // 1) TIME / ELEVATION (getDimensions)
        if (time != null && !time.isEmpty()
                && supportedCodes.contains(AbstractGridFormat.TIME.getName().getCode())) {
            Object timeValue = req.getTime().get(0);
            readParams = CoverageUtils.mergeParameter(descriptors, readParams, timeValue,
                    AbstractGridFormat.TIME.getName().getCode());
        }

        List<Object> elevation = req.getElevation();
        if (elevation!= null && !elevation.isEmpty()
                && supportedCodes.contains(AbstractGridFormat.ELEVATION.getName().getCode())) {
            Object elevValue = elevation.get(0);
            readParams = CoverageUtils.mergeParameter((List) descriptors, readParams, elevValue,
                    AbstractGridFormat.ELEVATION.getName().getCode());
        }

        // 2) FILTER support (ImageMosaic and some others)
        // - "filters" (already Filter objects, typically per-layer)
        // - "cqlFilters" (strings per-layer)

        Filter combined = Filter.INCLUDE;

        List<Filter> filters = req.getFilter();
        // a) Native Filter list
        if (filters != null && !filters.isEmpty()) {
            Filter f0 = filters.get(0);
            combined = and(combined, f0);
        }

        // b) CQL filter list -> Filter
        List<Filter> cqlfilters = req.getCQLFilter();
        if (cqlfilters != null && !cqlfilters.isEmpty()) {
            Filter f0 = cqlfilters.get(0);
            combined = and(combined, f0);
        }

        boolean hasNonTrivialFilter = combined != Filter.INCLUDE;

        if (hasNonTrivialFilter) {
            if (supportedCodes.contains("FILTER")) {
                readParams = CoverageUtils.mergeParameter( descriptors, readParams, combined, "FILTER");
            } else {
                // strict behavior recommended (avoid silently ignoring)
                throw unsupported("Request includes filters but the coverage reader does not support FILTER parameter.");
            }
        }

        if (supportedCodes.contains(AbstractGridFormat.USE_IMAGEN_IMAGEREAD.getName().getCode())) {
            readParams = CoverageUtils.mergeParameter(
                    (List) descriptors,
                    readParams,
                    Boolean.TRUE,
                    AbstractGridFormat.USE_IMAGEN_IMAGEREAD.getName().getCode()
            );
        }

        return readParams;
    }

    private Filter and(Filter a, Filter b) {
        if (a == null || a == Filter.INCLUDE) return b;
        if (b == null || b == Filter.INCLUDE) return a;
        // Use GeoTools FilterFactory2 in real code; leaving schematic here:
        return FF.and(a, b);
    }

    private ServiceException unsupported(String message) {
        message = "Unsupported getRequest for " + PngWindConstants.MIME_TYPE  + ": " + message;
        ServiceException se = new ServiceException(message);
        se.setCode("InvalidParameterValue");
        return se;
    }
}
