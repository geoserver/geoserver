package org.geoserver.wms.dynamic.legendgraphic;

import java.awt.Dimension;
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
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.ows.kvp.TimeKvpParser;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphic;
import org.geoserver.wms.legendgraphic.ColorMapLegendCreator;
import org.geoserver.wms.legendgraphic.ColorMapLegendCreator.Builder;
import org.geoserver.wms.legendgraphic.LegendUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.FilterFunction_gridCoverageStats;
import org.geotools.process.raster.FilterFunction_svgColorMap;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;

/**
 * A class which parses a getLegendGraphicRequest involving a style which uses a DynamicColorMap rendering transformation.
 * It takes the request parameters (being a dynamic ColorMap, the statistics are different for each granule so we need to parse
 * the parameters such as time, elevation, custom dimension) to read a coverage. Then it invokes filterFunction to retrieve statistics
 * and setup a colorMap. Finally, it builds a {@link BufferedImageLegendGraphic} on top of the newly created color map.  
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class DynamicColorMapBuilder {

    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(DynamicColorMapBuilder.class.getPackage().getName());

    private static final int DEFAULT_DIGITS = 3;

    TimeKvpParser parser = new TimeKvpParser("time");

    public DynamicColorMapBuilder(Catalog geoserverCatalog) {
        this.geoserverCatalog = geoserverCatalog;
    }

    private Catalog geoserverCatalog;

    public Object execute(GetLegendGraphicRequest request, Map<String, Object> map, String colorMap) {

        GridCoverage2D coverage = null;
        try {

            ColorMap cmap = null;

            // Setting up filter functions
            FilterFunction_svgColorMap colorMapFilterFunction = new FilterFunction_svgColorMap();
            FilterFunction_gridCoverageStats statsFilterFunction = new FilterFunction_gridCoverageStats();

            // Only parsing the first layer.
            String layerID = "";
            int digits = DEFAULT_DIGITS; 
            Set<String> params = map.keySet();
            for (String param: params) {
                if (param.equalsIgnoreCase("LAYER")) {
                    layerID = (String) map.get("LAYER"); 
                } else if (param.equalsIgnoreCase("DIGITS")) {
                    digits = Integer.parseInt((String) map.get("DIGITS"));
                }
            }

            final int indexOf = layerID.indexOf(":");
            
            final CoverageInfo coverageInfo = geoserverCatalog.getCoverageByName(new NameImpl(
                    layerID.substring(0, indexOf), layerID.substring(indexOf + 1)));
            List<CoverageDimensionInfo> dimensions = coverageInfo.getDimensions();
            String unit = "";
            if (dimensions != null && !dimensions.isEmpty()) {
                CoverageDimensionInfo dimensionInfo = dimensions.get(0);
                unit = dimensionInfo.getUnit();
            }

            // Getting coverage to parse statistics
            final CoverageStoreInfo storeInfo = coverageInfo.getStore();
            final GridCoverage2DReader reader = (GridCoverage2DReader) geoserverCatalog
                    .getResourcePool().getGridCoverageReader(storeInfo, null);

            GeneralParameterValue[] parameters = parseReadParameters(coverageInfo, map, reader);
            coverage = (GridCoverage2D) reader.read(parameters);

            final double min = (Double) statsFilterFunction.evaluate(coverage, "minimum");
            final double max = (Double) statsFilterFunction.evaluate(coverage, "maximum");

            // Getting a colorMap on top of that
            cmap = (ColorMap) colorMapFilterFunction.evaluate(colorMap, min, max);
            final Builder cmapLegendBuilder = new ColorMapLegendCreator.Builder();
            if (cmap != null && cmap.getColorMapEntries() != null
                    && cmap.getColorMapEntries().length > 0) {

                // setting type of colormap
                cmapLegendBuilder.setColorMapType(cmap.getType());

                // is this colormap using extended colors
                cmapLegendBuilder.setExtended(cmap.getExtendedColors());

                // setting the requested colormap entries
                cmapLegendBuilder.setRequestedDimension(new Dimension(request.getWidth(), request
                        .getHeight()));

                // // setting transparency and background bkgColor
                // cmapLegendBuilder.setTransparent(transparent);
                // cmapLegendBuilder.setBackgroundColor(bgColor);

                // setting band

                // Setting label font and font bkgColor
                cmapLegendBuilder.setLabelFont(LegendUtils.getLabelFont(request));
                cmapLegendBuilder.setLabelFontColor(LegendUtils.getLabelFontColor(request));
                cmapLegendBuilder.setUnit(unit);
                cmapLegendBuilder.setDigits(digits);
                cmapLegendBuilder.setAlternativeColorMapEntryBuilder(true);
                // set band
                // final ChannelSelection channelSelection = rasterSymbolizer.getChannelSelection();
                // cmapLegendBuilder.setBand(channelSelection != null ? channelSelection.getGrayChannel()
                // : null);

                // adding the colormap entries
                final ColorMapEntry[] colorMapEntries = cmap.getColorMapEntries();
                for (ColorMapEntry ce : colorMapEntries)
                    if (ce != null)
                        cmapLegendBuilder.addColorMapEntry(ce);

                // check the additional options before proceeding
                cmapLegendBuilder.checkAdditionalOptions();

                // instantiate the creator
                ColorMapLegendCreator cMapLegendCreator = cmapLegendBuilder.create();
                return new BufferedImageLegendGraphic(cMapLegendCreator.getLegend());
            }
            throw new RuntimeException("Unable to create a legend due to missing colorMap");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } finally {
            if (coverage != null) {
                try {
                    coverage.dispose(true);
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    /**
     * Parse the read parameter from the getLegendGraphicRequest in order to access the proper
     * coverage slice to retrieve the proper statistics. 
     * 
     * @param coverageInfo the coverage to be accessed
     * @param map the request parameters
     * @param reader the reader to be used to access the coverage
     * @return parameters setup on top of requested values.
     * @throws IOException
     * @throws ParseException
     */
    private GeneralParameterValue[] parseReadParameters(final CoverageInfo coverageInfo,
            final Map<String, Object> map, final GridCoverage2DReader reader) throws IOException,
            ParseException {

        // Parameters 
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        GeneralParameterValue[] readParameters = CoverageUtils.getParameters(
                readParametersDescriptor, coverageInfo.getParameters(), false);
        final List<GeneralParameterDescriptor> parameterDescriptors = new ArrayList<GeneralParameterDescriptor>(
                readParametersDescriptor.getDescriptor().descriptors());

        // add the descriptors for custom dimensions
        Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();
        parameterDescriptors.addAll(dynamicParameters);

        final ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
        final MetadataMap metadata = coverageInfo.getMetadata();

        // Setup small envelope to get a piece of coverage to retrieve stats
        final ReferencedEnvelope testEnvelope = createTestEnvelope(coverageInfo);
        final GridGeometry2D gridGeometry = new GridGeometry2D(new GridEnvelope2D(new Rectangle(0, 0, 2,
                2)), testEnvelope);
        readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                gridGeometry, AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString());

        // Parse Time
        readParameters = parseTimeParameter(metadata, readParameters, parameterDescriptors, map);

        // Parse Elevation
        readParameters = parseElevationParameter(metadata, readParameters, parameterDescriptors, map);

        // Parse custom domains
        readParameters = parseCustomDomains(dimensions, metadata, readParameters, parameterDescriptors, map);
        return readParameters;
    }

    /**
     * Parse custom dimension values if present
     * 
     * @param dimensions
     * @param metadata
     * @param readParameters
     * @param parameterDescriptors
     * @param map
     * @return
     * @throws IOException
     */
    private GeneralParameterValue[] parseCustomDomains(final ReaderDimensionsAccessor dimensions,
            final MetadataMap metadata, GeneralParameterValue[] readParameters,
            final List<GeneralParameterDescriptor> parameterDescriptors,
            final Map<String, Object> map) throws IOException {
        List<String> customDomains = new ArrayList(dimensions.getCustomDomains());
        if (customDomains != null && customDomains.size() > 0) {
            Set<String> params = map.keySet();
            for (String paramName : params) {
                if (paramName.regionMatches(true, 0, "dim_", 0, 4)) {
                    String name = paramName.substring(4);

                    // Getting the dimension
                    name = caseInsensitiveLookup(customDomains, name);
                    if (name != null) {
                        final DimensionInfo customInfo = metadata.get(
                                ResourceInfo.CUSTOM_DIMENSION_PREFIX + name, DimensionInfo.class);
                        if (dimensions.hasDomain(name) && customInfo != null && customInfo.isEnabled()) {
                            final ArrayList<String> val = new ArrayList<String>(1);
                            String value = (String) map.get(paramName);
                            if (value.indexOf(",") > 0) {
                                String[] elements = value.split(",");
                                val.addAll(Arrays.asList(elements));
                            } else {
                                val.add(value);
                            }
                            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters, val, name);
                        }
                    }
                }
            }
        }
        return readParameters;

    }

    /**
     *  Parse the elevation parameter if present.
     * 
     * @param metadata the elevationInfo metadata object
     * @param readParameters the readParameters to be set
     * @param parameterDescriptors the reader's parameter descriptors
     * @param map the request's parameters
     * @return the updated parameter set
     * @throws ParseException
     */
    private GeneralParameterValue[] parseElevationParameter(final MetadataMap metadata,
            GeneralParameterValue[] readParameters,
            final List<GeneralParameterDescriptor> parameterDescriptors,
            final Map<String, Object> map) {
        final DimensionInfo elevationInfo = metadata.get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        if (elevationInfo != null && elevationInfo.isEnabled()) {
            // handle "current"
            Set<String> params = map.keySet();
            for (String param : params) {
                if (param.equalsIgnoreCase("elevation")) {
                    readParameters = CoverageUtils.mergeParameter(parameterDescriptors,
                            readParameters, Double.valueOf((String) map.get(param)), "ELEVATION",
                            "Elevation");
                    break;
                }
            }
        }
        return readParameters;
    }

    /**
     *  Parse the time parameter if present.
     * 
     * @param metadata the timeInfo metadata object
     * @param readParameters the readParameters to be set
     * @param parameterDescriptors the reader's parameter descriptors
     * @param map the request's parameters
     * @return the updated parameter set
     * @throws ParseException
     */
    private GeneralParameterValue[] parseTimeParameter(final MetadataMap metadata,
            GeneralParameterValue[] readParameters,
            final List<GeneralParameterDescriptor> parameterDescriptors,
            final Map<String, Object> map) throws ParseException {
        final DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
        if (timeInfo != null && timeInfo.isEnabled()) {
            final Set<String> params = map.keySet();
            for (String param : params) {
                if (param.equalsIgnoreCase("time")) {
                    // pass down the parameters
                    readParameters = CoverageUtils.mergeParameter(parameterDescriptors,
                            readParameters, parser.parse((String) map.get(param)), "TIME", "Time");
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
     * Create a small 2x2 envelope to be used to read a small coverage in order to retrieve statistics from it
     * 
     * @param coverageInfo
     * @return
     */
    private ReferencedEnvelope createTestEnvelope(final CoverageInfo coverageInfo) {
        final ReferencedEnvelope envelope = coverageInfo.getNativeBoundingBox();
        final GridGeometry geometry = coverageInfo.getGrid();
        final MathTransform transform = geometry.getGridToCRS();

        // Creating a 2x2 envelope to get a sample coverage to retrieve statistics from that small piece
        final double scaleX = XAffineTransform.getScaleX0((AffineTransform) transform);
        final double scaleY = XAffineTransform.getScaleY0((AffineTransform) transform);
        final double minX = envelope.getMinimum(0);
        final double minY = envelope.getMinimum(1);
        final ReferencedEnvelope newEnvelope = new ReferencedEnvelope(minX, minX + scaleX * 2,
                minY, minY + scaleY * 2, envelope.getCoordinateReferenceSystem());
        return newEnvelope;
    }

}
