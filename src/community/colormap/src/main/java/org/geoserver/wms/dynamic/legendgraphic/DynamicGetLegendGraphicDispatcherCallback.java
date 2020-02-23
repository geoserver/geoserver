/* (c) 2013 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dynamic.legendgraphic;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.kvp.TimeKvpParser;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.RasterCleaner;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.util.NullProgressListener;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.function.ProcessFunction;
import org.geotools.process.raster.DynamicColorMapProcess;
import org.geotools.process.raster.FilterFunction_svgColorMap;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.styling.ColorMap;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.util.factory.GeoTools;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;

/**
 * A {@link DispatcherCallback} which intercepts a getLegendGraphicRequest and check whether that
 * request involve a dynamicColorRamp rendering transformation. In that case, it setup a legend
 * based on the dynamic values coming from the request. The callback works under the assumption that
 * there is only one style and one layer involved, it won't work for a multilayer/multistyle request
 * (that could be arranged, but we'd need to open an extension point in the legend graphics builder
 * to treat rendering transformations instead).
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class DynamicGetLegendGraphicDispatcherCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    DynamicGetLegendGraphicDispatcherCallback.class.getPackage().getName());

    private Catalog catalog;

    TimeKvpParser parser = new TimeKvpParser("time");

    public DynamicGetLegendGraphicDispatcherCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        final String id = operation.getId();

        // only intercepting getLegendGraphic invokation
        if (id.equalsIgnoreCase("getLegendGraphic")) {
            final Object[] params = operation.getParameters();
            if (params != null
                    && params.length > 0
                    && params[0] instanceof GetLegendGraphicRequest) {

                final GetLegendGraphicRequest getLegendRequest =
                        (GetLegendGraphicRequest) params[0];

                try {
                    for (LegendRequest legend : getLegendRequest.getLegends()) {
                        ProcessFunction transformation = getDynamicColorMapTransformation(legend);
                        if (transformation != null) {
                            LayerInfo layer = legend.getLayerInfo();
                            if (layer != null && layer.getResource() instanceof CoverageInfo) {
                                CoverageInfo coverageInfo = (CoverageInfo) layer.getResource();
                                List<CoverageDimensionInfo> dimensions =
                                        coverageInfo.getDimensions();
                                String unit = "";
                                if (dimensions != null && !dimensions.isEmpty()) {
                                    CoverageDimensionInfo dimensionInfo = dimensions.get(0);
                                    unit = dimensionInfo.getUnit();
                                    if (unit == null) {
                                        unit = "";
                                    }
                                }

                                Style style = getDynamicStyle(coverageInfo, transformation);
                                if (style != null) {
                                    legend.setStyle(style);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new ServiceException("Failed to extract legend", e);
                }
            }
        }
        return operation;
    }

    /**
     * Look for a ColorRamp string definition used by a {@link FilterFunction_svgColorMap} if any.
     */
    private ProcessFunction getDynamicColorMapTransformation(LegendRequest legendRequest) {
        if (legendRequest.getStyle() != null) {
            final Style style = legendRequest.getStyle();
            final FeatureTypeStyle[] featureTypeStyles =
                    style.featureTypeStyles().toArray(new FeatureTypeStyle[0]);
            for (FeatureTypeStyle featureTypeStyle : featureTypeStyles) {

                // Getting the main transformation
                Expression transformation = featureTypeStyle.getTransformation();
                if (transformation instanceof ProcessFunction) {
                    final ProcessFunction processFunction = (ProcessFunction) transformation;
                    final String processName = processFunction.getName();

                    // Checking whether the processFunction is a DynamicColorMapProcess
                    if (processName.equals(DynamicColorMapProcess.NAME)
                            || processName.equals("ras:" + DynamicColorMapProcess.NAME)) {
                        return processFunction;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Look for a ColorRamp definition used by a {@link DynamicColorMapProcess} rendering
     * transformation.
     */
    private Style getDynamicStyle(CoverageInfo coverageInfo, ProcessFunction transformation)
            throws IOException, ParseException {

        GridCoverage2D coverage = null;
        try {

            // Getting coverage to parse statistics
            GridCoverage2DReader reader =
                    (GridCoverage2DReader)
                            coverageInfo.getGridCoverageReader(
                                    new NullProgressListener(), GeoTools.getDefaultHints());

            GeneralParameterValue[] parameters = parseReadParameters(coverageInfo, reader);
            coverage = (GridCoverage2D) reader.read(parameters);

            ColorMap cm = null;
            double opacity = 1.0;
            for (Expression param : transformation.getParameters()) {
                // these functions evaluate to a singleton map
                Map map = param.evaluate(coverage, Map.class);
                Object paramValue = map.values().iterator().next();
                Object key = map.keySet().iterator().next();
                if (paramValue instanceof ColorMap) {
                    cm = (ColorMap) paramValue;
                } else {
                    if ("opacity".equals(key) && paramValue != null) {
                        opacity = ((Number) paramValue).doubleValue();
                    }
                }
            }
            if (cm != null) {
                StyleBuilder sb = new StyleBuilder();
                RasterSymbolizer rs = sb.createRasterSymbolizer(cm, opacity);
                return sb.createStyle(rs);
            }
        } finally {
            if (coverage != null) {
                RasterCleaner.addCoverage(coverage);
            }
        }

        return null;
    }

    /**
     * Parse the read parameter from the getLegendGraphicRequest in order to access the proper
     * coverage slice to retrieve the proper statistics.
     *
     * @param coverageInfo the coverage to be accessed
     * @param map the request parameters
     * @param reader the reader to be used to access the coverage
     * @return parameters setup on top of requested values.
     */
    private GeneralParameterValue[] parseReadParameters(
            final CoverageInfo coverageInfo, final GridCoverage2DReader reader)
            throws IOException, ParseException {

        // Parameters
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        GeneralParameterValue[] readParameters =
                CoverageUtils.getParameters(
                        readParametersDescriptor, coverageInfo.getParameters(), false);
        final List<GeneralParameterDescriptor> parameterDescriptors =
                new ArrayList<GeneralParameterDescriptor>(
                        readParametersDescriptor.getDescriptor().descriptors());

        // add the descriptors for custom dimensions
        Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();
        parameterDescriptors.addAll(dynamicParameters);

        final ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        final MetadataMap metadata = coverageInfo.getMetadata();

        // Setup small envelope to get a piece of coverage to retrieve stats
        final ReferencedEnvelope testEnvelope = createTestEnvelope(coverageInfo);
        final GridGeometry2D gridGeometry =
                new GridGeometry2D(new GridEnvelope2D(new Rectangle(0, 0, 2, 2)), testEnvelope);
        readParameters =
                CoverageUtils.mergeParameter(
                        parameterDescriptors,
                        readParameters,
                        gridGeometry,
                        AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString());

        // Parse Time
        Map<String, Object> map = Dispatcher.REQUEST.get().getKvp();
        readParameters = parseTimeParameter(metadata, readParameters, parameterDescriptors, map);

        // Parse Elevation
        readParameters =
                parseElevationParameter(metadata, readParameters, parameterDescriptors, map);

        // Parse custom domains
        readParameters =
                parseCustomDomains(dimensions, metadata, readParameters, parameterDescriptors, map);
        return readParameters;
    }

    /** Parse custom dimension values if present */
    private GeneralParameterValue[] parseCustomDomains(
            final ReaderDimensionsAccessor dimensions,
            final MetadataMap metadata,
            GeneralParameterValue[] readParameters,
            final List<GeneralParameterDescriptor> parameterDescriptors,
            final Map<String, Object> map)
            throws IOException {
        List<String> customDomains = new ArrayList(dimensions.getCustomDomains());
        if (customDomains != null && customDomains.size() > 0) {
            Set<String> params = map.keySet();
            for (String paramName : params) {
                if (paramName.regionMatches(true, 0, "dim_", 0, 4)) {
                    String name = paramName.substring(4);

                    // Getting the dimension
                    name = caseInsensitiveLookup(customDomains, name);
                    if (name != null) {
                        final DimensionInfo customInfo =
                                metadata.get(
                                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + name,
                                        DimensionInfo.class);
                        if (dimensions.hasDomain(name)
                                && customInfo != null
                                && customInfo.isEnabled()) {
                            final ArrayList<String> val = new ArrayList<String>(1);
                            String value = (String) map.get(paramName);
                            if (value.indexOf(",") > 0) {
                                String[] elements = value.split(",");
                                val.addAll(Arrays.asList(elements));
                            } else {
                                val.add(value);
                            }
                            readParameters =
                                    CoverageUtils.mergeParameter(
                                            parameterDescriptors, readParameters, val, name);
                        }
                    }
                }
            }
        }
        return readParameters;
    }

    /**
     * Parse the elevation parameter if present.
     *
     * @param metadata the elevationInfo metadata object
     * @param readParameters the readParameters to be set
     * @param parameterDescriptors the reader's parameter descriptors
     * @param map the request's parameters
     * @return the updated parameter set
     */
    private GeneralParameterValue[] parseElevationParameter(
            final MetadataMap metadata,
            GeneralParameterValue[] readParameters,
            final List<GeneralParameterDescriptor> parameterDescriptors,
            final Map<String, Object> map) {
        final DimensionInfo elevationInfo =
                metadata.get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevationInfo != null && elevationInfo.isEnabled()) {
            // handle "current"
            Set<String> params = map.keySet();
            for (String param : params) {
                if (param.equalsIgnoreCase("elevation")) {
                    readParameters =
                            CoverageUtils.mergeParameter(
                                    parameterDescriptors,
                                    readParameters,
                                    Double.valueOf((String) map.get(param)),
                                    "ELEVATION",
                                    "Elevation");
                    break;
                }
            }
        }
        return readParameters;
    }

    /**
     * Parse the time parameter if present.
     *
     * @param metadata the timeInfo metadata object
     * @param readParameters the readParameters to be set
     * @param parameterDescriptors the reader's parameter descriptors
     * @param map the request's parameters
     * @return the updated parameter set
     */
    private GeneralParameterValue[] parseTimeParameter(
            final MetadataMap metadata,
            GeneralParameterValue[] readParameters,
            final List<GeneralParameterDescriptor> parameterDescriptors,
            final Map<String, Object> map)
            throws ParseException {
        final DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
        if (timeInfo != null && timeInfo.isEnabled()) {
            final Set<String> params = map.keySet();
            for (String param : params) {
                if (param.equalsIgnoreCase("time")) {
                    // pass down the parameters
                    readParameters =
                            CoverageUtils.mergeParameter(
                                    parameterDescriptors,
                                    readParameters,
                                    parser.parse((String) map.get(param)),
                                    "TIME",
                                    "Time");
                    break;
                }
            }
        }
        return readParameters;
    }

    private String caseInsensitiveLookup(List<String> names, String name) {
        for (String s : names) {
            if (name.equalsIgnoreCase(s)) {
                return s;
            }
        }

        return null;
    }

    /**
     * Create a small 2x2 envelope to be used to read a small coverage in order to retrieve
     * statistics from it
     */
    private ReferencedEnvelope createTestEnvelope(final CoverageInfo coverageInfo) {
        final ReferencedEnvelope envelope = coverageInfo.getNativeBoundingBox();
        final GridGeometry geometry = coverageInfo.getGrid();
        final MathTransform transform = geometry.getGridToCRS();

        // Creating a 2x2 envelope to get a sample coverage to retrieve statistics from that small
        // piece
        final double scaleX = XAffineTransform.getScaleX0((AffineTransform) transform);
        final double scaleY = XAffineTransform.getScaleY0((AffineTransform) transform);
        final double minX = envelope.getMinimum(0);
        final double minY = envelope.getMinimum(1);
        final ReferencedEnvelope newEnvelope =
                new ReferencedEnvelope(
                        minX,
                        minX + scaleX * 2,
                        minY,
                        minY + scaleY * 2,
                        envelope.getCoordinateReferenceSystem());
        return newEnvelope;
    }
}
