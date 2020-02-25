/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.web.netcdf.DataPacking;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import si.uom.NonSI;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/** Specific class to encode GHRSST NetCDF files */
public class GHRSSTEncoder extends AbstractNetCDFEncoder {

    /**
     * Ideally these settings should go in a java object, but putting it in the configuration will
     * make XStream fail to deserialize the configuration if the GHRSST plugin is removed and the
     * configuration bean is gone. So using plain jane key/values in the settings metadata map
     * instead
     */
    public static String SETTINGS_KEY = "ghrsst";

    public static String SETTINGS_RDAC_KEY = "ghrsst.rdac";

    public static String SETTINGS_PROCESSING_LEVEL_KEY = "ghrsst.processingLevel";

    public static String SETTINGS_SST_TYPE = "ghrsst.sstType";

    public static String SETTINGS_PRODUCT_STRING = "ghrsst.productString";

    public static final Logger LOGGER = Logging.getLogger(GHRSSTEncoder.class);

    /** The GHRSST specification mandates specific variable types for well known variables */
    private static final Map<String, DataType> GHRSST_VARIABLE_TYPES = new HashMap<>();

    static {
        GHRSST_VARIABLE_TYPES.put("sea_surface_temperature", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sea_surface_skin_temperature", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sea_surface_subskin_temperature", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sea_surface_foundation_temperature", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sea_water_temperature", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sst_dtime", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sses_bias", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sses_standard_deviation", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("dt_analysis", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("wind_speed", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("wind_speed_dtime_from_sst", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sources_of_wind_speed", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sea_ice_fraction", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sea_ice_fraction_dtime_from_sst", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sources_of_sea_ice_fraction", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("aerosol_dynamic_indicator", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("adi_dtime_from_sst", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sources_of_adi", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("l2p_flags", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("quality_level", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("satellite_zenith_angle", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("solar_zenith_angle", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("surface_solar_irradiance", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("ssi_dtime_from_sst", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sources_of_ssi", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("or_latitude", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("or_longitude", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("or_number_of_pixels", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sum_sst", DataType.FLOAT);
        GHRSST_VARIABLE_TYPES.put("sum_square_sst", DataType.FLOAT);
        GHRSST_VARIABLE_TYPES.put("adjusted_sea_surface_temperature", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("adjusted_standard_deviation_error", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("bias_to_reference_sst", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("standard_deviation_to_reference_sst", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sources_of_sst", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("analysed_sst", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("analysis_error", DataType.SHORT);
        GHRSST_VARIABLE_TYPES.put("sea_ice_fraction", DataType.BYTE);
        GHRSST_VARIABLE_TYPES.put("sea_ice_fraction_error", DataType.BYTE);
    }

    /** In case of data packing best to remove these as well */
    private static final Set<String> DATA_PACKING_ATTRIBUTES_BLACKLIST =
            new HashSet<String>() {
                {
                    add("valid_min");
                    add("valid_max");
                    add("valid_range");
                    add("scale_factor");
                    add("add_offset");
                }
            };

    private static final String NETCDF_LIBRARY_VERSION;

    static {
        String version = "unknown";
        try {
            try (InputStream is = GHRSSTEncoder.class.getResourceAsStream("/netcdf.properties")) {
                Properties properties = new Properties();
                properties.load(is);
                String v = properties.getProperty("netcdf.version");
                if (v != null) {
                    version = v;
                }
            }
        } catch (IOException e) {
            LOGGER.log(
                    Level.INFO,
                    "Failed to initialize NetCDF library version from netcdf.properties in classpath",
                    e);
        }

        NETCDF_LIBRARY_VERSION = version;
    }

    /** Holds the configuration for a variable coming from a specific band */
    class BandVariable {
        /** The user supplied variableName */
        private String variableName;

        /** The user supplied unit of measure */
        private String variableUoM;

        private double noDataValue;

        private DataPacking.DataStats stats = null;

        private DataPacking.DataPacker dataPacker;

        private Variable var;

        private DataType netCDFDataType;

        private Array matrix;

        public void initForWriting(NetcdfFileWriter writer, int[] spatialDimensionSize) {
            var = writer.findVariable(variableName);
            if (var == null) {
                throw new IllegalArgumentException(
                        "The requested variable doesn't exists: " + variableName);
            }

            netCDFDataType = var.getDataType();
            matrix = NetCDFUtilities.getArray(spatialDimensionSize, netCDFDataType);
        }
    }

    /** All the band variables to be handled */
    BandVariable[] bandVariables;

    /**
     * {@link DefaultNetCDFEncoder} constructor.
     *
     * @param granuleStack the granule stack to be written
     * @param file an output file
     * @param encodingParameters customized encoding params
     */
    public GHRSSTEncoder(
            GranuleStack granuleStack,
            File file,
            Map<String, String> encodingParameters,
            String outputFormat)
            throws IOException {
        super(granuleStack, file, encodingParameters, outputFormat);
    }

    /** Initialize the NetCDF variables on this writer */
    protected void initializeVariables() {
        // group the dimensions to be added to the variable
        List<Dimension> netCDFDimensions = new LinkedList<Dimension>();
        for (NetCDFDimensionsManager.NetCDFDimensionMapping dimension :
                dimensionsManager.getDimensions()) {
            netCDFDimensions.add(dimension.getNetCDFDimension());
        }

        GridSampleDimension[] sampleDimensions = sampleGranule.getSampleDimensions();
        this.bandVariables = new BandVariable[sampleDimensions.length];
        List<DataPacking.DataStats> bandStatistics = null;
        for (int i = 0; i < sampleDimensions.length; i++) {
            BandVariable bandVariable = new BandVariable();
            bandVariables[i] = bandVariable;
            String bandName = sampleDimensions[i].getDescription().toString();
            bandVariable.variableName = bandName;

            // Set the proper dataType
            // TODO: pick the target data type from a lookup table based on the GHRSST standard
            int dataType = sampleGranule.getRenderedImage().getSampleModel().getDataType();

            DataType originalDataType = getDataType(dataType);
            DataType varDataType = getDataType(bandName, dataType);
            Variable var = writer.addVariable(null, bandName, varDataType, netCDFDimensions);

            // no data management
            boolean noDataSet = false;
            Double noDataValue = Double.NaN;
            Unit<?> inputUoM = null;
            if (sampleDimensions != null && sampleDimensions.length > 0) {
                GridSampleDimension sampleDimension = sampleDimensions[0];
                inputUoM = sampleDimension.getUnits();
                double[] noData = sampleDimension.getNoDataValues();
                if (noData != null && noData.length > 0) {
                    noDataValue = noData[0];
                    noDataSet = true;
                }
            }
            if (!noDataSet) {
                NoDataContainer noDataProperty = CoverageUtilities.getNoDataProperty(sampleGranule);
                if (noDataProperty != null) {
                    noDataValue = noDataProperty.getAsSingleValue();
                    noDataSet = true;
                }
            }
            bandVariable.noDataValue = noDataValue;

            // collect stats for data packing
            DataPacking.DataStats stats = null;
            DataPacking dataPacking = this.dataPacking;
            // ... are we changing the data type on the fly due to GHRSST spec, and going
            // towards a less expressive type?
            if (originalDataType.compareTo(varDataType) > 0 && !varDataType.isFloatingPoint()) {
                switch (varDataType) {
                    case BOOLEAN:
                        break;
                    case BYTE:
                        dataPacking = DataPacking.BYTE;
                        break;
                    case SHORT:
                        dataPacking = DataPacking.SHORT;
                        break;
                    case INT:
                        dataPacking = DataPacking.INT;
                        break;
                    case LONG:
                        dataPacking = DataPacking.LONG;
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Don't know how to handle packing for data type " + varDataType);
                }
            }

            if (!(dataPacking == DataPacking.NONE)) {
                if (bandStatistics == null) {
                    bandStatistics = new ArrayList<>();
                    for (int j = 0; j < sampleDimensions.length; j++) {
                        bandStatistics.add(new DataPacking.DataStats());
                    }
                    for (GridCoverage2D coverage : granuleStack.getGranules()) {
                        collectStats(coverage, bandStatistics);
                    }
                }
                stats = bandStatistics.get(i);
            }
            bandVariable.stats = stats;

            // Adding Units
            if (var.findAttribute(NetCDFUtilities.UNITS) == null) {
                String unit = null;
                if (inputUoM != null) {
                    unit = inputUoM.toString();
                }
                if (unit != null) {
                    writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.UNITS, unit));
                    bandVariable.variableUoM = unit;
                }
            }

            // Adding standard name if name and units are cf-compliant
            if (checkCompliant(var)) {
                writer.addVariableAttribute(
                        var, new Attribute(NetCDFUtilities.STANDARD_NAME, bandName));
            }

            // handle data packing
            DataPacking.DataPacker dataPacker = null;
            if (dataPacking != DataPacking.NONE) {
                // if copying attributes, we might copy over valid min and max, which might be out
                // of range vs the collected stats
                if (copyAttributes) {
                    try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                        if (source != null) {
                            Variable sourceVar = source.findVariable(bandName);
                            Double min = getDoubleAttribute(sourceVar, NetCDFUtilities.VALID_MIN);
                            Double max = getDoubleAttribute(sourceVar, NetCDFUtilities.VALID_MAX);
                            if (min != null && max != null) {
                                stats.update(min, max);
                            }
                        }
                    } catch (Exception e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.severe("Failed to copy from source NetCDF: " + e.getMessage());
                        }
                    }
                }

                dataPacker = dataPacking.getDataPacker(stats);
                writer.addVariableAttribute(
                        var, new Attribute(DataPacking.ADD_OFFSET, dataPacker.getOffset()));
                writer.addVariableAttribute(
                        var, new Attribute(DataPacking.SCALE_FACTOR, dataPacker.getScale()));
            }

            if (noDataSet) {
                Number noData = dataPacker != null ? dataPacker.getReservedValue() : noDataValue;
                writer.addVariableAttribute(
                        var,
                        new Attribute(
                                NetCDFUtilities.FILL_VALUE,
                                NetCDFUtilities.transcodeNumber(varDataType, noData)));
            }
            bandVariable.dataPacker = dataPacker;

            // Initialize the gridMapping part of the variable
            crsWriter.initializeGridMapping(var);

            // Copy from source NetCDF
            if (copyAttributes || extraVariables != null && !extraVariables.isEmpty()) {
                try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                    if (source != null) {
                        if (copyAttributes) {
                            Variable sourceVar = source.findVariable(bandName);
                            if (sourceVar == null) {
                                LOGGER.info(
                                        String.format(
                                                "Could not copy attributes because "
                                                        + "variable '%s' not found in NetCDF/GRIB %s",
                                                sampleGranule.getName().toString(),
                                                source.getLocation()));
                            } else {
                                for (Attribute att : sourceVar.getAttributes()) {
                                    // do not allow overwrite or attributes in blacklist
                                    if (var.findAttribute(att.getFullName()) == null
                                            && !isBlacklistedAttribute(att, dataPacking)) {
                                        writer.addVariableAttribute(var, att);
                                    }
                                }
                            }
                        }

                        // if datapacker is set and we have min/max valid value, repack them
                        if (dataPacking != DataPacking.NONE) {
                            Variable sourceVar = source.findVariable(bandName);
                            addValidMinMax(
                                    sourceVar, var, dataPacker, writer, NetCDFUtilities.VALID_MIN);
                            addValidMinMax(
                                    sourceVar, var, dataPacker, writer, NetCDFUtilities.VALID_MAX);
                        }

                        if (extraVariables != null) {
                            for (NetCDFSettingsContainer.ExtraVariable extra : extraVariables) {
                                Variable sourceVar = source.findVariable(extra.getSource());
                                if (sourceVar == null) {
                                    LOGGER.info(
                                            String.format(
                                                    "Could not find extra variable source '%s' "
                                                            + "in NetCDF/GRIB %s",
                                                    extra.getSource(), source.getLocation()));
                                } else if (!sourceVar.getDimensionsString().isEmpty()) {
                                    LOGGER.info(
                                            String.format(
                                                    "Only scalar extra variables are supported but source "
                                                            + "'%s' in NetCDF/GRIB %s has dimensions '%s'",
                                                    extra.getSource(),
                                                    source.getLocation(),
                                                    sourceVar.getDimensionsString()));
                                } else if (writer.findVariable(extra.getOutput()) != null) {
                                    LOGGER.info(
                                            String.format(
                                                    "Extra variable output '%s' already exists",
                                                    extra.getOutput()));
                                } else if (extra.getDimensions().split("\\s").length > 1) {
                                    LOGGER.info(
                                            String.format(
                                                    "Extra variable output '%s' "
                                                            + "has too many dimensions '%s'",
                                                    extra.getOutput(), extra.getDimensions()));
                                } else {
                                    Variable outputVar =
                                            writer.addVariable(
                                                    null,
                                                    extra.getOutput(),
                                                    sourceVar.getDataType(),
                                                    extra.getDimensions());
                                    for (Attribute att : sourceVar.getAttributes()) {
                                        writer.addVariableAttribute(outputVar, att);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe("Failed to copy from source NetCDF: " + e.getMessage());
                    }
                }
            }

            // Apply variable attributes from settings (allowing overwrite)
            if (variableAttributes != null) {
                for (NetCDFSettingsContainer.VariableAttribute att : variableAttributes) {
                    writer.deleteVariableAttribute(var, att.getKey());
                    writer.addVariableAttribute(var, buildAttribute(att.getKey(), att.getValue()));
                }
            }
        }
    }

    private Double getDoubleAttribute(Variable sourceVar, String attributeName) {
        Attribute attribute = sourceVar.findAttribute(attributeName);
        if (attribute != null) {
            return attribute.getNumericValue().doubleValue();
        } else {
            return null;
        }
    }

    private void addValidMinMax(
            Variable sourceVar,
            Variable var,
            DataPacking.DataPacker dataPacker,
            NetcdfFileWriter writer,
            String attributeName) {
        Attribute attribute = sourceVar.findAttribute(attributeName);
        if (attribute != null) {
            double value = attribute.getNumericValue().doubleValue();
            int packedValue = dataPacker.pack(value);
            writer.addVariableAttribute(var, new Attribute(attributeName, packedValue));
        }
    }

    private boolean isBlacklistedAttribute(Attribute att, DataPacking dataPacking) {
        // part of the blacklist?
        String shortName = att.getShortName();
        if (COPY_ATTRIBUTES_BLACKLIST.contains(shortName)) {
            return true;
        }

        // in case of data packing also valid_min and valid_max should go
        if (dataPacking != DataPacking.NONE) {
            return DATA_PACKING_ATTRIBUTES_BLACKLIST.contains(shortName);
        }

        return false;
    }

    private DataType getDataType(String variableName, int dataType) {
        // special case for sst_dtime (commented out as it does not work...)
        /*
        if ("sst_dtime".equalsIgnoreCase(variableName) && (version == NetcdfFileWriter.Version.netcdf4_classic
                || version == NetcdfFileWriter.Version.netcdf4)) {
            // TODO: should check if this is a L3 file
            return DataType.LONG;
        }
        */

        DataType outDataType = GHRSST_VARIABLE_TYPES.get(variableName);
        if (outDataType == null) {
            outDataType = getDataType(dataType);
        }

        return outDataType;
    }

    /** Set the variables values */
    protected void writeDataValues() throws IOException, InvalidRangeException {

        // Initialize dimensions sizes
        final int numDimensions = dimensionsManager.getNumDimensions();
        final int[] dimSize = new int[numDimensions];
        final String[] dimName = new String[numDimensions];
        int iDim = 0;
        for (NetCDFDimensionsManager.NetCDFDimensionMapping dimension :
                dimensionsManager.getDimensions()) {
            dimSize[iDim] = dimension.getDimensionValues().getSize();
            dimName[iDim] = dimension.getNetCDFDimension().getShortName();
            iDim++;
        }
        // allocate an array of dimensions for the spatial slice matching the granule in the stack
        final int[] spatialDimensionSize = new int[numDimensions];
        for (int i = 0; i < numDimensions - 2; i++) {
            spatialDimensionSize[i] = 1;
        }
        spatialDimensionSize[numDimensions - 2] = dimSize[numDimensions - 2];
        spatialDimensionSize[numDimensions - 1] = dimSize[numDimensions - 1];

        writeNonScalarExtraVariables(dimName);

        // prepare writing matrices
        for (BandVariable bandVariable : bandVariables) {
            bandVariable.initForWriting(writer, spatialDimensionSize);
        }

        // Loop over all granules
        for (GridCoverage2D gridCoverage : granuleStack.getGranules()) {
            final RenderedImage ri = gridCoverage.getRenderedImage();

            //
            // Preparing tile properties for future scan
            //
            int width = ri.getWidth();
            int height = ri.getHeight();
            int minX = ri.getMinX();
            int minY = ri.getMinY();
            int maxX = minX + width - 1;
            int maxY = minY + height - 1;
            int tileWidth = Math.min(ri.getTileWidth(), width);
            int tileHeight = Math.min(ri.getTileHeight(), height);

            int minTileX = minX / tileWidth - (minX < 0 ? (-minX % tileWidth > 0 ? 1 : 0) : 0);
            int minTileY = minY / tileHeight - (minY < 0 ? (-minY % tileHeight > 0 ? 1 : 0) : 0);
            int maxTileX = maxX / tileWidth - (maxX < 0 ? (-maxX % tileWidth > 0 ? 1 : 0) : 0);
            int maxTileY = maxY / tileHeight - (maxY < 0 ? (-maxY % tileHeight > 0 ? 1 : 0) : 0);

            // Update the NetCDF array indexing to set values for a specific 2D slice
            final int originIndexing[] = new int[numDimensions];
            updateIndexing(originIndexing, gridCoverage);

            // ----------------
            // Fill data matrix
            // ----------------

            // Loop over bands using a RandomIter
            final RandomIter data = RandomIterFactory.create(ri, null);
            final int imageDataType =
                    sampleGranule.getRenderedImage().getSampleModel().getDataType();
            DataType sourceDataType = NetCDFUtilities.transcodeImageDataType(imageDataType);
            // the local slice indexing, all the extra dimensions are set to 0, the spatial ones are
            // set in the loop
            int[] indexing = new int[numDimensions];
            Arrays.fill(indexing, 0);
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                    for (int trow = 0; trow < tileHeight; trow++) {
                        int j = (tileY * tileHeight) + trow;
                        if ((j >= minY) && (j <= maxY)) {
                            for (int tcol = 0; tcol < tileWidth; tcol++) {
                                int col = (tileX * tileWidth) + tcol;
                                if ((col >= minX) && (col <= maxX)) {
                                    int k = col;
                                    final int yPos = height - j + minY - 1;

                                    // Simply setting lat and lon
                                    indexing[numDimensions - 1] = k - minX;
                                    indexing[numDimensions - 2] = yPos;

                                    for (int bandIdx = 0;
                                            bandIdx < bandVariables.length;
                                            bandIdx++) {
                                        BandVariable bandVariable = bandVariables[bandIdx];

                                        final Index matrixIndex = bandVariable.matrix.getIndex();
                                        matrixIndex.set(indexing);
                                        setPixel(
                                                k,
                                                j,
                                                sourceDataType,
                                                bandVariable.netCDFDataType,
                                                data,
                                                bandVariable.matrix,
                                                matrixIndex,
                                                bandVariable.dataPacker,
                                                bandVariable.noDataValue,
                                                null,
                                                bandIdx);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // dump all collected matrices
            for (BandVariable bandVariable : bandVariables) {
                writer.write(bandVariable.var, originIndexing, bandVariable.matrix);
            }
            writer.flush();

            // Finalize the iterator
            data.done();
        }
    }

    /** Add global attributes to the Dataset if needed */
    protected void initializeGlobalAttributes() {
        copyGlobalAttribute();

        // add computed global attributes
        writer.addGroupAttribute(null, new Attribute("uuid", UUID.randomUUID().toString()));
        writer.addGroupAttribute(null, new Attribute("netcdf_version_id", NETCDF_LIBRARY_VERSION));
        String isoDate = toISODate(new Date());
        writer.addGroupAttribute(null, new Attribute("date_created", isoDate));
        writer.addGroupAttribute(
                null, new Attribute("spatial_resolution", getSpatialResolutionDescription()));
        DateRange startEnd = getDatasetDateRange();
        if (startEnd != null) {
            String startIsoTime = toISODate(startEnd.getMinValue());
            writer.addGroupAttribute(null, new Attribute("start_time", startIsoTime));
            writer.addGroupAttribute(null, new Attribute("time_coverage_start", startIsoTime));
            String endIsoTime = toISODate(startEnd.getMaxValue());
            writer.addGroupAttribute(null, new Attribute("stop_time", endIsoTime));
            writer.addGroupAttribute(null, new Attribute("time_coverage_end", endIsoTime));
        }
        try {
            GeneralEnvelope wgs84Envelope =
                    CRS.transform(sampleGranule.getEnvelope(), DefaultGeographicCRS.WGS84);
            writer.addGroupAttribute(
                    null, new Attribute("northernmost_latitude", wgs84Envelope.getMaximum(1)));
            writer.addGroupAttribute(
                    null, new Attribute("southernmost_latitude", wgs84Envelope.getMinimum(1)));
            writer.addGroupAttribute(
                    null, new Attribute("easternmost_longitude", wgs84Envelope.getMaximum(0)));
            writer.addGroupAttribute(
                    null, new Attribute("westernmost_longitude", wgs84Envelope.getMinimum(0)));
        } catch (TransformException e) {
            LOGGER.log(Level.FINE, "Failed to compute WGS84 envelope, GHRRST bounds", e);
        }
        // assumption here is that if we have to specify the units, the coordinates can be something
        // other
        // than WGS84, e.g. projected (otherwise the unit would always be degrees, no?... the spec
        // is not clear here...)
        double[] resolutions = getResolutions();
        String unit = getAxisUnit();
        if (resolutions != null) {
            writer.addGroupAttribute(null, new Attribute("geospatial_lat_units", unit));
            writer.addGroupAttribute(
                    null, new Attribute("geospatial_lat_resolution", resolutions[0]));
            writer.addGroupAttribute(null, new Attribute("geospatial_lon_units", unit));
            writer.addGroupAttribute(
                    null, new Attribute("geospatial_lon_resolution", resolutions[1]));
        }

        addGlobalAttributesFromSettings();
    }

    private DateRange getDatasetDateRange() {
        Date startDate = null;
        Date endDate = null;
        for (NetCDFDimensionsManager.NetCDFDimensionMapping dimension :
                dimensionsManager.getDimensions()) {
            if ("time".equalsIgnoreCase(dimension.getName())) {
                TreeSet<Object> values =
                        (TreeSet<Object>) dimension.getDimensionValues().getValues();
                Object first = values.first();
                if (first instanceof Date) {
                    startDate = (Date) first;
                } else if (first instanceof DateRange) {
                    startDate = ((DateRange) first).getMinValue();
                } else {
                    throw new IllegalArgumentException(
                            "Unrecognized data type for start date: " + first);
                }

                Object last = values.last();
                if (last instanceof Date) {
                    endDate = (Date) last;
                } else if (last instanceof DateRange) {
                    endDate = ((DateRange) first).getMaxValue();
                } else {
                    throw new IllegalArgumentException(
                            "Unrecognized data type for end date: " + first);
                }
            }
        }

        if (startDate != null && endDate != null) {
            return new DateRange(startDate, endDate);
        } else {
            return null;
        }
    }

    private double[] getResolutions() {
        MathTransform2D gridToCRS2D = this.sampleGranule.getGridGeometry().getGridToCRS2D();
        if (!(gridToCRS2D instanceof AffineTransform2D)) {
            return null;
        }
        AffineTransform2D at = (AffineTransform2D) gridToCRS2D;
        return new double[] {Math.abs(at.getScaleX()), Math.abs(at.getScaleY())};
    }

    private String getSpatialResolutionDescription() {
        double[] resolutions = getResolutions();
        if (resolutions == null) {
            return "Unknown";
        }
        double resolution = Math.max(resolutions[0], resolutions[1]);
        String unitName = getAxisUnit();
        return resolution + " " + unitName;
    }

    private String getAxisUnit() {
        CoordinateReferenceSystem crs = this.sampleGranule.getCoordinateReferenceSystem2D();
        CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
        Unit<?> unit = axis.getUnit();
        if (unit == null) {
            return "";
        } else if (unit == NonSI.DEGREE_ANGLE) {
            return "degrees";
        } else {
            return unit.toString();
        }
    }

    private String toISODate(Date date) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.ENGLISH);
        isoFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return isoFormat.format(date);
    }
}
