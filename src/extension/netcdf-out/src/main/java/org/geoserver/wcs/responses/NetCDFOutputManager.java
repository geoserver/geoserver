/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.converter.ConversionException;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.responses.NetCDFDimensionsManager.NetCDFDimensionMapping;
import org.geoserver.wcs.responses.NetCDFDimensionsManager.NetCDFDimensionMapping.DimensionValuesSet;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geoserver.wcs2_0.response.DimensionBean.DimensionType;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.wcs2_0.response.WCS20GetCoverageResponse;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.web.netcdf.DataPacking;
import org.geoserver.web.netcdf.DataPacking.DataPacker;
import org.geoserver.web.netcdf.DataPacking.DataStats;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.ExtraVariable;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.GlobalAttribute;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.VariableAttribute;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geoserver.web.netcdf.layer.NetCDFParserBean;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.io.netcdf.cf.Entry;
import org.geotools.coverage.io.netcdf.cf.NetCDFCFParser;
import org.geotools.coverage.io.util.DateRangeComparator;
import org.geotools.coverage.io.util.NumberRangeComparator;
import org.geotools.image.ImageWorker;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.logging.Logging;

import it.geosolutions.jaiext.range.NoDataContainer;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.units.NoSuchUnitException;
import ucar.units.PrefixDBException;
import ucar.units.SpecificationException;
import ucar.units.StandardUnitFormat;
import ucar.units.UnitDBException;
import ucar.units.UnitParseException;
import ucar.units.UnitSystemException;

/**
 * A class which takes care of initializing NetCDF dimension from coverages dimension, 
 * variables, values for the NetCDF output file and finally write them when invoking 
 * the write method.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 */
public class NetCDFOutputManager {

    private static StandardUnitFormat SUF = StandardUnitFormat.instance(); 
    
    public static final String STANDARD_NAME = "STANDARD_NAME";

    public static final String UNIT = "UNIT";

    public static final String NETCDF_VERSION_KEY = "NetCDFVersion";

    public static final Logger LOGGER = Logging.getLogger("org.geoserver.wcs.responses.NetCDFOutputManager");

    private static final double EQUALITY_DELTA = 1E-10; //Consider customizing it depending on the noData magnitude

    /**
     * Attributes that are never copied to the main output variable from a NetCDF/GRIB source because they require special handling.
     */
    @SuppressWarnings("serial")
    private static final Set<String> COPY_ATTRIBUTES_BLACKLIST = new HashSet<String>() {
        {
            // coordinate variable names are usually changed
            add("coordinates");
            // these do not survive type change or packing and should be set from nodata value
            add("_FillValue");
            add("missing_value");
        }
    };

    /** 
     * A dimension mapping between dimension names and dimension manager instances
     * We use a Linked map to preserve the dimension order 
     */
    private NetCDFDimensionsManager dimensionsManager = new NetCDFDimensionsManager();

    
    /** A sample reference granule to get basic properties. */
    private GridCoverage2D sampleGranule;

    /** The stack of granules containing all the GridCoverage2D to be written. */
    private GranuleStack granuleStack;

    /** The global attributes to be added to the output NetCDF */
    private List<GlobalAttribute> globalAttributes;

    /** The variable attributes to be added to the output variable */
    private List<VariableAttribute> variableAttributes;

    /** The extra variables to be copied from the source to output NetCDF */
    private List<ExtraVariable> extraVariables;

    private boolean shuffle = NetCDFSettingsContainer.DEFAULT_SHUFFLE;

    /** Whether to copy attributes from NetCDF source variable to output variable */
    private boolean copyAttributes = NetCDFSettingsContainer.DEFAULT_COPY_ATTRIBUTES;

    private int compressionLevel = NetCDFSettingsContainer.DEFAULT_COMPRESSION;

    private DataPacking dataPacking = DataPacking.getDefault();

    /** The underlying {@link NetcdfFileWriter} which will be used to write down data. */
    private NetcdfFileWriter writer;

    private Version version;

    /** The user supplied variableName */
    private String variableName;

    /** The user supplied unit of measure */
    private String variableUoM;

    /** 
     * The unitConverter to be used to convert the pixel values from the input unit 
     * (the one coming from the original coverage) to output unit (the one specified
     * by the user). As an instance, we may work against a sea_surface_temperature 
     * coverage having temperature in celsius whilst we want to store it back
     * as a sea_surface_temperature in kelvin. In that case the machinery will setup
     * a not-null unitConverter to be used at time of data writing.
     * */
    private UnitConverter unitConverter = null;

    /** Bean related to the {@link NetCDFCFParser} */
    private static NetCDFParserBean parserBean;

    /** The instance of the class delegated to do proper NetCDF coordinates setup*/
    private NetCDFCRSWriter crsWriter;

    private double noDataValue;

    private DataStats stats = null;

    private DataPacker dataPacker;

    static {
        // Getting the NetCDFCFParser bean
        parserBean = GeoServerExtensions.bean(NetCDFParserBean.class);
    }

    /**
     * {@link NetCDFOutputManager} constructor.
     * @param granuleStack the granule stack to be written
     * @param file an output file
     * @throws IOException
     */
    public NetCDFOutputManager(final GranuleStack granuleStack, final File file) throws IOException {
       this(granuleStack, file, null, null);
    }

    /**
     * {@link NetCDFOutputManager} constructor.
     * @param granuleStack the granule stack to be written
     * @param file an output file
     * @param encodingParameters customized encoding params
     * @throws IOException
     */
    public NetCDFOutputManager(GranuleStack granuleStack, File file,
            Map<String, String> encodingParameters, String outputFormat) throws IOException {
        this.granuleStack = granuleStack;
        parseParams(encodingParameters);
        this.writer = getWriter(file, outputFormat);
        collectCoverageDimensions();
        initializeNetCDF();
    }

    /** 
     * Basic NetCDF Initialization 
     */
    private void initializeNetCDF() {
        // Initialize the coordinates writer
        crsWriter = new NetCDFCRSWriter(writer, sampleGranule);

        // Initialize the Dimensions and coordinates variable
        initializeDimensions();

        // Initialize the variable by setting proper coordinates and attributes
        initializeVariables();

        initializeGlobalAttributes();
    }

    private NetcdfFileWriter getWriter(File file, String outputFormat) throws IOException {
        if (NetCDFUtilities.NETCDF4_MIMETYPE.equalsIgnoreCase(outputFormat)) {
            version = Version.netcdf4_classic;
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Requested output format " + outputFormat + " isn't "
                        + NetCDFUtilities.NETCDF4_MIMETYPE + "\nFallback to Version 3");
            }
            // Version 3 as fallback (the Default)
            version = Version.netcdf3;
        }

        NetcdfFileWriter writer = null;
        if (version == Version.netcdf4_classic) {
            if (!NetCDFUtilities.isNC4CAvailable()) {
                throw new IOException(NetCDFUtilities.NC4_ERROR_MESSAGE);
            }
            Nc4Chunking chunker = new Nc4ChunkingDefault(compressionLevel, shuffle);
            writer = NetcdfFileWriter.createNew(version, file.getAbsolutePath(), chunker);
        }

        return writer != null ? writer : NetcdfFileWriter.createNew(Version.netcdf3, file.getAbsolutePath());
    }

    /**
     * Initialize the Manager by collecting all dimensions from the granule stack 
     * and preparing the mapping. 
     */
    private void collectCoverageDimensions() {

        final List<DimensionBean> dimensions = granuleStack.getDimensions();
        for (DimensionBean dimension : dimensions) {

            // Create a new DimensionManager for each dimension
            final String name = dimension.getName();
            final NetCDFDimensionMapping mapper = new NetCDFDimensionMapping(name);

            // Set the input coverage dimension
            mapper.setCoverageDimension(dimension);

            // Set the dimension values type
            final DimensionType dimensionType = dimension.getDimensionType();
            final boolean isRange = dimension.isRange();
            TreeSet<Object> tree = null;
            switch (dimensionType) {
            case TIME:
                tree = new TreeSet(new DateRangeComparator());
//                isRange ? new TreeSet(new DateRangeComparator()) : new TreeSet<Date>();
                break;
            case ELEVATION:
                tree = new TreeSet(new NumberRangeComparator());
//                isRange ? new TreeSet(new NumberRangeComparator()) : new TreeSet<Number>();
                break;
            case CUSTOM:
                String dataType = dimension.getDatatype();
                if (NetCDFUtilities.isATime(dataType)) {
                    tree = 
                            //new TreeSet(new DateRangeComparator());
                            isRange ? new TreeSet(new DateRangeComparator()) : new TreeSet<Object>();
                } else {
                    tree = //new TreeSet<Object>();
                            isRange ? new TreeSet(new NumberRangeComparator()) : new TreeSet<Object>();
                }
            }
            mapper.setDimensionValues(new DimensionValuesSet(tree));
            dimensionsManager.add(name, mapper);
        }

        // Get the dimension values from the coverage and put them on the mapping
        // Note that using tree set allows to respect the ordering when writing
        // down the NetCDF dimensions
        sampleGranule = granuleStack.getGranules().get(0);
        double[] statisticsPeriods = null;
        if (!(dataPacking == DataPacking.NONE)) {
            // Should we take into account numBands
            AffineTransform transform = (AffineTransform) sampleGranule.getGridGeometry().getGridToCRS();
            statisticsPeriods = new double[]{1 * XAffineTransform.getScaleX0(transform),
                    1 * XAffineTransform.getScaleY0(transform)};
            stats = new DataStats();
        }
        for (GridCoverage2D coverage : granuleStack.getGranules()) {
            updateDimensionValues(coverage);
            if (!(dataPacking == DataPacking.NONE)) {
                collectStats(coverage, statisticsPeriods);
            }
        }
    }

    /**
     * Collects stats for future dataPacking from the provided coverage and update 
     * the statistics.
     * 
     * @param coverage
     * @param statsParams
     */
    private void collectStats(GridCoverage2D coverage, double[] statsParams) {
        // It will internally take care of noData
        ImageWorker iw = new ImageWorker(coverage.getRenderedImage());
        double[] minimum = iw.getMinimums();
        double[] maximum = iw.getMaximums();
        double min = minimum[0];
        double max = maximum[0];
        stats.update(min,max);
    }

    /**
     * Parse encodingParams
     * 
     * @param encodingParameters
     */
    private void parseParams(Map<String, String> encodingParameters) {
        Set<String> keys = encodingParameters.keySet();
        if (keys != null && !keys.isEmpty() && keys.contains(WCS20GetCoverageResponse.COVERAGE_ID_PARAM)) {
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
                if (map != null && !map.isEmpty()
                        && map.containsKey(NetCDFSettingsContainer.NETCDFOUT_KEY)) {
                    NetCDFLayerSettingsContainer settings = (NetCDFLayerSettingsContainer) map.get(
                            NetCDFSettingsContainer.NETCDFOUT_KEY,
                            NetCDFLayerSettingsContainer.class);
                    shuffle = settings.isShuffle();
                    copyAttributes = settings.isCopyAttributes();
                    dataPacking = settings.getDataPacking();
                    compressionLevel = checkLevel(settings.getCompressionLevel());
                    variableName = settings.getLayerName();
                    variableUoM = settings.getLayerUOM();
                    globalAttributes = settings.getGlobalAttributes();
                    variableAttributes = settings.getVariableAttributes();
                    extraVariables = settings.getExtraVariables();
                }
            }
        }
    }

    private int checkLevel(Integer level) {
        if (level == null || (level < 0 || level > 9)) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("NetCDF 4 compression Level not in the proper range [0, 9]: "
                        + level + "\nProceeding with default value: " +
                        NetCDFSettingsContainer.DEFAULT_COMPRESSION);
            }
            return NetCDFSettingsContainer.DEFAULT_COMPRESSION;
        }
        return level;
    }

    /**
     * Update the dimension values of a Dimension, by inspecting the coverage properties
     * 
     * @param coverage
     */
    private void updateDimensionValues(GridCoverage2D coverage) {
        Map properties = coverage.getProperties();
        for (NetCDFDimensionMapping dimension : dimensionsManager.getDimensions()) {
            final String dimensionName = dimension.getName();
            final Object value = properties.get(dimensionName);
            if (value == null) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("No Dimensions available with the specified name: " + dimensionName);
                }
            } else {
                dimension.getDimensionValues().addValue(value);
            }
        }
    }


    /**
     * Initialize the dimensions by creating NetCDF Dimensions of the proper type.
     */
    private void initializeDimensions() {

        initializeHigherRankDimensions();

        // Initialize the lat,lon/y,x 2D dimensions and coordinates
        dimensionsManager.addDimensions(crsWriter.initialize2DCoordinatesDimensions());

    }

    /**
     * Initialize higher rank dimensions, such as time, elevation, custom, ...
     */
    private void initializeHigherRankDimensions() {
        // TODO: Do we support coverages which doesn't share same BBox?
        // I assume they will still have the same bbox, eventually filled with background data/fill value

        // Loop over dimensions
        Dimension boundDimension = null;
        for (NetCDFDimensionMapping dimension : dimensionsManager.getDimensions()) {
            final DimensionBean dim = dimension.getCoverageDimension();
            final boolean isRange = dim.isRange();
            String dimensionName = dimension.getName();
            final int dimensionLength = dimension.getDimensionValues().getSize();
            if (dimensionName.equalsIgnoreCase("TIME") || dimensionName.equalsIgnoreCase("ELEVATION")) {
                // Special management for TIME and ELEVATION dimensions
                // we will put these dimension lowercase for NetCDF names
                dimensionName = dimensionName.toLowerCase();
            }
            if (isRange) {
                if (boundDimension == null) {
                    boundDimension = writer.addDimension(null, NetCDFUtilities.BOUNDARY_DIMENSION, 2);
                }
            }
            final Dimension netcdfDimension = writer.addDimension(null, dimensionName, dimensionLength);
            dimension.setNetCDFDimension(netcdfDimension);

            // Assign variable to dimensions having coordinates
            Variable var = writer.addVariable(null, dimensionName,
                    NetCDFUtilities.getNetCDFDataType(dim.getDatatype()), dimensionName);
            writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.LONG_NAME, dimensionName));
            writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.DESCRIPTION, dimensionName)); 
            // TODO: introduce some lookup table to get a description if needed

            if (NetCDFUtilities.isATime(dim.getDatatype())) {
                // Special management for times. We use the NetCDF convention of defining times starting from
                // an origin. Right now we use the Linux EPOCH
                writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.UNITS, NetCDFUtilities.TIME_ORIGIN));
            } else {
                writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.UNITS, dim.getSymbol()));
            }

            // Add bounds variable for ranges
            if (isRange) {
                final List<Dimension> boundsDimensions = new ArrayList<Dimension>();
                boundsDimensions.add(netcdfDimension);
                boundsDimensions.add(boundDimension);
                final String boundName = dimensionName + NetCDFUtilities.BOUNDS_SUFFIX;
                writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.BOUNDS, boundName));
                writer.addVariable(null, boundName, NetCDFUtilities.getNetCDFDataType(dim.getDatatype()), boundsDimensions);
            }
        }
    }

    /**
     * Initialize the NetCDF variables on this writer
     */
    private void initializeVariables() {

        // group the dimensions to be added to the variable
        List<Dimension> netCDFDimensions = new LinkedList<Dimension>();
        for (NetCDFDimensionMapping dimension : dimensionsManager.getDimensions()) {
            netCDFDimensions.add(dimension.getNetCDFDimension());
        }

        String coverageName = sampleGranule.getName().toString();

        // Set the proper dataType
        int dataType = sampleGranule.getRenderedImage().getSampleModel().getDataType();

        DataType varDataType = getDataType(dataType);
        if (variableName != null && !variableName.isEmpty()) {
            coverageName = variableName;
        }
        Variable var = writer.addVariable(null, coverageName, varDataType, netCDFDimensions);
        GridSampleDimension[] sampleDimensions = sampleGranule.getSampleDimensions();

        // no data management
        boolean noDataSet = false;
        noDataValue = Double.NaN;
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

        // Adding long-name
        if (variableName != null && !variableName.isEmpty()) {
            writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.LONG_NAME, variableName));
        }

        // Adding Units
        if (var.findAttribute(NetCDFUtilities.UNITS) == null) {
            String unit = null;
            boolean hasDefinedUoM = (variableUoM != null && !variableUoM.isEmpty());
            if (hasDefinedUoM) {
                unit = variableUoM;
            } else if (inputUoM != null) {
                unit = inputUoM.toString();
            }
            if (unit != null) {
                writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.UNITS, unit));
            }
            if (inputUoM != null && hasDefinedUoM) {
                // Replace some chars from the UOM to make sure it can be properly parsed
                // by the JSR Unit class. We can refactor this replacement by using bytes check
                // instead of specific replace calls.
                String unitString = variableUoM.replace(" ", "*").replace("-", "^-").replace(".", "*")
                .replace("m2","m^2").replace("m3","m^3").replace("s2", "s^2");
                try {
                    Unit outputUoM = Unit.valueOf(unitString);
                    if (outputUoM != null && !inputUoM.equals(outputUoM)) {
                        if (!inputUoM.isCompatible(outputUoM)){
                            if (LOGGER.isLoggable(Level.WARNING)) {
                                LOGGER.warning("input unit " + inputUoM.toString() 
                                        + " and output unit " + outputUoM.toString() 
                                        + " are incompatible.\nNo unit conversion will be performed");
                            }
                        } else {
                            unitConverter = inputUoM.getConverterTo(outputUoM);
                        }
                    }
                } catch (ConversionException ce) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe("Unable to create a converter for the specified unit: " + unitString 
                                + "\nNo unit conversion will be performed" );
                    }
                } catch (IllegalArgumentException e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe("Unable to parse the specified unit: " + unitString 
                                + "\nNo unit conversion will be performed" );
                    }
                }
            }
        }
        // Adding standard name if name and units are cf-compliant
        if (checkCompliant(var)) {
            writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.STANDARD_NAME,
                    variableName));
        }
        if (dataPacking != DataPacking.NONE) {
            DataStats updatedStats = stats;
            if (unitConverter != null) {
                //Update stats by applying unitConversion
                updatedStats.setMax(unitConverter.convert(updatedStats.getMax()));
                updatedStats.setMin(unitConverter.convert(updatedStats.getMin()));
            }
            dataPacker = dataPacking.getDataPacker(updatedStats);
            writer.addVariableAttribute(var, new Attribute(DataPacking.ADD_OFFSET, dataPacker.getOffset()));
            writer.addVariableAttribute(var, new Attribute(DataPacking.SCALE_FACTOR, dataPacker.getScale()));
        }

        if (noDataSet) {
            Number noData = dataPacker != null ? dataPacker.getReservedValue() : noDataValue;
            writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.FILL_VALUE, NetCDFUtilities.transcodeNumber(varDataType, noData)));
        }
        
        // Initialize the gridMapping part of the variable
        crsWriter.initializeGridMapping(var);

        // Copy from source NetCDF
        if (copyAttributes || extraVariables != null && !extraVariables.isEmpty()) {
            try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                if (source != null) {
                    if (copyAttributes) {
                        Variable sourceVar = source
                                .findVariable(sampleGranule.getName().toString());
                        if (sourceVar == null) {
                            LOGGER.info(String.format(
                                    "Could not copy attributes because "
                                            + "variable '%s' not found in NetCDF/GRIB %s",
                                    sampleGranule.getName().toString(), source.getLocation()));
                        } else {
                            for (Attribute att : sourceVar.getAttributes()) {
                                // do not allow overwrite or attributes in blacklist
                                if (var.findAttribute(att.getFullName()) == null
                                        && !COPY_ATTRIBUTES_BLACKLIST
                                                .contains(att.getShortName())) {
                                    writer.addVariableAttribute(var, att);
                                }
                            }
                        }
                    }
                    if (extraVariables != null) {
                        for (ExtraVariable extra : extraVariables) {
                            Variable sourceVar = source.findVariable(extra.getSource());
                            if (sourceVar == null) {
                                LOGGER.info(String.format(
                                        "Could not find extra variable source '%s' "
                                                + "in NetCDF/GRIB %s",
                                        extra.getSource(), source.getLocation()));
                            } else if (!sourceVar.getDimensionsString().isEmpty()) {
                                LOGGER.info(String.format(
                                        "Only scalar extra variables are supported but source "
                                                + "'%s' in NetCDF/GRIB %s has dimensions '%s'",
                                        extra.getSource(), source.getLocation(),
                                        sourceVar.getDimensionsString()));
                            } else if (writer.findVariable(extra.getOutput()) != null) {
                                LOGGER.info(
                                        String.format("Extra variable output '%s' already exists",
                                                extra.getOutput()));
                            } else if (extra.getDimensions().split("\\s").length > 1) {
                                LOGGER.info(String.format(
                                        "Extra variable output '%s' "
                                                + "has too many dimensions '%s'",
                                        extra.getOutput(), extra.getDimensions()));
                            } else {
                                Variable outputVar = writer.addVariable(null, extra.getOutput(),
                                        sourceVar.getDataType(), extra.getDimensions());
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
            for (VariableAttribute att : variableAttributes) {
                writer.deleteVariableAttribute(var, att.getKey());
                writer.addVariableAttribute(var, buildAttribute(att.getKey(), att.getValue()));
            }
        }

    }

    private DataType getDataType(int dataType) {
        DataType outDataType = dataPacking.getDataType();
        if (outDataType == null) {
            // This may happen for NONE dataPacking
            outDataType = NetCDFUtilities.transcodeImageDataType(dataType);
        }
        return outDataType; 
    }

    /**
     * Method checking if LayerName and LayerUOM are compliant
     */
    private boolean checkCompliant(Variable var) {
        // Check in the Variable
        if (var == null) {
            // Variable is not present
            return false;
        }
        // Check the layer name
        if (variableName == null || variableName.isEmpty()) {
            // Wrong Layer name
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
        // Getting the parser
        NetCDFCFParser parser = parserBean.getParser();
        // Checking CF convention
        boolean validName = parser.hasEntryId(variableName) || parser.hasAliasId(variableName);
        // Checking UOM
        Entry e = parser.getEntry(variableName) != null ? parser.getEntry(variableName) : parser
                .getEntryFromAlias(variableName);
        
        boolean validUOM = false;
        if (e != null) {
            String canonical = e.getCanonicalUnits();
            String definedUnit = unit.getStringValue();
            if (canonical.equalsIgnoreCase(definedUnit)) {
                validUOM = true;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Canonical unit and specified unit are equal. "
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
                            LOGGER.fine("Canonical unit and specified unit are compatible. "
                                    + "Proceeding with standard_name set");
                        }
                    }
                } catch (NoSuchUnitException e1) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(e1.getLocalizedMessage());
                    }
                } catch (UnitParseException e1) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(e1.getLocalizedMessage());
                    }
                } catch (SpecificationException e1) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(e1.getLocalizedMessage());
                    }
                } catch (UnitDBException e1) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(e1.getLocalizedMessage());
                    }
                } catch (PrefixDBException e1) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(e1.getLocalizedMessage());
                    }
                } catch (UnitSystemException e1) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(e1.getLocalizedMessage());
                    }
                }
                if (!parseable) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("The specified unit " + definedUnit + " can't be converted to a " +
                                " UCAR unit so it doesn't allow to define a standard name");
                    }
                }

            }
        }
                
                
        // Return the result
        return validName && validUOM;
    }

    /**
     * Set the variables values
     * @param writer
     * @throws IOException
     * @throws InvalidRangeException
     */
    private void writeDataValues() throws IOException, InvalidRangeException {

        // Initialize dimensions sizes
        final int numDimensions = dimensionsManager.getNumDimensions();
        final int[] dimSize = new int[numDimensions];
        final String[] dimName = new String[numDimensions];
        int iDim = 0;
        for (NetCDFDimensionMapping dimension: dimensionsManager.getDimensions()) {
            dimSize[iDim] = dimension.getDimensionValues().getSize();
            dimName[iDim] = dimension.getNetCDFDimension().getShortName();
            iDim++;
        }
        String name = variableName != null ? variableName : sampleGranule.getName().toString();
        final Variable var = writer.findVariable(name);
        if (var == null) {
            throw new IllegalArgumentException("The requested variable doesn't exists: " + name);
        }

        // Non-scalar ExtraVariable
        class ExtraVariableRecord {

            public final ExtraVariable extraVariable;

            // index into indexing array
            public final int dimensionIndex;

            // indices for which a data value as been written
            public final Set<Integer> writtenIndices = new HashSet<Integer>();

            public ExtraVariableRecord(ExtraVariable extraVariable, int dimensionIndex) {
                this.extraVariable = extraVariable;
                this.dimensionIndex = dimensionIndex;
            }

        }

        List<ExtraVariableRecord> nonscalarExtraVariables = new ArrayList<ExtraVariableRecord>();
        if (extraVariables != null) {
            List<ExtraVariable> scalarExtraVariables = new ArrayList<ExtraVariable>();
            for (ExtraVariable extra : extraVariables) {
                if (extra.getDimensions().isEmpty()) {
                    scalarExtraVariables.add(extra);
                } else {
                    for (int dimensionIndex = 0; dimensionIndex < numDimensions; dimensionIndex++) {
                        // side effect of this condition is to skip extra variables
                        // with multiple output dimensions (unsupported)
                        if (extra.getDimensions().equals(dimName[dimensionIndex])) {
                            nonscalarExtraVariables
                                    .add(new ExtraVariableRecord(extra, dimensionIndex));
                            break;
                        }
                    }
                }
            }
            // copy scalar extra variable data
            if (!scalarExtraVariables.isEmpty()) {
                try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                    for (ExtraVariable extra : scalarExtraVariables) {
                        writer.write(writer.findVariable(extra.getOutput()),
                                source.findVariable(extra.getSource()).read());
                    }
                }
            }
        }

        // Get the data type for a sample image (All granules of the same coverage will use
        // the same sample model 
        final int imageDataType = sampleGranule.getRenderedImage().getSampleModel().getDataType();
        final DataType netCDFDataType = var.getDataType();
        final Array matrix = NetCDFUtilities.getArray(dimSize, netCDFDataType);

        // Loop over all granules
        for (GridCoverage2D gridCoverage: granuleStack.getGranules()) {
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

            int minTileX = minX / tileWidth - (minX < 0 ? (-minX % tileWidth > 0 ? 1 : 0): 0);
            int minTileY = minY / tileHeight - (minY < 0 ? (-minY % tileHeight > 0 ? 1 : 0): 0);
            int maxTileX = maxX / tileWidth - (maxX < 0 ? (-maxX % tileWidth > 0 ? 1 : 0): 0);
            int maxTileY = maxY / tileHeight - (maxY < 0 ? (-maxY % tileHeight > 0 ? 1 : 0): 0);

            final Index matrixIndex = matrix.getIndex();
            final int indexing[] = new int[numDimensions];

            // Update the NetCDF array indexing to set values for a specific 2D slice 
            updateIndexing(indexing, gridCoverage);

            // copy non-scalar extra variable data
            if (!nonscalarExtraVariables.isEmpty()) {
                // Before opening the source NetCDF/GRIB, see if any record requires data from it;
                // we might be iterating over many time/elevation/custom dimensions but have
                // granules with sources in common and want to avoid unnecessary opening of
                // source NetCDF/GRIB. Only the first matching data value is used.
                // This loop also ensures that the source for each granule is only opened once.
                boolean needSource = false;
                for (ExtraVariableRecord record : nonscalarExtraVariables) {
                    if (!record.writtenIndices.contains(indexing[record.dimensionIndex])) {
                        needSource = true;
                        break;
                    }
                }
                if (needSource) {
                    try (NetcdfDataset source = getSourceNetcdfDataset(gridCoverage)) {
                        if (source != null) {
                            for (ExtraVariableRecord record : nonscalarExtraVariables) {
                                if (!record.writtenIndices
                                        .contains(indexing[record.dimensionIndex])) {
                                    writer.write(
                                            writer.findVariable(record.extraVariable.getOutput()),
                                            new int[] { indexing[record.dimensionIndex] },
                                            source.findVariable(record.extraVariable.getSource())
                                                    .read().reshape(new int[] { 1 }));
                                    record.writtenIndices.add(indexing[record.dimensionIndex]);
                                }
                            }
                        }
                    }
                }
            }

            // ----------------
            // Fill data matrix
            // ----------------

            // Loop over bands using a RandomIter 
            final RandomIter data = RandomIterFactory.create(ri, null);
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
                                    matrixIndex.set(indexing);
                                    setPixel(k, j, NetCDFUtilities.transcodeImageDataType(imageDataType), netCDFDataType, data, matrix, matrixIndex);
                                }
                            }
                        }
                    }
                }
            }
            // Finalize the iterator 
            data.done();
        }

        // ------------------------------
        // Write the data to the variable
        // ------------------------------
        writer.write(var, matrix);
        writer.flush();
    }

    /**
     * Get the x, y pixel from the data iterator and assign it to the NetCDF array matrix.
     * Also check if the read pixel is noData and apply the unitConversion (if needed) and
     * dataPacking (if needed).
     * @param x
     * @param y
     * @param imageDataType 
     * @param netCDFDataType
     * @param data
     * @param matrix
     * @param matrixIndex
     */
    private void setPixel(int x, int y, DataType imageDataType, 
            DataType netCDFDataType, RandomIter data, Array matrix, Index matrixIndex) {

        // Read the data, check if nodata and convert it if needed
        int sample = Integer.MIN_VALUE;
        boolean validSample = true;
        switch (imageDataType) {
        case BYTE:
        case SHORT:
        case INT:
            sample =  data.getSample(x, y, 0);
            validSample = !Double.isNaN(noDataValue) && !isNaN(sample, noDataValue);
            if (unitConverter != null && validSample) {
                sample = (int) unitConverter.convert(sample);
            }
            if (dataPacker != null) {
                
                sample = dataPacker.pack((double)sample);
            }
            setIntegerSample(netCDFDataType, matrix, matrixIndex, sample);
            break;
        case FLOAT:
            float sampleFloat = data.getSampleFloat(x, y, 0);
            validSample = !Double.isNaN(noDataValue) && !isNaN(sampleFloat, noDataValue);
            if (unitConverter != null && validSample) {
                sampleFloat = (float) unitConverter.convert(sampleFloat);
            }
            if (dataPacker != null) {
                sample = validSample ? dataPacker.pack(sampleFloat) : dataPacker.getReservedValue();
                setIntegerSample(netCDFDataType, matrix, matrixIndex, sample);
            } else {
                matrix.setFloat(matrixIndex, sampleFloat);
            }
            break;
        case DOUBLE:
            double sampleDouble = data.getSampleDouble(x, y, 0);
            validSample = !Double.isNaN(noDataValue) && !isNaN(sampleDouble, noDataValue);
            if (unitConverter != null && !Double.isNaN(noDataValue) && !isNaN(sampleDouble, noDataValue)) {
                sampleDouble = unitConverter.convert(sampleDouble);
            }
            if (dataPacker != null) {
                sample = validSample ? dataPacker.pack(sampleDouble) : dataPacker.getReservedValue();
                setIntegerSample(netCDFDataType, matrix, matrixIndex, sample);
            } else {
                matrix.setDouble(matrixIndex, sampleDouble);
            }
            break;
        default:
            throw new UnsupportedOperationException("Operation not supported for this dataType: " + netCDFDataType);
        }
    }

    private void setIntegerSample(DataType netCDFDataType, Array matrix, Index matrixIndex, int sample) {
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

    private boolean isNaN(Number sample, double noDataValue) {
        return (Math.abs(noDataValue - sample.doubleValue()) < EQUALITY_DELTA);
    }

    /**
     * Setup the proper NetCDF array indexing, taking current dimension values from the current coverage 
     * @param indexing
     * @param currentCoverage
     */
    private void updateIndexing(final int[] indexing, final GridCoverage2D currentCoverage) {
        int i = 0;
        int dimElement = 0;
        final Map properties = currentCoverage.getProperties();
        for (NetCDFDimensionMapping manager : dimensionsManager.getDimensions()) {
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

    /**
     * Write the NetCDF file
     * @throws IOException
     * @throws InvalidRangeException
     */
    public void write() throws IOException, InvalidRangeException {
        // end of define mode
        writer.create();

        // Setting values
        for (NetCDFDimensionMapping mapper : dimensionsManager.getDimensions()) {
            crsWriter.setCoordinateVariable(mapper);
        }

        writeDataValues();

        // Close the writer
        writer.close();

    }

    /**
     * Add global attributes to the Dataset if needed
     */
    private void initializeGlobalAttributes() {
        if (globalAttributes != null) {
            for (GlobalAttribute att : globalAttributes) {
                if (att.getKey().equalsIgnoreCase(NetCDFUtilities.CONVENTIONS)) {
                    writer.addGroupAttribute(null, new Attribute(NetCDFUtilities.COORD_SYS_BUILDER,
                            NetCDFUtilities.COORD_SYS_BUILDER_CONVENTION));
                }
                writer.addGroupAttribute(null, buildAttribute(att.getKey(), att.getValue()));
            }
        }
    }

    /**
     * Release resources
     */
    public void close() {
        // release resources
        for (NetCDFDimensionMapping mapper: dimensionsManager.getDimensions()){
            mapper.dispose();
        }
        dimensionsManager.dispose();
    }

    /**
     * Return source {@link NetcdfDataset} for this granule or null if it does not have one.
     */
    private NetcdfDataset getSourceNetcdfDataset(GridCoverage2D granule) {
        URL sourceUrl = (URL) granule.getProperty(GridCoverage2DReader.SOURCE_URL_PROPERTY);
        if (sourceUrl != null) {
            try {
                return NetCDFUtilities.getDataset(sourceUrl);
            } catch (Exception e) {
                LOGGER.info(String.format("Failed to open source URL %s as NetCDF/GRIB: %s",
                        sourceUrl, e.getMessage()));
            }
        }
        return null;
    }

    /**
     * Build an {@link Attribute}, trying different numeric types before falling back on string.
     */
    private Attribute buildAttribute(String key, String value) {
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

}
