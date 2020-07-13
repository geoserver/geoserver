/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import it.geosolutions.jaiext.range.Range;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.media.jai.PlanarImage;
import org.apache.xml.utils.XMLChar;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.clip.CroppedGridCoverage2DReader;
import org.geoserver.wms.map.RasterSymbolizerVisitor;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.TransformedDirectPosition;
import org.geotools.geometry.util.XRectangle2D;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.ows.ServiceException;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RenderingTransformationHelper;
import org.geotools.styling.Style;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.sort.SortBy;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Layer identifier specifialized for raster layers
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RasterLayerIdentifier implements LayerIdentifier<GridCoverage2DReader> {

    static final Logger LOGGER = Logging.getLogger(RasterLayerIdentifier.class);

    private WMS wms;

    public RasterLayerIdentifier(final WMS wms) {
        this.wms = wms;
    }

    public boolean canHandle(MapLayerInfo layer) {
        int type = layer.getType();
        return type == MapLayerInfo.TYPE_RASTER;
    }

    public List<FeatureCollection> identify(
            FeatureInfoRequestParameters requestParams, int maxFeatures) throws Exception {
        final MapLayerInfo layer = requestParams.getLayer();
        final Filter filter = requestParams.getFilter();
        final SortBy[] sort = requestParams.getSort();
        final CoverageInfo cinfo = layer.getCoverage();
        final GridCoverage2DReader reader =
                handleClipParam(
                        requestParams,
                        (GridCoverage2DReader)
                                cinfo.getGridCoverageReader(
                                        new NullProgressListener(), GeoTools.getDefaultHints()));
        DirectPosition position = getQueryPosition(requestParams, cinfo, reader);

        // check that the provided point is inside the bbox for this coverage
        if (!reader.getOriginalEnvelope().contains(position)) {
            return null;
        }
        GeneralParameterValue[] parameters =
                setupReadParameters(requestParams, layer, filter, sort, cinfo, reader, position);
        if (parameters == null) return null;

        GridCoverage2D coverage = readCoverage(requestParams, reader, parameters);

        if (coverage == null) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("Unable to load raster data for this request.");
            return null;
        }

        FeatureCollection pixel = null;
        try {
            final double[] pixelValues = coverage.evaluate(position, (double[]) null);
            if (requestParams.isExcludeNodataResults() && pixelsAreNodata(coverage, pixelValues)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Returning no result due to nodata pixel");
                }
                for (int i = 0; i < pixelValues.length; i++) {
                    pixelValues[i] = Double.NaN;
                }
            }
            pixel = wrapPixelInFeatureCollection(coverage, pixelValues, cinfo.getQualifiedName());
        } catch (PointOutsideCoverageException e) {
            // it's fine, users might legitimately query point outside, we just don't
            // return anything
        } finally {
            RenderedImage ri = coverage.getRenderedImage();
            coverage.dispose(true);
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
        return Collections.singletonList(pixel);
    }

    private GeneralParameterValue[] setupReadParameters(
            FeatureInfoRequestParameters requestParams,
            MapLayerInfo layer,
            Filter filter,
            SortBy[] sort,
            CoverageInfo cinfo,
            GridCoverage2DReader reader,
            DirectPosition position)
            throws IOException, TransformException, ServiceException {
        // read from the request
        GetMapRequest getMap = requestParams.getGetMapRequest();
        GeneralParameterValue[] parameters =
                wms.getWMSReadParameters(
                        getMap,
                        layer,
                        filter,
                        sort,
                        requestParams.getTimes(),
                        requestParams.getElevations(),
                        reader,
                        true);

        // now get the position in raster space using the world to grid related to
        // corner
        final MathTransform worldToGrid =
                reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER).inverse();
        final DirectPosition rasterMid = worldToGrid.transform(position, null);
        // create a 20X20 rectangle aruond the mid point and then intersect with the
        // original range
        final Rectangle2D.Double rasterArea = new Rectangle2D.Double();
        rasterArea.setFrameFromCenter(
                rasterMid.getOrdinate(0),
                rasterMid.getOrdinate(1),
                rasterMid.getOrdinate(0) + 2,
                rasterMid.getOrdinate(1) + 2);
        final Rectangle integerRasterArea = rasterArea.getBounds();
        final GridEnvelope gridEnvelope = reader.getOriginalGridRange();
        final Rectangle originalArea =
                (gridEnvelope instanceof GridEnvelope2D)
                        ? (GridEnvelope2D) gridEnvelope
                        : new Rectangle();
        XRectangle2D.intersect(integerRasterArea, originalArea, integerRasterArea);
        // paranoiac check, did we fall outside the coverage raster area? This should
        // never really happne if the request is well formed.
        if (integerRasterArea.isEmpty()) {
            return null;
        }

        // now set the extra request parameters for this request
        String[] propertyNames = requestParams.getPropertyNames();
        for (int k = 0; k < parameters.length; k++) {
            if (!(parameters[k] instanceof Parameter<?>)) {
                continue;
            }

            final Parameter<?> parameter = (Parameter<?>) parameters[k];
            ReferenceIdentifier name = parameter.getDescriptor().getName();
            if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName())) {
                //
                // create a suitable geometry for this request reusing the getmap (we
                // could probably optimize)
                //
                parameter.setValue(
                        new GridGeometry2D(
                                new GridEnvelope2D(integerRasterArea),
                                reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER),
                                reader.getCoordinateReferenceSystem()));
            } else if (propertyNames != null
                    && propertyNames.length > 0
                    && name.equals(AbstractGridFormat.BANDS.getName())) {
                int[] bands = new int[propertyNames.length];
                Set<String> requestedNames = new HashSet<>(Arrays.asList(propertyNames));
                List<String> dimensionNames =
                        cinfo.getDimensions()
                                .stream()
                                .map(d -> d.getName())
                                .collect(Collectors.toList());
                for (int i = 0, j = 0;
                        i < dimensionNames.size() && !requestedNames.isEmpty();
                        i++) {
                    String dimensionName = dimensionNames.get(i);
                    if (requestedNames.remove(dimensionName)) {
                        bands[j++] = i;
                    }
                }
                if (!requestedNames.isEmpty()) {
                    String availableNames =
                            dimensionNames.stream().collect(Collectors.joining(", "));
                    throw new ServiceException(
                            "Could not find the following requested properties "
                                    + requestedNames
                                    + ", available property names are "
                                    + availableNames,
                            org.geoserver.platform.ServiceException.INVALID_PARAMETER_VALUE,
                            "PropertyName");
                }
                parameter.setValue(bands);
            }
        }
        return parameters;
    }

    private DirectPosition getQueryPosition(
            FeatureInfoRequestParameters params, CoverageInfo cinfo, GridCoverage2DReader reader) {
        CoordinateReferenceSystem requestedCRS = params.getRequestedCRS();

        CoordinateReferenceSystem targetCRS;
        if ((cinfo.getProjectionPolicy() == ProjectionPolicy.NONE)
                || (cinfo.getProjectionPolicy() == ProjectionPolicy.REPROJECT_TO_DECLARED)) {
            targetCRS = cinfo.getNativeCRS();
        } else {
            targetCRS = cinfo.getCRS();
        }

        // set the requested position in model space for this request
        final Coordinate middle =
                WMS.pixelToWorld(
                        params.getX(),
                        params.getY(),
                        params.getRequestedBounds(),
                        params.getWidth(),
                        params.getHeight());
        double x = middle.x;
        double y = middle.y;
        // coverage median position in the coverage (target) CRS
        DirectPosition targetCoverageMedianPosition = reader.getOriginalEnvelope().getMedian();

        // support continuous map wrapping by adding integer multiples of 360 degrees to longitude
        // to move the requested position closer to the centre of the coverage
        if (requestedCRS != null && requestedCRS instanceof GeographicCRS) {
            // for consistency, the transformation is exactly the same as below when preparing
            // the request, but in the inverse direction (coverage (target) CRS to requested CRS)
            // coverage median position transformed into the requested CRS
            TransformedDirectPosition coverageMedianPosition =
                    new TransformedDirectPosition(
                            targetCRS,
                            requestedCRS,
                            new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
            try {
                coverageMedianPosition.transform(targetCoverageMedianPosition);
            } catch (TransformException exception) {
                throw new CannotEvaluateException(
                        "Cannot find coverage median position in requested CRS", exception);
            }
            if (CRS.getAxisOrder(requestedCRS) == CRS.AxisOrder.NORTH_EAST) {
                // y and second ordinate are longitude
                y += 360 * Math.round((coverageMedianPosition.getOrdinate(1) - y) / 360);
            } else {
                // x and first ordinate are longitude
                x += 360 * Math.round((coverageMedianPosition.getOrdinate(0) - x) / 360);
            }
        }

        DirectPosition position = new DirectPosition2D(requestedCRS, x, y);

        // change from request crs to coverage crs in order to compute a minimal request
        // area,
        // TODO this code need to be made much more robust
        if (requestedCRS != null) {
            final TransformedDirectPosition arbitraryToInternal =
                    new TransformedDirectPosition(
                            requestedCRS,
                            targetCRS,
                            new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
            try {
                arbitraryToInternal.transform(position);
            } catch (TransformException exception) {
                throw new CannotEvaluateException(
                        "Unable to answer the geatfeatureinfo", exception);
            }
            position = arbitraryToInternal;
        }

        // now a *second* round of wrapping in the target CRS to support coverages that have
        // original envelopes with longitudes outside (-180, 180)
        if (targetCRS != null && targetCRS instanceof GeographicCRS) {
            x = position.getOrdinate(0);
            y = position.getOrdinate(1);
            if (CRS.getAxisOrder(targetCRS) == CRS.AxisOrder.NORTH_EAST) {
                // y and second ordinate are longitude
                y += 360 * Math.round((targetCoverageMedianPosition.getOrdinate(1) - y) / 360);
            } else {
                // x and first ordinate are longitude
                x += 360 * Math.round((targetCoverageMedianPosition.getOrdinate(0) - x) / 360);
            }
            position = new DirectPosition2D(targetCRS, x, y);
        }
        return position;
    }

    private GridCoverage2D readCoverage(
            FeatureInfoRequestParameters params,
            GridCoverage2DReader reader,
            GeneralParameterValue[] parameters)
            throws IOException, SchemaException, TransformException, FactoryException {
        GridCoverage2D coverage = reader.read(parameters);

        //
        Style style = params.getStyle();
        RasterSymbolizerVisitor visitor =
                new RasterSymbolizerVisitor(params.getScaleDenominator(), null);
        style.accept(visitor);
        Expression transformation = visitor.getRasterRenderingTransformation();
        if (transformation != null) {
            RenderingTransformationHelper helper =
                    new RenderingTransformationHelper() {
                        @Override
                        protected GridCoverage2D readCoverage(
                                GridCoverage2DReader reader, Object params, GridGeometry2D readGG)
                                throws IOException {
                            throw new UnsupportedOperationException(
                                    "This helper is meant to be used with a coverage already read");
                        }
                    };
            Object result =
                    helper.applyRenderingTransformation(
                            transformation,
                            DataUtilities.source(FeatureUtilities.wrapGridCoverage(coverage)),
                            Query.ALL,
                            Query.ALL,
                            null,
                            coverage.getCoordinateReferenceSystem2D(),
                            null);
            if (result instanceof GridCoverage2D) {
                coverage = (GridCoverage2D) result;
            } else {
                coverage = null;
            }
        }
        return coverage;
    }

    private boolean pixelsAreNodata(GridCoverage2D coverage, final double values[]) {
        RenderedImage ri = coverage.getRenderedImage();
        ImageWorker worker = new ImageWorker(ri);
        Range nodata = worker.getNoData();
        int nodataValues = 0;
        if (nodata != null) {
            for (double value : values) {
                if (nodata.contains(value)) {
                    nodataValues++;
                }
            }
        }
        return nodataValues == values.length;
    }

    private SimpleFeatureCollection wrapPixelInFeatureCollection(
            GridCoverage2D coverage, double[] pixelValues, Name coverageName)
            throws SchemaException {

        GridSampleDimension[] sampleDimensions = coverage.getSampleDimensions();

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(coverageName);
        final Set<String> bandNames = new HashSet<String>();
        for (int i = 0; i < sampleDimensions.length; i++) {
            String name = descriptionToNcName(sampleDimensions[i].getDescription().toString());
            // GEOS-2518
            if (bandNames.contains(name)) {
                // it might happen again that the name already exists but it pretty difficult I'd
                // say
                name = new StringBuilder(name).append("_Band").append(i).toString();
            }
            bandNames.add(name);
            builder.add(name, Double.class);
        }
        SimpleFeatureType gridType = builder.buildFeatureType();

        Double[] values = new Double[pixelValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Double.valueOf(pixelValues[i]);
        }
        return DataUtilities.collection(SimpleFeatureBuilder.build(gridType, values, ""));
    }

    /**
     * Convert sample dimension description to a valid XML NCName by replacing invalid characters
     * with underscores (<code>'_'</code>).
     *
     * <p>If the description is null or has zero length, the NCName "Unknown" is returned.
     *
     * @param description sample dimension description
     * @return valid XML NCName
     */
    static String descriptionToNcName(String description) {
        if (description == null || description.length() == 0) {
            return "Unknown";
        } else {
            char[] result = description.toCharArray();
            for (int i = 0; i < result.length; i++) {
                if ((i == 0 && !XMLChar.isNCNameStart(result[i]))
                        || (i > 0 && !XMLChar.isNCName(result[i]))) {
                    result[i] = '_';
                }
            }
            return new String(result);
        }
    }

    @Override
    public GridCoverage2DReader handleClipParam(
            FeatureInfoRequestParameters params, GridCoverage2DReader reader) {
        Geometry roiGeom = params.getGetMapRequest().getClip();
        if (roiGeom == null) return reader;
        return new CroppedGridCoverage2DReader(reader, roiGeom);
    }
}
