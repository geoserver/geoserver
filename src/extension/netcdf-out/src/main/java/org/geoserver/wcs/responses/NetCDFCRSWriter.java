/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wcs.responses.NetCDFDimensionsManager.NetCDFDimensionMapping;
import org.geoserver.wcs.responses.NetCDFDimensionsManager.NetCDFDimensionMapping.DimensionValuesArray;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.io.netcdf.crs.NetCDFCoordinateReferenceSystemType;
import org.geotools.coverage.io.netcdf.crs.NetCDFCoordinateReferenceSystemType.NetCDFCoordinate;
import org.geotools.coverage.io.netcdf.crs.NetCDFProjection;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Projection;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * An inner class delegated to add Coordinates to the output NetCDF file, as well as setting
 * additional attributes and variables needed to properly represent the related
 * CoordinateReferenceSystem.
 *
 * <p>Note that NetCDF files write is made in 2 steps: 1) the data initialization (define mode) 2)
 * the data write
 *
 * <p>Therefore, the NetCDFCoordinates writer needs to be initialized first, through the {@link
 * #initialize2DCoordinatesDimensions(Map)}
 *
 * <p>Once all other elements of the NetCDF file have been initialized, the Coordinates need to be
 * written through the {@link #setCoordinateVariable(NetCDFDimensionMapping)} calls.
 *
 * @author Daniele Romagnoli, GeoSolutions
 */
class NetCDFCRSWriter {

    public static final Logger LOGGER = Logging.getLogger(NetCDFCRSWriter.class);

    /** the NetCDF CoordinateReferenceSystem holder */
    private NetCDFCoordinateReferenceSystemType netcdfCrsType;

    /** the NetCDF File writer */
    private NetcdfFileWriter writer;

    /**
     * A sample granule used to extract properties such as CoordinateReferenceSystem, Grid2World
     * transformation
     */
    private GridCoverage2D sampleGranule;

    /** A map to assign a Dimension Mapping to each coordinate */
    private Map<String, NetCDFDimensionMapping> coordinatesDimensions =
            new LinkedHashMap<String, NetCDFDimensionMapping>();

    /** The underlying CoordinateReferenceSystem */
    private CoordinateReferenceSystem crs;

    /** The Grid2World transformation, used to setup geoTransformation */
    private MathTransform transform;

    public NetCDFCRSWriter(NetcdfFileWriter writer, GridCoverage2D sampleGranule) {
        this.writer = writer;
        this.sampleGranule = sampleGranule;
        crs = sampleGranule.getCoordinateReferenceSystem();
        RenderedImage image = sampleGranule.getRenderedImage();

        // Depending on the operations involved in granule's creation
        // there might be some translates/crops (=> GridRange not starting from 0,0).
        // Let recreate the transformation to actual size and envelope.
        GridToEnvelopeMapper geMapper =
                new GridToEnvelopeMapper(
                        new GridEnvelope2D(
                                new Rectangle(0, 0, image.getWidth(), image.getHeight())),
                        sampleGranule.getEnvelope());
        transform = geMapper.createTransform();
        netcdfCrsType = NetCDFCoordinateReferenceSystemType.parseCRS(crs);
    }

    /**
     * Setup lat,lon dimension (or y,x) and related coordinates variable and add them to the
     * provided dimensionsManager
     */
    public Map<String, NetCDFDimensionMapping> initialize2DCoordinatesDimensions() {
        final RenderedImage image = sampleGranule.getRenderedImage();
        final Envelope envelope = sampleGranule.getEnvelope2D();

        AxisOrder axisOrder = CRS.getAxisOrder(crs);

        final int height = image.getHeight();
        final int width = image.getWidth();

        final AffineTransform at = (AffineTransform) transform;

        // Get the proper type of axisCoordinates depending on
        // the type of CoordinateReferenceSystem
        NetCDFCoordinate[] axisCoordinates = netcdfCrsType.getCoordinates(crs);

        // Setup resolutions and bbox extrema to populate regularly gridded coordinate data
        // TODO: investigate whether we need to do some Y axis flipping
        double xmin =
                (axisOrder == AxisOrder.NORTH_EAST)
                        ? envelope.getMinimum(1)
                        : envelope.getMinimum(0);
        double ymin =
                (axisOrder == AxisOrder.NORTH_EAST)
                        ? envelope.getMinimum(0)
                        : envelope.getMinimum(1);
        final double periodY =
                ((axisOrder == AxisOrder.NORTH_EAST)
                        ? XAffineTransform.getScaleX0(at)
                        : XAffineTransform.getScaleY0(at));
        final double periodX =
                (axisOrder == AxisOrder.NORTH_EAST)
                        ? XAffineTransform.getScaleY0(at)
                        : XAffineTransform.getScaleX0(at);

        // NetCDF coordinates are relative to center. Envelopes are relative to corners: apply an
        // half pixel shift to go back to center
        xmin += (periodX / 2d);
        ymin += (periodY / 2d);

        // -----------------------------------------
        // First coordinate (latitude/northing, ...)
        // -----------------------------------------
        addCoordinateVariable(axisCoordinates[0], height, ymin, periodY);

        // ------------------------------------------
        // Second coordinate (longitude/easting, ...)
        // ------------------------------------------
        addCoordinateVariable(axisCoordinates[1], width, xmin, periodX);
        return coordinatesDimensions;
    }

    /**
     * Add a coordinate variable to the dataset, along with the related dimension. Finally, add the
     * created dimension to the coordinates map
     */
    private void addCoordinateVariable(
            NetCDFCoordinate netCDFCoordinate, int size, double min, double period) {
        String dimensionName = netCDFCoordinate.getDimensionName();
        String standardName = netCDFCoordinate.getStandardName();

        // Create the dimension
        final Dimension dimension = writer.addDimension(null, dimensionName, size);
        final ArrayFloat dimensionData = new ArrayFloat(new int[] {size});
        final Index index = dimensionData.getIndex();

        // Create the related coordinate variable
        final Variable coordinateVariable =
                writer.addVariable(
                        null, netCDFCoordinate.getShortName(), DataType.FLOAT, dimensionName);
        writer.addVariableAttribute(
                coordinateVariable,
                new Attribute(NetCDFUtilities.LONG_NAME, netCDFCoordinate.getLongName()));
        writer.addVariableAttribute(
                coordinateVariable,
                new Attribute(NetCDFUtilities.UNITS, netCDFCoordinate.getUnits()));

        // Associate the standardName if defined
        if (standardName != null && !standardName.isEmpty()) {
            writer.addVariableAttribute(
                    coordinateVariable, new Attribute(NetCDFUtilities.STANDARD_NAME, standardName));
        }

        // Set the coordinate values
        for (int pos = 0; pos < size; pos++) {
            dimensionData.setFloat(index.set(pos), (float) (min + (pos * period)));
        }

        // Add the dimension mapping to the coordinates dimensions
        final NetCDFDimensionMapping dimensionMapper = new NetCDFDimensionMapping(dimensionName);
        dimensionMapper.setNetCDFDimension(dimension);
        dimensionMapper.setDimensionValues(new DimensionValuesArray(dimensionData));
        coordinatesDimensions.put(dimensionName, dimensionMapper);
    }

    /** Set the coordinate values for all the dimensions */
    void setCoordinateVariable(NetCDFDimensionMapping manager)
            throws IOException, InvalidRangeException {

        // Get the defined ucar dimension
        Dimension dimension = manager.getNetCDFDimension();
        if (dimension == null) {
            throw new IllegalArgumentException(
                    "No Dimension found for this manager: " + manager.getName());
        }

        // Get the associate coordinate variable for that dimension
        final String dimensionName = dimension.getShortName();
        Variable var = writer.findVariable(dimensionName);
        if (var == null) {
            throw new IllegalArgumentException(
                    "Unable to find the specified coordinate variable: " + dimensionName);
        }

        // Writing coordinate variable values
        writer.write(var, manager.getDimensionData(false, netcdfCrsType.getCoordinates()));

        // handle ranges
        DimensionBean coverageDimension = manager.getCoverageDimension();
        if (coverageDimension != null) { // 2D coords (lat,lon / x,y) may be null
            boolean isRange = coverageDimension.isRange();
            if (isRange) {
                var = writer.findVariable(dimensionName + NetCDFUtilities.BOUNDS_SUFFIX);
                writer.write(var, manager.getDimensionData(true, null));
            }
        }
    }

    /**
     * Add gridMapping variable for projected datasets.
     *
     * @param var the {@link Variable} where the mapping attribute needs to be appended
     */
    void initializeGridMapping(Variable var) {
        NetCDFProjection projection = netcdfCrsType.getNetCDFProjection();

        // CRS may be standard WGS84 or a projected one.
        // Add GridMapping name if projected
        if (projection != null) {
            String mappingName = projection.getName();
            if (var != null) {
                writer.addVariableAttribute(
                        var, new Attribute(NetCDFUtilities.GRID_MAPPING, mappingName));
            }

            // Add the mapping variable
            writer.addVariable(null, mappingName, DataType.CHAR, (String) null);
        }

        // update CRS information
        updateProjectionInformation(netcdfCrsType, writer, crs, transform);
    }

    /**
     * Add GeoReferencing information to the writer, starting from the CoordinateReferenceSystem and
     * the MathTransform
     */
    public void updateProjectionInformation(
            NetCDFCoordinateReferenceSystemType crsType,
            NetcdfFileWriter writer,
            CoordinateReferenceSystem crs,
            MathTransform transform) {
        NetCDFProjection projection = crsType.getNetCDFProjection();

        // Projection may be exposed as standard NetCDF CF GridMapping (if available)
        // as well as through SPATIAL_REF and GeoTransform attributes (GDAL way)
        if (projection != null) {
            String name = projection.getName();
            Variable var = writer.findVariable(name);
            setGridMappingVariableAttributes(writer, crs, var, projection);
            setGeoreferencingAttributes(writer, crs, transform, var);
        } else {
            addGlobalAttributes(writer, crs, transform);
        }
    }

    /** Setup proper projection information to the output NetCDF */
    private void setGridMappingVariableAttributes(
            NetcdfFileWriter writer,
            CoordinateReferenceSystem crs,
            Variable var,
            NetCDFProjection projection) {
        if (!(crs instanceof ProjectedCRS)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "The provided CRS is not a projected CRS\n"
                                + "No projection information needs to be added");
            }
            return;
        }
        Map<String, String> referencingToNetCDFParameters = projection.getParameters();
        ProjectedCRS projectedCRS = (ProjectedCRS) crs;
        Projection conversionFromBase = projectedCRS.getConversionFromBase();
        if (conversionFromBase != null) {

            // getting the list of parameters needed for the NetCDF mapping
            Set<String> keySet = referencingToNetCDFParameters.keySet();

            // getting the list of parameters from the GT Referencing Projection
            ParameterValueGroup values =
                    projection.getNetcdfParameters(conversionFromBase.getParameterValues());
            List<GeneralParameterValue> valuesList = values.values();

            // Set up NetCDF CF parameters to be written
            Map<String, List<Double>> parameterValues = new HashMap<String, List<Double>>();

            // Loop over the available conversion parameters
            for (GeneralParameterValue param : valuesList) {
                String code = param.getDescriptor().getName().getCode();

                // Check if one of the parameters is needed by NetCDF
                if (keySet.contains(code)) {

                    // Get the parameter value
                    Double value = ((ParameterValue) param).doubleValue();

                    // Get the related NetCDF CF parameter
                    updateParameterValues(
                            referencingToNetCDFParameters, code, value, parameterValues);
                }
            }

            // Setup projections attributes
            Set<String> paramKeys = parameterValues.keySet();
            for (String key : paramKeys) {
                List<Double> val = parameterValues.get(key);
                if (val.size() == 1) {
                    // Set attribute as single element
                    writer.addVariableAttribute(var, new Attribute(key, val.get(0)));
                } else {
                    // Set attribute as List element (typical case is
                    // StandardParallel with SP1 and SP2)
                    writer.addVariableAttribute(var, new Attribute(key, val));
                }
            }
        }
        writer.addVariableAttribute(
                var, new Attribute(NetCDFUtilities.GRID_MAPPING_NAME, projection.getName()));
    }

    private void updateParameterValues(
            Map<String, String> referencingToNetCDFParameters,
            String code,
            Double value,
            Map<String, List<Double>> parameterValues) {
        String mappedKey = referencingToNetCDFParameters.get(code);
        if (!mappedKey.contains(NetCDFProjection.PARAMS_SEPARATOR)) {
            updateParam(mappedKey, parameterValues, value);
        } else {
            String[] keys = mappedKey.split(NetCDFProjection.PARAMS_SEPARATOR);
            // Assign the same value to multiple params.
            // As an instance, Lambert_conformal_conic_1sp use same values
            // for standard_parallel and latitude_of_projection_origin
            for (String key : keys) {
                updateParam(key, parameterValues, value);
            }
        }
    }

    private void updateParam(
            String mappedKey, Map<String, List<Double>> parameterValues, Double value) {

        // Make sure to proper deal with Number and Arrays
        // Standard Parallels are provided as a single attribute with
        // 2 values
        if (parameterValues.containsKey(mappedKey)) {
            List<Double> paramValues = parameterValues.get(mappedKey);
            paramValues.add(value);
        } else {
            List<Double> paramValues = new ArrayList<Double>(1);
            paramValues.add(value);
            parameterValues.put(mappedKey, paramValues);
        }
    }

    /**
     * Add GeoReferencing global attributes (GDAL's spatial_ref and GeoTransform). They will be used
     * for datasets with unsupported NetCDF CF projection.
     */
    private void addGlobalAttributes(
            NetcdfFileWriter writer, CoordinateReferenceSystem crs, MathTransform transform) {
        writer.addGroupAttribute(null, getSpatialRefAttribute(crs));
        writer.addGroupAttribute(null, getGeoTransformAttribute(transform));
    }

    /** Add the gridMapping attribute */
    private void setGeoreferencingAttributes(
            NetcdfFileWriter writer,
            CoordinateReferenceSystem crs,
            MathTransform transform,
            Variable var) {

        // Adding GDAL Attributes spatial_ref and GeoTransform
        writer.addVariableAttribute(var, getSpatialRefAttribute(crs));
        writer.addVariableAttribute(var, getGeoTransformAttribute(transform));
    }

    /**
     * Setup a {@link NetCDFUtilities#SPATIAL_REF} attribute on top of the CoordinateReferenceSystem
     *
     * @param crs the {@link CoordinateReferenceSystem} instance
     * @return the {@link Attribute} containing the spatial_ref attribute
     */
    private Attribute getSpatialRefAttribute(CoordinateReferenceSystem crs) {
        String wkt = crs.toWKT().replace("\r\n", "").replace("  ", " ").replace("  ", " ");
        return new Attribute(NetCDFUtilities.SPATIAL_REF, wkt);
    }

    /**
     * Setup a {@link NetCDFUtilities#GEO_TRANSFORM} attribute on top of the MathTransform
     *
     * @param transform the grid2world geoTransformation
     * @return the {@link Attribute} containing the GeotTransform attribute
     */
    private Attribute getGeoTransformAttribute(MathTransform transform) {
        AffineTransform at = (AffineTransform) transform;
        String geoTransform =
                Double.toString(at.getTranslateX())
                        + " "
                        + Double.toString(at.getScaleX())
                        + " "
                        + Double.toString(at.getShearX())
                        + " "
                        + Double.toString(at.getTranslateY())
                        + " "
                        + Double.toString(at.getShearY())
                        + " "
                        + Double.toString(at.getScaleY());
        return new Attribute(NetCDFUtilities.GEO_TRANSFORM, geoTransform);
    }

    public Set<String> getCoordinatesDimensionNames() {
        return coordinatesDimensions.keySet();
    }
}
