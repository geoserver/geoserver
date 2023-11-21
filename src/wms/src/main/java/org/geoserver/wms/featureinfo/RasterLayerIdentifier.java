/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.geoserver.wms.featureinfo.ColorMapLabelMatcher.DEFAULT_ATTRIBUTE_NAME;
import static org.geoserver.wms.featureinfo.ColorMapLabelMatcher.getLabelAttributeNameCount;
import static org.geoserver.wms.featureinfo.ColorMapLabelMatcher.isLabelReplacingValue;

import it.geosolutions.jaiext.range.Range;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
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
import org.geotools.api.coverage.CannotEvaluateException;
import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.geometry.Position;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Style;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.CoverageReadingTransformation;
import org.geotools.coverage.grid.io.CoverageReadingTransformation.ReaderAndParams;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.function.RenderingTransformation;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.TransformedPosition;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.util.XRectangle2D;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.ows.ServiceException;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RenderingTransformationHelper;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

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

    @Override
    public boolean canHandle(MapLayerInfo layer) {
        int type = layer.getType();
        return type == MapLayerInfo.TYPE_RASTER;
    }

    @Override
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
        Position position = getQueryPosition(requestParams, cinfo, reader);

        // check that the provided point is inside the bbox for this coverage
        if (!reader.getOriginalEnvelope().contains(position)) {
            return null;
        }
        GeneralParameterValue[] parameters =
                setupReadParameters(requestParams, layer, filter, sort, cinfo, reader, position);
        if (parameters == null) return null;

        /* identify straight caster rendering once, and each raster to vector tx */
        List<FeatureCollection> result = new ArrayList<>();
        boolean plainRenderingIdentified = false;
        for (FeatureTypeStyle fts : requestParams.getStyle().featureTypeStyles()) {
            Expression tx = isTransformFeatureInfo(fts) ? fts.getTransformation() : null;
            if (tx == null && plainRenderingIdentified) {
                continue;
            }
            plainRenderingIdentified |= tx == null;
            Object info = read(requestParams, reader, parameters, fts);
            if (info instanceof GridCoverage2D) {
                GridCoverage2D coverage = (GridCoverage2D) info;
                result.addAll(toFeatures(cinfo, coverage, position, requestParams));
            } else if (info instanceof FeatureCollection) {
                // the read used the whole WMS GetMap area (an RT may need more space than just the
                // queried pixel to produce correct results), need to filter down the results
                FeatureCollection fc = (FeatureCollection) info;
                result.add((filterCollection(fc, requestParams)));
            } else {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.fine("Unable to load raster data for this request.");
            }
        }

        return result;
    }

    private boolean isTransformFeatureInfo(FeatureTypeStyle fts) {
        // check the vendor option first, then the WMS settings
        String option = fts.getOptions().get(RasterSymbolizerVisitor.TRANSFORM_FEATURE_INFO);
        return option != null ? Boolean.parseBoolean(option) : this.wms.isTransformFeatureInfo();
    }

    @SuppressWarnings("unchecked")
    private FeatureCollection filterCollection(
            FeatureCollection fc, FeatureInfoRequestParameters params) {
        // is it part of the request params?
        int requestBuffer =
                params.getBuffer() <= 0
                        ? VectorBasicLayerIdentifier.MIN_BUFFER_SIZE
                        : params.getBuffer();
        ReferencedEnvelope bbox = LayerIdentifier.getEnvelopeFilter(params, requestBuffer);

        FilterFactory ff = params.getFilterFactory();
        BBOX filter = ff.bbox(ff.property(""), bbox);

        return new FilteringFeatureCollection(fc, filter);
    }

    private List<FeatureCollection> toFeatures(
            CoverageInfo cinfo,
            GridCoverage2D coverage,
            Position position,
            FeatureInfoRequestParameters requestParams) {
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

            ColorMapLabelMatcherExtractor labelVisitor =
                    new ColorMapLabelMatcherExtractor(requestParams.getScaleDenominator());
            requestParams.getStyle().accept(labelVisitor);
            List<ColorMapLabelMatcher> colorMapLabelMatcherList =
                    labelVisitor.getColorMapLabelMatcherList();

            pixel =
                    wrapPixelInFeatureCollection(
                            coverage,
                            pixelValues,
                            cinfo.getQualifiedName(),
                            colorMapLabelMatcherList);
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
            Position position)
            throws IOException, TransformException, ServiceException {
        // read from the request
        GetMapRequest getMap = requestParams.getGetMapRequest();
        List<Object> times = requestParams.getTimes();
        List<Object> elevations = requestParams.getElevations();
        wms.validateRasterDimensions(times, elevations, layer, requestParams.getGetMapRequest());
        GeneralParameterValue[] parameters =
                wms.getWMSReadParameters(
                        getMap, layer, filter, sort, times, elevations, reader, true);

        // now get the position in raster space using the world to grid related to
        // corner
        final MathTransform worldToGrid =
                reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER).inverse();
        final Position rasterMid = worldToGrid.transform(position, null);
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
        for (GeneralParameterValue generalParameterValue : parameters) {
            if (!(generalParameterValue instanceof Parameter<?>)) {
                continue;
            }

            final Parameter<?> parameter = (Parameter<?>) generalParameterValue;
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
                        cinfo.getDimensions().stream()
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
                if (!requestedNames.isEmpty() && !hasVectorTransformations(requestParams)) {
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

    private boolean hasVectorTransformations(FeatureInfoRequestParameters params) {
        Style style = params.getStyle();
        RasterSymbolizerVisitor visitor =
                new RasterSymbolizerVisitor(
                        params.getScaleDenominator(), null, this.wms.isTransformFeatureInfo());
        style.accept(visitor);

        // we could skip the reading altogether
        CoverageReadingTransformation readingTx = visitor.getCoverageReadingTransformation();
        return readingTx != null || getTransformation(visitor) != null;
    }

    private Position getQueryPosition(
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
        Position targetCoverageMedianPosition = reader.getOriginalEnvelope().getMedian();

        // support continuous map wrapping by adding integer multiples of 360 degrees to longitude
        // to move the requested position closer to the centre of the coverage
        if (requestedCRS != null && requestedCRS instanceof GeographicCRS) {
            // for consistency, the transformation is exactly the same as below when preparing
            // the request, but in the inverse direction (coverage (target) CRS to requested CRS)
            // coverage median position transformed into the requested CRS
            TransformedPosition coverageMedianPosition =
                    new TransformedPosition(
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

        Position position = new Position2D(requestedCRS, x, y);

        // change from request crs to coverage crs in order to compute a minimal request
        // area,
        // TODO this code need to be made much more robust
        if (requestedCRS != null) {
            final TransformedPosition arbitraryToInternal =
                    new TransformedPosition(
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
            position = new Position2D(targetCRS, x, y);
        }
        return position;
    }

    private Object read(
            FeatureInfoRequestParameters params,
            GridCoverage2DReader reader,
            GeneralParameterValue[] parameters,
            FeatureTypeStyle fts)
            throws IOException, SchemaException, TransformException, FactoryException {
        RasterSymbolizerVisitor visitor =
                new RasterSymbolizerVisitor(
                        params.getScaleDenominator(), null, this.wms.isTransformFeatureInfo());
        fts.accept(visitor);

        // we could skip the reading altogether
        CoverageReadingTransformation readingTx = visitor.getCoverageReadingTransformation();
        if (readingTx != null) {
            ReaderAndParams ctx = new ReaderAndParams(reader, parameters);
            return readingTx.evaluate(ctx);
        }

        // otherwise read, evaluate if a transformation is needed
        Expression transformation = getTransformation(visitor);
        if (transformation != null && transformation instanceof RenderingTransformation) {
            ((RenderingTransformation) transformation).customizeReadParams(reader, parameters);
        }
        GridCoverage2D coverage = reader.read(parameters);

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
            return helper.applyRenderingTransformation(
                    transformation,
                    DataUtilities.source(FeatureUtilities.wrapGridCoverage(coverage)),
                    Query.ALL,
                    Query.ALL,
                    null,
                    coverage.getCoordinateReferenceSystem2D(),
                    null);
        } else if (!visitor.getRasterSymbolizers().isEmpty() || !isTransformFeatureInfo(fts)) {
            // return the coverage if there are active raster symbolizers or if transforming
            // the feature info data is disabled
            return coverage;
        }

        // no transformation and no active raster symbolizers either
        return null;
    }

    private Expression getTransformation(RasterSymbolizerVisitor visitor) {
        Expression result = visitor.getRasterRenderingTransformation();
        if (result == null && visitor.getOtherRenderingTransformations().size() == 1) {
            result = visitor.getOtherRenderingTransformations().get(0);
        }
        return result;
    }

    private boolean pixelsAreNodata(GridCoverage2D coverage, final double[] values) {
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
            GridCoverage2D coverage,
            double[] pixelValues,
            Name coverageName,
            List<ColorMapLabelMatcher> colorMapLabelMatcherList) {
        GridSampleDimension[] sampleDimensions = coverage.getSampleDimensions();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(coverageName);

        boolean isLabelActive = !colorMapLabelMatcherList.isEmpty();
        boolean isLabelReplacingValue = isLabelReplacingValue(colorMapLabelMatcherList);

        if (!isLabelReplacingValue) addBandNamesToFeatureType(sampleDimensions, builder);

        if (isLabelActive)
            addLabelAttributeNameToFeatureType(colorMapLabelMatcherList, builder, coverage);

        SimpleFeatureType gridType = builder.buildFeatureType();

        int valuesLength;
        int labelSize = colorMapLabelMatcherList.size();
        if (isLabelReplacingValue) valuesLength = labelSize;
        else if (isLabelActive) valuesLength = pixelValues.length + labelSize;
        else valuesLength = pixelValues.length;

        Object[] values = new Object[valuesLength];

        List<String> labels = new ArrayList<>();
        int lastOccupiedPosition = 0;
        for (int i = 0; i < pixelValues.length; i++) {
            double pixelVal = Double.valueOf(pixelValues[i]);
            if (isLabelActive)
                addValueToLabelListByPixel(labels, colorMapLabelMatcherList, pixelVal, i);
            if (!isLabelReplacingValue) {
                values[i] = pixelVal;
                lastOccupiedPosition++;
            }
        }
        if (isLabelActive) {
            for (String label : labels) {
                values[lastOccupiedPosition] = label;
                lastOccupiedPosition++;
            }
        }
        return DataUtilities.collection(SimpleFeatureBuilder.build(gridType, values, ""));
    }

    private void addBandNamesToFeatureType(
            GridSampleDimension[] sampleDimensions, SimpleFeatureTypeBuilder builder) {
        final Set<String> bandNames = new HashSet<>();
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
    }

    private void addLabelAttributeNameToFeatureType(
            List<ColorMapLabelMatcher> colorMapLabelMatcherList,
            SimpleFeatureTypeBuilder builder,
            GridCoverage2D coverage) {
        int numLabel = getLabelAttributeNameCount(colorMapLabelMatcherList);
        int indexLabel = 1;
        for (ColorMapLabelMatcher lifi : colorMapLabelMatcherList) {
            String label = lifi.getAttributeName();
            Integer channelName = lifi.getChannel();
            if (label.equals(DEFAULT_ATTRIBUTE_NAME)) {
                // add the index to the attribute name in case we have more  then one
                // LabelInFeatureInfo with the default attribute name
                String labelIndexed = numLabel > 1 ? label + indexLabel : label;
                indexLabel++;
                GridSampleDimension sampleDimension =
                        channelName != null
                                ? coverage.getSampleDimension(channelName.intValue() - 1)
                                : coverage.getSampleDimensions()[0];
                String sampleDimDesc = sampleDimension.getDescription().toString();
                label = labelIndexed + "_" + descriptionToNcName(sampleDimDesc);
            }
            builder.add(label, String.class);
        }
    }

    private void addValueToLabelListByPixel(
            List<String> labels,
            List<ColorMapLabelMatcher> colorMapLabelMatcherList,
            double pixel,
            int currentPixelIdx) {
        for (ColorMapLabelMatcher lifi : colorMapLabelMatcherList) {
            Integer channelName = lifi.getChannel();
            if (channelName == null || currentPixelIdx == channelName.intValue() - 1)
                labels.add(lifi.getLabelForPixel(pixel));
        }
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
