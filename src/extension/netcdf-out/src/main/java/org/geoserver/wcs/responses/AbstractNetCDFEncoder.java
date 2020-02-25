/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.UnitConverter;
import javax.media.jai.iterator.RandomIter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.wcs2_0.response.WCS20GetCoverageResponse;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.web.netcdf.DataPacking;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geoserver.web.netcdf.layer.NetCDFParserBean;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.io.netcdf.cf.Entry;
import org.geotools.coverage.io.netcdf.cf.NetCDFCFParser;
import org.geotools.image.ImageWorker;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.logging.Logging;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.units.PrefixDBException;
import ucar.units.SpecificationException;
import ucar.units.StandardUnitFormat;
import ucar.units.UnitDBException;
import ucar.units.UnitSystemException;

public abstract class AbstractNetCDFEncoder implements NetCDFEncoder {

    protected static StandardUnitFormat SUF = StandardUnitFormat.instance();

    public static final Logger LOGGER = Logging.getLogger(AbstractNetCDFEncoder.class);

    protected static final double EQUALITY_DELTA =
            1E-10; // Consider customizing it depending on the noData magnitude

    /**
     * Attributes that are never copied to the main output variable from a NetCDF/GRIB source
     * because they require special handling.
     */
    @SuppressWarnings("serial")
    protected static final Set<String> COPY_ATTRIBUTES_BLACKLIST =
            new HashSet<String>() {
                {
                    // coordinate variable names are usually changed
                    add("coordinates");
                    // these do not survive type change or packing and should be set from nodata
                    // value
                    add("_FillValue");
                    add("missing_value");
                    // this one is better not copied over in case of subsetting instead
                    add("_ChunkSizes");
                }
            };

    /**
     * Global Attributes that are never copied from a NetCDF/GRIB source because they require
     * special handling.
     */
    @SuppressWarnings("serial")
    protected static final Set<String> COPY_GLOBAL_ATTRIBUTES_BLACKLIST =
            new HashSet<String>() {
                {
                    // Not copying this since the UCAR writer will create it again
                    add("_NCProperties");
                }
            };

    /** Bean related to the {@link NetCDFCFParser} */
    protected static NetCDFParserBean parserBean = GeoServerExtensions.bean(NetCDFParserBean.class);

    /**
     * A dimension mapping between dimension names and dimension manager instances We use a Linked
     * map to preserve the dimension order
     */
    protected NetCDFDimensionsManager dimensionsManager = new NetCDFDimensionsManager();

    /** A sample reference granule to get basic properties. */
    protected GridCoverage2D sampleGranule;

    /** The stack of granules containing all the GridCoverage2D to be written. */
    protected GranuleStack granuleStack;

    /** The global attributes to be added to the output NetCDF */
    protected List<NetCDFSettingsContainer.GlobalAttribute> globalAttributes;

    /** The variable attributes to be added to the output variable */
    protected List<NetCDFSettingsContainer.VariableAttribute> variableAttributes;

    /** The extra variables to be copied from the source to output NetCDF */
    protected List<NetCDFSettingsContainer.ExtraVariable> extraVariables;

    protected boolean shuffle = NetCDFSettingsContainer.DEFAULT_SHUFFLE;

    /** Whether to copy attributes from NetCDF source variable to output variable */
    protected boolean copyAttributes = NetCDFSettingsContainer.DEFAULT_COPY_ATTRIBUTES;

    /** Weather to copy global attributes from NetCDF source to output */
    protected boolean copyGlobalAttributes = NetCDFSettingsContainer.DEFAULT_COPY_GLOBAL_ATTRIBUTES;

    protected int compressionLevel = NetCDFSettingsContainer.DEFAULT_COMPRESSION;

    protected DataPacking dataPacking = DataPacking.getDefault();

    /** The underlying {@link NetcdfFileWriter} which will be used to write down data. */
    protected NetcdfFileWriter writer;

    protected NetcdfFileWriter.Version version;

    /** The instance of the class delegated to do proper NetCDF coordinates setup */
    protected NetCDFCRSWriter crsWriter;

    /**
     * {@link DefaultNetCDFEncoder} constructor.
     *
     * @param granuleStack the granule stack to be written
     * @param file an output file
     * @param encodingParameters customized encoding params
     */
    public AbstractNetCDFEncoder(
            GranuleStack granuleStack,
            File file,
            Map<String, String> encodingParameters,
            String outputFormat)
            throws IOException {
        this.granuleStack = granuleStack;
        this.sampleGranule = granuleStack.getGranules().get(0);
        NetCDFLayerSettingsContainer settings = getSettings(encodingParameters);
        if (settings != null) {
            initializeFromSettings(settings);
        }
        this.writer = getWriter(file, outputFormat);
        dimensionsManager.collectCoverageDimensions(this.granuleStack);
        initializeNetCDF();
    }

    protected void initializeFromSettings(NetCDFLayerSettingsContainer settings) {
        shuffle = settings.isShuffle();
        copyAttributes = settings.isCopyAttributes();
        copyGlobalAttributes = settings.isCopyGlobalAttributes();
        dataPacking = settings.getDataPacking();
        compressionLevel = checkLevel(settings.getCompressionLevel());
        globalAttributes = settings.getGlobalAttributes();
        variableAttributes = settings.getVariableAttributes();
        extraVariables = settings.getExtraVariables();
    }

    /** Basic NetCDF Initialization */
    protected void initializeNetCDF() {
        // Initialize the coordinates writer
        crsWriter = new NetCDFCRSWriter(writer, sampleGranule);

        // Initialize the Dimensions and coordinates variable
        initializeDimensions();

        // Initialize the variable by setting proper coordinates and attributes
        initializeVariables();

        initializeGlobalAttributes();
    }

    /** Initializes the actual data variables in the output NetCDF (not lon/lat/time/elevation) */
    protected abstract void initializeVariables();

    /** Add global attributes to the Dataset if needed */
    protected void initializeGlobalAttributes() {
        copyGlobalAttribute();
        addGlobalAttributesFromSettings();
    }

    /** Adds the global attributes from the settings in the UI */
    protected void addGlobalAttributesFromSettings() {
        // Add global attributes from settings
        if (globalAttributes != null) {
            for (NetCDFSettingsContainer.GlobalAttribute att : globalAttributes) {
                if (att.getKey().equalsIgnoreCase(NetCDFUtilities.CONVENTIONS)) {
                    writer.addGroupAttribute(
                            null,
                            new Attribute(
                                    NetCDFUtilities.COORD_SYS_BUILDER,
                                    NetCDFUtilities.COORD_SYS_BUILDER_CONVENTION));
                }
                writer.addGroupAttribute(null, buildAttribute(att.getKey(), att.getValue()));
            }
        }
    }

    /** Copies over the global attributes, if enabled */
    protected void copyGlobalAttribute() {
        // Copy global attributes from source NetCDF
        if (copyGlobalAttributes) {
            try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                if (source != null) {
                    for (Attribute att : source.getGlobalAttributes()) {
                        // part of the blacklist?
                        String shortName = att.getShortName();
                        if (!COPY_GLOBAL_ATTRIBUTES_BLACKLIST.contains(shortName)) {
                            writer.addGroupAttribute(null, att);
                        }
                    }
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe("Failed to copy from source NetCDF: " + e.getMessage());
                }
            }
        }
    }

    /** Return source {@link NetcdfDataset} for this granule or null if it does not have one. */
    protected NetcdfDataset getSourceNetcdfDataset(GridCoverage2D granule) {
        URL sourceUrl = (URL) granule.getProperty(GridCoverage2DReader.SOURCE_URL_PROPERTY);
        if (sourceUrl != null) {
            try {
                return NetCDFUtilities.getDataset(sourceUrl);
            } catch (Exception e) {
                LOGGER.info(
                        String.format(
                                "Failed to open source URL %s as NetCDF/GRIB: %s",
                                sourceUrl, e.getMessage()));
            }
        }
        return null;
    }

    /** Build an {@link Attribute}, trying different numeric types before falling back on string. */
    protected Attribute buildAttribute(String key, String value) {
        try {
            return new Attribute(key, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            // ignore
        }
        try {
            return new Attribute(key, Double.parseDouble(value));
        } catch (NumberFormatException e) {
            // ignore
        }
        return new Attribute(key, value);
    }

    protected NetcdfFileWriter getWriter(File file, String outputFormat) throws IOException {
        if (NetCDFUtilities.NETCDF4_MIMETYPE.equalsIgnoreCase(outputFormat)) {
            version = NetcdfFileWriter.Version.netcdf4_classic;
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Requested output format "
                                + outputFormat
                                + " isn't "
                                + NetCDFUtilities.NETCDF4_MIMETYPE
                                + "\nFallback to Version 3");
            }
            // Version 3 as fallback (the Default)
            version = NetcdfFileWriter.Version.netcdf3;
        }

        NetcdfFileWriter writer = null;
        if (version == NetcdfFileWriter.Version.netcdf4_classic) {
            if (!NetCDFUtilities.isNC4CAvailable()) {
                throw new IOException(NetCDFUtilities.NC4_ERROR_MESSAGE);
            }
            Nc4Chunking chunker = new Nc4ChunkingDefault(compressionLevel, shuffle);
            writer = NetcdfFileWriter.createNew(version, file.getAbsolutePath(), chunker);
        }

        return writer != null
                ? writer
                : NetcdfFileWriter.createNew(
                        NetcdfFileWriter.Version.netcdf3, file.getAbsolutePath());
    }

    /**
     * Collects stats for future dataPacking from the provided coverage and update the statistics.
     *
     * @param coverage The coverage on which the statistics will be collected
     * @param statsList The list of statistic beans, one per image band
     */
    protected void collectStats(GridCoverage2D coverage, List<DataPacking.DataStats> statsList) {
        // It will internally take care of noData
        ImageWorker iw = new ImageWorker(coverage.getRenderedImage());
        double[] minimum = iw.getMinimums();
        double[] maximum = iw.getMaximums();
        int count = Math.min(minimum.length, statsList.size());
        for (int i = 0; i < count; i++) {
            DataPacking.DataStats stats = statsList.get(i);
            double min = minimum[i];
            double max = maximum[i];
            stats.update(min, max);
        }
    }

    /** Parse encodingParams */
    protected NetCDFLayerSettingsContainer getSettings(Map<String, String> encodingParameters) {
        Set<String> keys = encodingParameters.keySet();
        if (keys != null
                && !keys.isEmpty()
                && keys.contains(WCS20GetCoverageResponse.COVERAGE_ID_PARAM)) {
            String coverageId = encodingParameters.get(WCS20GetCoverageResponse.COVERAGE_ID_PARAM);
            if (coverageId != null) {
                GeoServer geoserver = GeoServerExtensions.bean(GeoServer.class);
                MetadataMap map = null;
                if (geoserver != null) {
                    Catalog gsCatalog = geoserver.getCatalog();
                    LayerInfo info = NCNameResourceCodec.getCoverage(gsCatalog, coverageId);
                    if (info != null) {
                        map = info.getResource().getMetadata();
                    }
                }
                if (map != null
                        && !map.isEmpty()
                        && map.containsKey(NetCDFSettingsContainer.NETCDFOUT_KEY)) {
                    NetCDFLayerSettingsContainer settings =
                            map.get(
                                    NetCDFSettingsContainer.NETCDFOUT_KEY,
                                    NetCDFLayerSettingsContainer.class);
                    return settings;
                }
            }
        }

        return null;
    }

    protected int checkLevel(Integer level) {
        if (level == null || (level < 0 || level > 9)) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(
                        "NetCDF 4 compression Level not in the proper range [0, 9]: "
                                + level
                                + "\nProceeding with default value: "
                                + NetCDFSettingsContainer.DEFAULT_COMPRESSION);
            }
            return NetCDFSettingsContainer.DEFAULT_COMPRESSION;
        }
        return level;
    }

    /** Update the dimension values of a Dimension, by inspecting the coverage properties */
    protected void updateDimensionValues(GridCoverage2D coverage) {
        Map properties = coverage.getProperties();
        for (NetCDFDimensionsManager.NetCDFDimensionMapping dimension :
                dimensionsManager.getDimensions()) {
            final String dimensionName = dimension.getName();
            final Object value = properties.get(dimensionName);
            if (value == null) {
                Set<String> dimensions = crsWriter.getCoordinatesDimensionNames();
                // Coordinates dimensions (lon/lat) aren't taken into account
                // for values update. Do not warn if they are missing
                if (dimensions != null
                        && !dimensions.contains(dimensionName)
                        && LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(
                            "No Dimensions available with the specified name: " + dimensionName);
                }
            } else {
                dimension.getDimensionValues().addValue(value);
            }
        }
    }

    /** Initialize the dimensions by creating NetCDF Dimensions of the proper type. */
    protected void initializeDimensions() {

        initializeHigherRankDimensions();

        // Initialize the lat,lon/y,x 2D dimensions and coordinates
        dimensionsManager.addDimensions(crsWriter.initialize2DCoordinatesDimensions());
    }

    /** Initialize higher rank dimensions, such as time, elevation, custom, ... */
    protected void initializeHigherRankDimensions() {
        // TODO: Do we support coverages which doesn't share same BBox?
        // I assume they will still have the same bbox, eventually filled with background data/fill
        // value

        // Loop over dimensions
        Dimension boundDimension = null;
        for (NetCDFDimensionsManager.NetCDFDimensionMapping dimension :
                dimensionsManager.getDimensions()) {
            final DimensionBean dim = dimension.getCoverageDimension();
            final boolean isRange = dim.isRange();
            String dimensionName = dimension.getName();
            final int dimensionLength = dimension.getDimensionValues().getSize();
            if (dimensionName.equalsIgnoreCase("TIME")
                    || dimensionName.equalsIgnoreCase("ELEVATION")) {
                // Special management for TIME and ELEVATION dimensions
                // we will put these dimension lowercase for NetCDF names
                dimensionName = dimensionName.toLowerCase();
            }
            if (isRange) {
                if (boundDimension == null) {
                    boundDimension =
                            writer.addDimension(null, NetCDFUtilities.BOUNDARY_DIMENSION, 2);
                }
            }
            final Dimension netcdfDimension =
                    writer.addDimension(null, dimensionName, dimensionLength);
            dimension.setNetCDFDimension(netcdfDimension);

            // Assign variable to dimensions having coordinates
            Variable var =
                    writer.addVariable(
                            null,
                            dimensionName,
                            NetCDFUtilities.getNetCDFDataType(dim.getDatatype()),
                            dimensionName);
            writer.addVariableAttribute(
                    var, new Attribute(NetCDFUtilities.LONG_NAME, dimensionName));
            writer.addVariableAttribute(
                    var, new Attribute(NetCDFUtilities.DESCRIPTION, dimensionName));
            // TODO: introduce some lookup table to get a description if needed

            if (NetCDFUtilities.isATime(dim.getDatatype())) {
                // Special management for times. We use the NetCDF convention of defining times
                // starting from
                // an origin. Right now we use the Linux EPOCH
                writer.addVariableAttribute(
                        var, new Attribute(NetCDFUtilities.UNITS, NetCDFUtilities.TIME_ORIGIN));
            } else {
                writer.addVariableAttribute(
                        var, new Attribute(NetCDFUtilities.UNITS, dim.getSymbol()));
            }

            // Add bounds variable for ranges
            if (isRange) {
                final List<Dimension> boundsDimensions = new ArrayList<Dimension>();
                boundsDimensions.add(netcdfDimension);
                boundsDimensions.add(boundDimension);
                final String boundName = dimensionName + NetCDFUtilities.BOUNDS_SUFFIX;
                writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.BOUNDS, boundName));
                writer.addVariable(
                        null,
                        boundName,
                        NetCDFUtilities.getNetCDFDataType(dim.getDatatype()),
                        boundsDimensions);
            }
        }
    }

    /** Write the NetCDF file */
    public void write() throws IOException, ucar.ma2.InvalidRangeException {
        // end of define mode
        writer.create();

        try {
            // Setting values
            for (NetCDFDimensionsManager.NetCDFDimensionMapping mapper :
                    dimensionsManager.getDimensions()) {
                crsWriter.setCoordinateVariable(mapper);
            }

            writeDataValues();
        } finally {
            // Close the writer
            writer.close();
        }
    }

    /** Actually writes out values into the output NetCDf */
    protected abstract void writeDataValues() throws IOException, InvalidRangeException;

    /** Release resources */
    public void close() {
        // release resources
        for (NetCDFDimensionsManager.NetCDFDimensionMapping mapper :
                dimensionsManager.getDimensions()) {
            mapper.dispose();
        }
        dimensionsManager.dispose();
    }

    protected DataType getDataType(int dataType) {
        DataType outDataType = dataPacking.getDataType();
        if (outDataType == null) {
            // This may happen for NONE dataPacking
            outDataType = NetCDFUtilities.transcodeImageDataType(dataType);
        }
        return outDataType;
    }

    /** Method checking if LayerName and LayerUOM are compliant */
    protected boolean checkCompliant(Variable var) {
        // Check in the Variable
        if (var == null) {
            // Variable is not present
            return false;
        }

        // Check the unit is defined
        Attribute unit = var.findAttribute(NetCDFUtilities.UNITS);
        if (unit == null) {
            // No unit defined
            return false;
        }
        if (parserBean == null || parserBean.getParser() == null) {
            // Unable to check if it is cf-compliant
            return false;
        }

        String variableName = var.getShortName();
        // Getting the parser
        NetCDFCFParser parser = parserBean.getParser();
        // Checking CF convention
        boolean validName = parser.hasEntryId(variableName) || parser.hasAliasId(variableName);
        // Checking UOM
        Entry e =
                parser.getEntry(variableName) != null
                        ? parser.getEntry(variableName)
                        : parser.getEntryFromAlias(variableName);

        boolean validUOM = false;
        if (e != null) {
            String canonical = e.getCanonicalUnits();
            String definedUnit = unit.getStringValue();
            if (canonical.equalsIgnoreCase(definedUnit)) {
                validUOM = true;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Canonical unit and specified unit are equal. "
                                    + "Proceeding with standard_name set");
                }
            } else {
                boolean parseable = false;
                try {
                    ucar.units.Unit ucarUnit = SUF.parse(definedUnit);
                    ucar.units.Unit canonicalUnit = SUF.parse(canonical);
                    if (ucarUnit.isCompatible(canonicalUnit)) {
                        validUOM = true;
                        parseable = true;
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine(
                                    "Canonical unit and specified unit are compatible. "
                                            + "Proceeding with standard_name set");
                        }
                    }
                } catch (UnitDBException
                        | SpecificationException
                        | PrefixDBException
                        | UnitSystemException e1) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(e1.getLocalizedMessage());
                    }
                }
                if (!parseable) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info(
                                "The specified unit "
                                        + definedUnit
                                        + " can't be converted to a "
                                        + " UCAR unit so it doesn't allow to define a standard name");
                    }
                }
            }
        }

        // Return the result
        return validName && validUOM;
    }

    /**
     * Get the x, y pixel from the data iterator and assign it to the NetCDF array matrix. Also
     * check if the read pixel is noData and apply the unitConversion (if needed) and dataPacking
     * (if needed).
     */
    protected void setPixel(
            int x,
            int y,
            DataType imageDataType,
            DataType netCDFDataType,
            RandomIter data,
            Array matrix,
            Index matrixIndex,
            DataPacking.DataPacker dataPacker,
            Double noDataValue,
            UnitConverter unitConverter,
            int bandIdx) {

        // Read the data, check if nodata and convert it if needed
        int sample;
        boolean validSample;
        switch (imageDataType) {
            case BYTE:
            case SHORT:
            case INT:
                sample = data.getSample(x, y, bandIdx);
                validSample = !isNaN(sample, noDataValue);
                if (unitConverter != null && validSample) {
                    sample = (int) unitConverter.convert(sample);
                }
                if (dataPacker != null) {
                    sample = dataPacker.pack((double) sample);
                }
                setIntegerSample(netCDFDataType, matrix, matrixIndex, sample);
                break;
            case FLOAT:
                float sampleFloat = data.getSampleFloat(x, y, bandIdx);
                validSample = !isNaN(sampleFloat, noDataValue);
                if (unitConverter != null && validSample) {
                    sampleFloat = (float) unitConverter.convert(sampleFloat);
                }
                if (dataPacker != null) {
                    sample =
                            validSample
                                    ? dataPacker.pack(sampleFloat)
                                    : dataPacker.getReservedValue();
                    setIntegerSample(netCDFDataType, matrix, matrixIndex, sample);
                } else {
                    matrix.setFloat(matrixIndex, sampleFloat);
                }
                break;
            case DOUBLE:
                double sampleDouble = data.getSampleDouble(x, y, bandIdx);
                validSample = !isNaN(sampleDouble, noDataValue);
                if (unitConverter != null && validSample) {
                    sampleDouble = unitConverter.convert(sampleDouble);
                }
                if (dataPacker != null) {
                    sample =
                            validSample
                                    ? dataPacker.pack(sampleDouble)
                                    : dataPacker.getReservedValue();
                    setIntegerSample(netCDFDataType, matrix, matrixIndex, sample);
                } else {
                    matrix.setDouble(matrixIndex, sampleDouble);
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Operation not supported for this dataType: " + netCDFDataType);
        }
    }

    protected void setIntegerSample(
            DataType netCDFDataType, Array matrix, Index matrixIndex, int sample) {
        switch (netCDFDataType) {
            case BYTE:
                matrix.setByte(matrixIndex, (byte) sample);
                break;
            case SHORT:
                matrix.setShort(matrixIndex, (short) sample);
                break;
            case INT:
                matrix.setInt(matrixIndex, sample);
                break;
        }
    }

    protected boolean isNaN(Number sample, double noDataValue) {
        double sampleValue = sample.doubleValue();
        if (Double.isNaN(noDataValue)) {
            return Double.isNaN(sampleValue);
        }
        return (Math.abs(noDataValue - sample.doubleValue()) < EQUALITY_DELTA);
    }

    /**
     * Setup the proper NetCDF array indexing, taking current dimension values from the current
     * coverage
     */
    protected void updateIndexing(final int[] indexing, final GridCoverage2D currentCoverage) {
        int i = 0;
        int dimElement = 0;
        final Map properties = currentCoverage.getProperties();
        for (NetCDFDimensionsManager.NetCDFDimensionMapping manager :
                dimensionsManager.getDimensions()) {
            // Loop over dimensions
            final DimensionBean coverageDimension = manager.getCoverageDimension();
            if (coverageDimension != null) { // Lat and lon doesn't have a Coverage dimension
                final String dimensionName = manager.getName();

                // Get the current value for that dimension for this coverage
                final Object val = properties.get(dimensionName);

                // Get all the values for that dimension, looking for the one
                // which matches the coverage's one
                // TODO: Improve this search. Make it more smart/performant
                final Set<Object> values = (Set<Object>) manager.getDimensionValues().getValues();
                final Iterator<Object> it = values.iterator();
                while (it.hasNext()) {
                    Object value = it.next();
                    if (value.equals(val)) {
                        indexing[i++] = dimElement;
                        dimElement = 0;
                        break;
                    }
                    dimElement++;
                }
            }
        }
    }

    // Non-scalar ExtraVariable
    protected static class ExtraVariableRecord {

        public final NetCDFSettingsContainer.ExtraVariable extraVariable;

        // index into indexing array
        public final int dimensionIndex;

        // indices for which a data value as been written
        public final Set<Integer> writtenIndices = new HashSet<>();

        public ExtraVariableRecord(
                NetCDFSettingsContainer.ExtraVariable extraVariable, int dimensionIndex) {
            this.extraVariable = extraVariable;
            this.dimensionIndex = dimensionIndex;
        }
    }

    /** Writes out all non scalar extra variable configured */
    protected List<ExtraVariableRecord> writeNonScalarExtraVariables(String[] dimName)
            throws IOException, InvalidRangeException {
        List<ExtraVariableRecord> nonscalarExtraVariables = new ArrayList<>();
        if (extraVariables != null) {
            List<NetCDFSettingsContainer.ExtraVariable> scalarExtraVariables = new ArrayList<>();
            for (NetCDFSettingsContainer.ExtraVariable extra : extraVariables) {
                if (extra.getDimensions().isEmpty()) {
                    scalarExtraVariables.add(extra);
                } else {
                    for (int dimensionIndex = 0;
                            dimensionIndex < dimName.length;
                            dimensionIndex++) {
                        // side effect of this condition is to skip extra variables
                        // with multiple output dimensions (unsupported)
                        if (extra.getDimensions().equals(dimName[dimensionIndex])) {
                            nonscalarExtraVariables.add(
                                    new ExtraVariableRecord(extra, dimensionIndex));
                            break;
                        }
                    }
                }
            }
            // copy scalar extra variable data
            if (!scalarExtraVariables.isEmpty()) {
                try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                    for (NetCDFSettingsContainer.ExtraVariable extra : scalarExtraVariables) {
                        writer.write(
                                writer.findVariable(extra.getOutput()),
                                source.findVariable(extra.getSource()).read());
                    }
                }
            }
        }

        return nonscalarExtraVariables;
    }
}
