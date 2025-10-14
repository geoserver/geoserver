/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.RandomIterFactory;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geoserver.wcs.responses.NetCDFDimensionsManager.NetCDFDimensionMapping;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.web.netcdf.DataPacking;
import org.geoserver.web.netcdf.DataPacking.DataPacker;
import org.geoserver.web.netcdf.DataPacking.DataStats;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.imageio.netcdf.NetCDFUnitFormat;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * A class which takes care of initializing NetCDF dimension from coverages dimension, variables, values for the NetCDF
 * output file and finally write them when invoking the write method.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class DefaultNetCDFEncoder extends AbstractNetCDFEncoder {

    /** The user supplied variableName */
    private String variableName;

    /** The user supplied unit of measure */
    private String variableUoM;

    /**
     * The unitConverter to be used to convert the pixel values from the input unit (the one coming from the original
     * coverage) to output unit (the one specified by the user). As an instance, we may work against a
     * sea_surface_temperature coverage having temperature in celsius whilst we want to store it back as a
     * sea_surface_temperature in kelvin. In that case the machinery will setup a not-null unitConverter to be used at
     * time of data writing.
     */
    private UnitConverter unitConverter;

    private double noDataValue;

    private DataStats stats = null;

    private DataPacker dataPacker;

    /**
     * {@link DefaultNetCDFEncoder} constructor.
     *
     * @param granuleStack the granule stack to be written
     * @param file an output file
     * @param encodingParameters customized encoding params
     */
    public DefaultNetCDFEncoder(
            GranuleStack granuleStack, File file, Map<String, String> encodingParameters, String outputFormat)
            throws IOException {
        super(granuleStack, file, encodingParameters, outputFormat);
    }

    @Override
    protected void initializeFromSettings(NetCDFLayerSettingsContainer settings) {
        super.initializeFromSettings(settings);
        variableName = settings.getLayerName();
        variableUoM = settings.getLayerUOM();
    }

    /** Initialize the NetCDF variables on this writer */
    @Override
    protected void initializeVariables() {

        // group the dimensions to be added to the variable
        List<Dimension> netCDFDimensions = new LinkedList<>();
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
        Variable.Builder varb = writerb.addVariable(coverageName, varDataType, netCDFDimensions);
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
            varb.addAttribute(new Attribute(NetCDFUtilities.LONG_NAME, variableName));
        }

        // Adding Units
        if (varb.getAttributeContainer().findAttribute(NetCDFUtilities.UNITS) == null) {
            String unit = null;
            boolean hasDefinedUoM = (variableUoM != null && !variableUoM.isEmpty());
            if (hasDefinedUoM) {
                unit = variableUoM;
            } else if (inputUoM != null) {
                unit = inputUoM.toString();
            }
            if (unit != null) {
                varb.addAttribute(new Attribute(NetCDFUtilities.UNITS, unit));
            }
            if (inputUoM != null && hasDefinedUoM) {
                try {
                    Unit<?> outputUoM = NetCDFUnitFormat.getInstance().parse(variableUoM);
                    if (outputUoM != null && !inputUoM.equals(outputUoM)) {
                        if (!inputUoM.isCompatible(outputUoM)) {
                            if (LOGGER.isLoggable(Level.WARNING)) {
                                LOGGER.warning("input unit "
                                        + inputUoM
                                        + " and output unit "
                                        + outputUoM
                                        + " are incompatible.\nNo unit conversion will be performed");
                            }
                        } else {
                            unitConverter = getConverter(inputUoM, outputUoM);
                        }
                    }
                } catch (UnconvertibleException ce) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe("Unable to create a converter for the specified unit: "
                                + variableUoM
                                + "\nNo unit conversion will be performed");
                    }
                } catch (IllegalArgumentException e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe("Unable to parse the specified unit: "
                                + variableUoM
                                + "\nNo unit conversion will be performed");
                    }
                }
            }
        }
        // Adding standard name if name and units are cf-compliant
        if (checkCompliant(varb)) {
            varb.addAttribute(new Attribute(NetCDFUtilities.STANDARD_NAME, variableName));
        }
        if (dataPacking != DataPacking.NONE) {
            // Get the dimension values from the coverage and put them on the mapping
            // Note that using tree set allows to respect the ordering when writing
            // down the NetCDF dimensions
            this.stats = new DataPacking.DataStats();
            for (GridCoverage2D coverage : this.granuleStack.getGranules()) {
                updateDimensionValues(coverage);
                if (!(dataPacking == DataPacking.NONE)) {
                    collectStats(coverage, Arrays.asList(this.stats));
                }
            }

            DataStats updatedStats = stats;
            if (unitConverter != null) {
                // Update stats by applying unitConversion
                updatedStats.setMax(unitConverter.convert(updatedStats.getMax()));
                updatedStats.setMin(unitConverter.convert(updatedStats.getMin()));
            }
            dataPacker = dataPacking.getDataPacker(updatedStats);
            varb.addAttribute(new Attribute(DataPacking.ADD_OFFSET, dataPacker.getOffset()));
            varb.addAttribute(new Attribute(DataPacking.SCALE_FACTOR, dataPacker.getScale()));
        }

        if (noDataSet) {
            Number noData = dataPacker != null ? dataPacker.getReservedValue() : noDataValue;
            varb.addAttribute(
                    new Attribute(NetCDFUtilities.FILL_VALUE, NetCDFUtilities.transcodeNumber(varDataType, noData)));
        }

        // Initialize the gridMapping part of the variable
        crsWriter.initializeGridMapping(varb);

        // Copy from source NetCDF
        if (copyAttributes || extraVariables != null && !extraVariables.isEmpty()) {
            try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                if (source != null) {
                    if (copyAttributes) {
                        Variable sourceVar =
                                source.findVariable(sampleGranule.getName().toString());
                        if (sourceVar == null) {
                            LOGGER.info(String.format(
                                    "Could not copy attributes because " + "variable '%s' not found in NetCDF/GRIB %s",
                                    sampleGranule.getName().toString(), source.getLocation()));
                        } else {
                            copyAttributes(sourceVar, varb, dataPacking);
                        }
                    }

                    addExtraVariables(source);
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe("Failed to copy from source NetCDF: " + e.getMessage());
                }
            }
        }

        addSettingsVariableAttributes(varb);
    }

    /** Makes conversion compile when the two units are of an unknown quantity */
    @SuppressWarnings("unchecked")
    private UnitConverter getConverter(Unit<?> inputUoM, Unit<?> outputUoM) {
        return inputUoM.getConverterTo((Unit) outputUoM);
    }

    /** Set the variables values */
    @Override
    protected void writeDataValues() throws IOException, InvalidRangeException {
        // Initialize dimensions sizes
        final int numDimensions = dimensionsManager.getNumDimensions();
        final int[] dimSize = new int[numDimensions];
        final String[] dimName = new String[numDimensions];
        int iDim = 0;
        for (NetCDFDimensionMapping dimension : dimensionsManager.getDimensions()) {
            dimSize[iDim] = dimension.getDimensionValues().getSize();
            dimName[iDim] = dimension.getNetCDFDimension().getShortName();
            iDim++;
        }
        String name =
                variableName != null ? variableName : sampleGranule.getName().toString();
        final Variable var = writer.findVariable(name);
        if (var == null) {
            throw new IllegalArgumentException("The requested variable doesn't exists: " + name);
        }

        List<ExtraVariableRecord> nonscalarExtraVariables = writeNonScalarExtraVariables(dimName);

        // Get the data type for a sample image (All granules of the same coverage will use
        // the same sample model
        final int imageDataType =
                sampleGranule.getRenderedImage().getSampleModel().getDataType();
        final DataType netCDFDataType = var.getDataType();
        final Array matrix = NetCDFUtilities.getArray(dimSize, netCDFDataType);

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

            final Index matrixIndex = matrix.getIndex();
            final int[] indexing = new int[numDimensions];

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
                                if (!record.writtenIndices.contains(indexing[record.dimensionIndex])) {
                                    writer.write(
                                            writer.findVariable(record.extraVariable.getOutput()),
                                            new int[] {indexing[record.dimensionIndex]},
                                            source.findVariable(record.extraVariable.getSource())
                                                    .read()
                                                    .reshape(new int[] {1}));
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
                                    setPixel(
                                            k,
                                            j,
                                            NetCDFUtilities.transcodeImageDataType(imageDataType),
                                            netCDFDataType,
                                            data,
                                            matrix,
                                            matrixIndex,
                                            dataPacker,
                                            noDataValue,
                                            unitConverter,
                                            0);
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

    @Override
    protected boolean checkCompliant(Variable.Builder var) {
        // Check the layer name
        if (variableName == null || variableName.isEmpty()) {
            // Wrong Layer name
            return false;
        }

        return super.checkCompliant(var);
    }
}
