/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.BandSetting;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.VariableAttribute;
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
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * A class which takes care of initializing NetCDF dimensions from coverage dimensions, variables, values for the NetCDF
 * output file and finally writes them when invoking the write method.
 *
 * <p>For single-band coverages — the historical and most common case — this encoder writes one output variable named
 * after the layer (or the {@code layerName} from {@link NetCDFLayerSettingsContainer}, if set), preserving the
 * pre-existing behavior bit-for-bit.
 *
 * <p>For coverages with more than one sample dimension (typically built via WCS {@code COVERAGE_VIEW} with
 * {@code BAND_SELECT}), this encoder now writes one output variable per band. The output variable name is taken from
 * (in precedence order):
 *
 * <ol>
 *   <li>the corresponding {@link BandSetting#getName() bandSettings[i].name} entry from the layer's NetCDF Output
 *       configuration, if set;
 *   <li>the source band's {@link GridSampleDimension#getDescription() sample dimension description}, which equals the
 *       {@code <definition>} value when the source coverage is a {@code COVERAGE_VIEW} with {@code BAND_SELECT}
 *       entries;
 *   <li>as a last-resort fallback, {@code <coverageName>_<bandIndex>}.
 * </ol>
 *
 * <p>Per-band unit of measure, no-data, data packing, and variable attributes are tracked individually so that each
 * output variable carries the metadata of its source band. The container-level
 * {@link NetCDFSettingsContainer#getVariableAttributes() variableAttributes} list still applies to <em>every</em>
 * output variable; per-band {@link BandSetting#getVariableAttributes()} entries are additive and take precedence on key
 * collisions.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class DefaultNetCDFEncoder extends AbstractNetCDFEncoder {

    /** The user-supplied variable name (only used as a backwards-compatible override for single-band coverages). */
    private String variableName;

    /** The user-supplied unit of measure (only used as a backwards-compatible override for single-band coverages). */
    private String variableUoM;

    /**
     * Per-band settings extracted from the layer's NetCDF Output configuration. May be {@code null} or empty, in which
     * case multi-band defaults are derived from the source band sample dimensions.
     */
    private List<BandSetting> bandSettings;

    /**
     * One {@link BandVariable} per source band; populated by {@link #initializeVariables()} and consumed by
     * {@link #writeDataValues()}. For single-band coverages this array has length 1 and the encoder writes a single
     * output variable, matching the pre-multi-band behavior.
     */
    private BandVariable[] bandVariables;

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
        bandSettings = settings.getBandSettings();
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
        int dataType = sampleGranule.getRenderedImage().getSampleModel().getDataType();
        DataType varDataType = getDataType(dataType);
        GridSampleDimension[] sampleDimensions = sampleGranule.getSampleDimensions();

        // Allocate one BandVariable per source band. Single-band coverages flow through this path with a single
        // entry whose name resolves to either `variableName` (legacy `layerName` setting) or the coverage name —
        // identical to the pre-multi-band behavior.
        int bandCount = (sampleDimensions == null || sampleDimensions.length == 0) ? 1 : sampleDimensions.length;
        bandVariables = new BandVariable[bandCount];
        for (int i = 0; i < bandCount; i++) {
            bandVariables[i] = new BandVariable();
            bandVariables[i].name = resolveBandVariableName(coverageName, sampleDimensions, i, bandCount);
        }

        // Pre-allocate per-band stats containers when data packing is enabled; populated below as we iterate the
        // granule stack.
        List<DataStats> bandStatistics = null;
        if (dataPacking != DataPacking.NONE) {
            bandStatistics = new ArrayList<>(bandCount);
            for (int i = 0; i < bandCount; i++) {
                bandStatistics.add(new DataStats());
            }
        }

        // Configure each band's output variable: dims, no-data, units, dataPacking, gridMapping, copied attributes,
        // and the per-band variable attributes from the layer settings.
        for (int i = 0; i < bandCount; i++) {
            initializeBandVariable(bandVariables[i], i, sampleDimensions, netCDFDimensions, varDataType, bandCount);
        }

        // Compute data packing stats once across all granules and bands; the per-band scale_factor / add_offset
        // attributes are then attached individually below.
        if (bandStatistics != null) {
            for (GridCoverage2D coverage : granuleStack.getGranules()) {
                updateDimensionValues(coverage);
                collectStats(coverage, bandStatistics);
            }
            applyDataPacking(bandStatistics);
        }

        // Add the _FillValue / missing_value attribute to each band that has a known no-data sentinel — using the
        // packed reserved value when data packing is in effect, the raw no-data value otherwise.
        for (BandVariable bv : bandVariables) {
            if (bv.noDataSet) {
                Number noData = bv.dataPacker != null ? bv.dataPacker.getReservedValue() : bv.noDataValue;
                bv.builder.addAttribute(new Attribute(
                        NetCDFUtilities.FILL_VALUE, NetCDFUtilities.transcodeNumber(varDataType, noData)));
            }
        }

        // Initialize the gridMapping part of every band variable. The grid mapping reference is the same for all
        // bands — they share the same horizontal CRS by construction (a multi-band COVERAGE_VIEW collapses bands
        // sharing the same grid).
        for (BandVariable bv : bandVariables) {
            crsWriter.initializeGridMapping(bv.builder);
        }

        // Copy attributes from the source NetCDF (when requested) and add any extra variables. We open the source
        // dataset once per encode, looking up each band's source variable by:
        //   - its sample dimension description (the COVERAGE_VIEW input band name) when multi-band, OR
        //   - the coverage name itself (legacy single-band behavior).
        if (copyAttributes || (extraVariables != null && !extraVariables.isEmpty())) {
            try (NetcdfDataset source = getSourceNetcdfDataset(sampleGranule)) {
                if (source != null) {
                    if (copyAttributes) {
                        for (int i = 0; i < bandCount; i++) {
                            String sourceVarName = sourceVariableNameFor(coverageName, sampleDimensions, i, bandCount);
                            Variable sourceVar = source.findVariable(sourceVarName);
                            if (sourceVar == null) {
                                LOGGER.info(String.format(
                                        "Could not copy attributes because variable '%s' not found in NetCDF/GRIB %s",
                                        sourceVarName, source.getLocation()));
                            } else {
                                copyAttributes(sourceVar, bandVariables[i].builder, dataPacking);
                            }
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

        // Apply the global variableAttributes list to every band, then layer the per-band variableAttributes on top
        // (additive; per-band entries replace global ones on key collision).
        for (int i = 0; i < bandCount; i++) {
            addSettingsVariableAttributes(bandVariables[i].builder);
            applyPerBandVariableAttributes(bandVariables[i].builder, i);
        }
    }

    /** Resolve the output variable name for a single band, applying the precedence rules documented on the class. */
    private String resolveBandVariableName(
            String coverageName, GridSampleDimension[] sampleDimensions, int bandIdx, int bandCount) {
        // Precedence 1: per-band setting from the layer's NetCDF Output configuration.
        BandSetting bs = bandSettingFor(bandIdx);
        if (bs != null && bs.getName() != null && !bs.getName().isEmpty()) {
            return bs.getName();
        }
        if (bandCount == 1) {
            // Single-band: preserve the historical `variableName ?? coverageName` behavior so existing layer
            // configurations and tests are unaffected.
            return (variableName != null && !variableName.isEmpty()) ? variableName : coverageName;
        }
        // Precedence 2: sample dim description (e.g. COVERAGE_VIEW BAND_SELECT <definition>).
        if (sampleDimensions != null
                && bandIdx < sampleDimensions.length
                && sampleDimensions[bandIdx].getDescription() != null) {
            String desc = sampleDimensions[bandIdx].getDescription().toString();
            if (!desc.isEmpty()) {
                return desc;
            }
        }
        // Precedence 3: synthetic fallback.
        return coverageName + "_" + bandIdx;
    }

    /**
     * Resolve the source NetCDF variable name to read attributes from for the given output band. For multi-band
     * coverages this is the band's sample dim description (the upstream variable name); for single-band coverages (the
     * legacy path) this is the coverage name itself.
     */
    private String sourceVariableNameFor(
            String coverageName, GridSampleDimension[] sampleDimensions, int bandIdx, int bandCount) {
        if (bandCount > 1
                && sampleDimensions != null
                && bandIdx < sampleDimensions.length
                && sampleDimensions[bandIdx].getDescription() != null) {
            String desc = sampleDimensions[bandIdx].getDescription().toString();
            if (!desc.isEmpty()) {
                return desc;
            }
        }
        return coverageName;
    }

    /** Return the per-band setting for the requested band index, or {@code null} if no override is configured. */
    private BandSetting bandSettingFor(int bandIdx) {
        if (bandSettings == null || bandIdx >= bandSettings.size()) {
            return null;
        }
        return bandSettings.get(bandIdx);
    }

    /** Set up a single band's output variable: name, dims, no-data, units, unit converter, long_name, standard_name. */
    private void initializeBandVariable(
            BandVariable bv,
            int bandIdx,
            GridSampleDimension[] sampleDimensions,
            List<Dimension> netCDFDimensions,
            DataType varDataType,
            int bandCount) {

        bv.builder = writerb.addVariable(bv.name, varDataType, netCDFDimensions);

        // Resolve no-data and input UoM from the matching sample dimension (or the granule's no-data property as a
        // last resort, the same fallback the pre-multi-band code used).
        bv.noDataValue = Double.NaN;
        bv.noDataSet = false;
        Unit<?> inputUoM = null;
        if (sampleDimensions != null && bandIdx < sampleDimensions.length) {
            GridSampleDimension sd = sampleDimensions[bandIdx];
            inputUoM = sd.getUnits();
            double[] noData = sd.getNoDataValues();
            if (noData != null && noData.length > 0) {
                bv.noDataValue = noData[0];
                bv.noDataSet = true;
            }
        }
        if (!bv.noDataSet) {
            NoDataContainer ndc = CoverageUtilities.getNoDataProperty(sampleGranule);
            if (ndc != null) {
                bv.noDataValue = ndc.getAsSingleValue();
                bv.noDataSet = true;
            }
        }
        // long_name: only emitted when the user has explicitly named the variable (single-band variableName, or a
        // per-band BandSetting.name override). Auto-derived names from sample dims don't get a long_name attribute
        // — they typically already carry CF metadata via copyAttributes.
        String userSuppliedName = userSuppliedNameFor(bandIdx, bandCount);
        if (userSuppliedName != null) {
            bv.builder.addAttribute(new Attribute(NetCDFUtilities.LONG_NAME, userSuppliedName));
        }

        // Units: per-band BandSetting.uom > legacy variableUoM (single-band) > input sample dim unit.
        if (bv.builder.getAttributeContainer().findAttribute(NetCDFUtilities.UNITS) == null) {
            String overrideUom = perBandUomOverride(bandIdx, bandCount);
            String unit = null;
            if (overrideUom != null && !overrideUom.isEmpty()) {
                unit = overrideUom;
            } else if (inputUoM != null) {
                unit = inputUoM.toString();
            }
            if (unit != null) {
                bv.builder.addAttribute(new Attribute(NetCDFUtilities.UNITS, unit));
            }
            // Set up an input→output unit converter when the user explicitly declared an output uom that differs
            // from the input — values are converted at write time in setPixel via the band's converter.
            if (inputUoM != null && overrideUom != null && !overrideUom.isEmpty()) {
                bv.unitConverter = buildUnitConverter(inputUoM, overrideUom);
            }
        }

        // standard_name: only set when the resolved variable name + units are CF-compliant. The check parses the
        // CF standard names table and matches the variable name against the canonical list and aliases.
        if (checkBandCompliant(bv)) {
            bv.builder.addAttribute(new Attribute(NetCDFUtilities.STANDARD_NAME, bv.name));
        }

        // dataPacker is finalized after we collect global stats below.
    }

    /** True if the user has explicitly named this band (vs the encoder auto-deriving from the sample dim). */
    private String userSuppliedNameFor(int bandIdx, int bandCount) {
        BandSetting bs = bandSettingFor(bandIdx);
        if (bs != null && bs.getName() != null && !bs.getName().isEmpty()) {
            return bs.getName();
        }
        if (bandCount == 1 && variableName != null && !variableName.isEmpty()) {
            return variableName;
        }
        return null;
    }

    /** Resolve the unit-of-measure override for this band, considering per-band and legacy single-band settings. */
    private String perBandUomOverride(int bandIdx, int bandCount) {
        BandSetting bs = bandSettingFor(bandIdx);
        if (bs != null && bs.getUom() != null && !bs.getUom().isEmpty()) {
            return bs.getUom();
        }
        if (bandCount == 1 && variableUoM != null && !variableUoM.isEmpty()) {
            return variableUoM;
        }
        return null;
    }

    /**
     * Build a {@link UnitConverter} from the input sample dimension unit to the user-declared output unit. Returns
     * {@code null} when no conversion is needed or when the units are incompatible (a warning is logged in that case so
     * the user can fix the configuration).
     */
    @SuppressWarnings("unchecked")
    private UnitConverter buildUnitConverter(Unit<?> inputUoM, String outputUomStr) {
        try {
            Unit<?> outputUoM = NetCDFUnitFormat.getInstance().parse(outputUomStr);
            if (outputUoM == null || inputUoM.equals(outputUoM)) {
                return null;
            }
            if (!inputUoM.isCompatible(outputUoM)) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("input unit "
                            + inputUoM
                            + " and output unit "
                            + outputUoM
                            + " are incompatible.\nNo unit conversion will be performed");
                }
                return null;
            }
            return inputUoM.getConverterTo((Unit) outputUoM);
        } catch (UnconvertibleException ce) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("Unable to create a converter for the specified unit: "
                        + outputUomStr
                        + "\nNo unit conversion will be performed");
            }
        } catch (IllegalArgumentException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("Unable to parse the specified unit: " + outputUomStr
                        + "\nNo unit conversion will be performed");
            }
        }
        return null;
    }

    /** Per-band variant of {@link #checkCompliant(Variable.Builder)} that operates on a {@link BandVariable}. */
    private boolean checkBandCompliant(BandVariable bv) {
        // Only auto-derived names go through CF compliance check on multi-band coverages: when the user supplied
        // an explicit name (single-band layerName, or per-band bandSettings[i].name) we want the compliance check
        // to apply to that name. Either way, the variable's resolved name is what matters here.
        if (bv.name == null || bv.name.isEmpty()) {
            return false;
        }
        // Reuse the parent's compliance check, which inspects the Variable.Builder's name + units attribute.
        return super.checkCompliant(bv.builder);
    }

    /**
     * Apply the {@code dataPacking} settings to every band: pick the {@link DataPacker} from the band's collected stats
     * (after applying any unit conversion), then attach the {@code add_offset} and {@code scale_factor} attributes to
     * the corresponding variable builder.
     */
    private void applyDataPacking(List<DataStats> bandStatistics) {
        for (int i = 0; i < bandVariables.length; i++) {
            BandVariable bv = bandVariables[i];
            DataStats stats = bandStatistics.get(i);
            if (bv.unitConverter != null) {
                stats.setMax(bv.unitConverter.convert(stats.getMax()));
                stats.setMin(bv.unitConverter.convert(stats.getMin()));
            }
            bv.dataPacker = dataPacking.getDataPacker(stats);
            bv.builder.addAttribute(new Attribute(DataPacking.ADD_OFFSET, bv.dataPacker.getOffset()));
            bv.builder.addAttribute(new Attribute(DataPacking.SCALE_FACTOR, bv.dataPacker.getScale()));
        }
    }

    /**
     * Apply the per-band {@link VariableAttribute}s on top of the container-level ones already added by
     * {@link #addSettingsVariableAttributes(Variable.Builder)} — per-band entries take precedence on key collisions.
     */
    private void applyPerBandVariableAttributes(Variable.Builder varb, int bandIdx) {
        BandSetting bs = bandSettingFor(bandIdx);
        if (bs == null) {
            return;
        }
        List<VariableAttribute> perBand = bs.getVariableAttributes();
        if (perBand == null || perBand.isEmpty()) {
            return;
        }
        AttributeContainerMutable attrs = varb.getAttributeContainer();
        for (VariableAttribute att : perBand) {
            attrs.removeAttribute(att.getKey());
            varb.addAttribute(buildAttribute(att.getKey(), att.getValue()));
        }
    }

    /** Set the variables values */
    @Override
    protected void writeDataValues() throws IOException, InvalidRangeException {
        // Initialize dimension sizes
        final int numDimensions = dimensionsManager.getNumDimensions();
        final int[] dimSize = new int[numDimensions];
        final String[] dimName = new String[numDimensions];
        int iDim = 0;
        for (NetCDFDimensionMapping dimension : dimensionsManager.getDimensions()) {
            dimSize[iDim] = dimension.getDimensionValues().getSize();
            dimName[iDim] = dimension.getNetCDFDimension().getShortName();
            iDim++;
        }

        // Resolve every band's output Variable + allocate its data buffer (the buffer covers all dimensions, matching
        // the pre-multi-band single-write strategy: one full-shape Array per band, written once after the granule
        // loop fills in every cell).
        for (BandVariable bv : bandVariables) {
            bv.var = writer.findVariable(bv.name);
            if (bv.var == null) {
                throw new IllegalArgumentException("The requested variable doesn't exists: " + bv.name);
            }
            bv.netCDFDataType = bv.var.getDataType();
            bv.matrix = NetCDFUtilities.getArray(dimSize, bv.netCDFDataType);
        }

        List<ExtraVariableRecord> nonscalarExtraVariables = writeNonScalarExtraVariables(dimName);

        // Get the data type for a sample image (all granules of the same coverage share the sample model).
        final int imageDataType =
                sampleGranule.getRenderedImage().getSampleModel().getDataType();
        final DataType sourceDataType = NetCDFUtilities.transcodeImageDataType(imageDataType);

        // Loop over all granules
        for (GridCoverage2D gridCoverage : granuleStack.getGranules()) {
            final RenderedImage ri = gridCoverage.getRenderedImage();

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

            final int[] indexing = new int[numDimensions];

            // Update the NetCDF array indexing to set values for a specific 2D slice
            updateIndexing(indexing, gridCoverage);

            // copy non-scalar extra variable data (single instance per source granule, not per band).
            writeNonScalarPerGranule(gridCoverage, nonscalarExtraVariables, indexing);

            // Fill data matrix for every band, single tile-walking pass — the band-inner loop reads the per-band
            // pixel from the same RandomIter, so we don't pay the cost of restarting per band.
            final RandomIter data = RandomIterFactory.create(ri, null);
            try {
                fillBandMatrices(
                        data,
                        sourceDataType,
                        height,
                        minX,
                        minY,
                        maxX,
                        maxY,
                        tileWidth,
                        tileHeight,
                        minTileX,
                        minTileY,
                        maxTileX,
                        maxTileY,
                        indexing,
                        numDimensions);
            } finally {
                data.done();
            }
        }

        // Write each band's matrix to its variable (single write per band — same number of writes as before for
        // single-band coverages, multiplied by the band count for multi-band).
        for (BandVariable bv : bandVariables) {
            writer.write(bv.var, bv.matrix);
        }
        writer.flush();
    }

    /** Copy non-scalar extra variable values for the current granule, mirroring the pre-multi-band logic. */
    private void writeNonScalarPerGranule(
            GridCoverage2D gridCoverage, List<ExtraVariableRecord> nonscalarExtraVariables, int[] indexing)
            throws IOException, InvalidRangeException {
        if (nonscalarExtraVariables.isEmpty()) {
            return;
        }
        // Before opening the source NetCDF/GRIB, see if any record requires data from it; we might be iterating
        // over many time/elevation/custom dimensions but have granules with sources in common and want to avoid
        // unnecessary opening of source NetCDF/GRIB. Only the first matching data value is used.
        boolean needSource = false;
        for (ExtraVariableRecord record : nonscalarExtraVariables) {
            if (!record.writtenIndices.contains(indexing[record.dimensionIndex])) {
                needSource = true;
                break;
            }
        }
        if (!needSource) {
            return;
        }
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

    /** Tile-and-pixel walk that fills every band's matrix from the current granule's {@link RandomIter}. */
    private void fillBandMatrices(
            RandomIter data,
            DataType sourceDataType,
            int height,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int tileWidth,
            int tileHeight,
            int minTileX,
            int minTileY,
            int maxTileX,
            int maxTileY,
            int[] indexing,
            int numDimensions) {
        for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                for (int trow = 0; trow < tileHeight; trow++) {
                    int j = (tileY * tileHeight) + trow;
                    if (j < minY || j > maxY) {
                        continue;
                    }
                    for (int tcol = 0; tcol < tileWidth; tcol++) {
                        int col = (tileX * tileWidth) + tcol;
                        if (col < minX || col > maxX) {
                            continue;
                        }
                        int yPos = height - j + minY - 1;
                        indexing[numDimensions - 1] = col - minX;
                        indexing[numDimensions - 2] = yPos;
                        for (int bandIdx = 0; bandIdx < bandVariables.length; bandIdx++) {
                            BandVariable bv = bandVariables[bandIdx];
                            Index matrixIndex = bv.matrix.getIndex();
                            matrixIndex.set(indexing);
                            setPixel(
                                    col,
                                    j,
                                    sourceDataType,
                                    bv.netCDFDataType,
                                    data,
                                    bv.matrix,
                                    matrixIndex,
                                    bv.dataPacker,
                                    bv.noDataValue,
                                    bv.unitConverter,
                                    bandIdx);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean checkCompliant(Variable.Builder var) {
        // Single-band legacy path: existing behavior is to require a non-empty layerName before stamping a
        // standard_name attribute. Per-band callers go through checkBandCompliant() above.
        if (variableName == null || variableName.isEmpty()) {
            return false;
        }
        return super.checkCompliant(var);
    }

    /**
     * Per-band write state and metadata. One instance per source band, populated by {@link #initializeVariables()} and
     * consumed by {@link #writeDataValues()}.
     */
    private static class BandVariable {

        /** Output variable name (resolved by {@link #resolveBandVariableName}). */
        private String name;

        /** The Builder used during initialization to attach attributes. */
        private Variable.Builder builder;

        /** The resolved {@link Variable} reference once the writer has been built. */
        private Variable var;

        /** Cached NetCDF data type, set after the writer is built. */
        private DataType netCDFDataType;

        /** Per-band data buffer; allocated at write time, then filled cell-by-cell across all granules. */
        private Array matrix;

        /** No-data sentinel value for this band, sourced from its sample dimension or the granule's noData prop. */
        private double noDataValue;

        /** Whether {@link #noDataValue} was explicitly resolved for this band. */
        private boolean noDataSet;

        /** Optional input → output unit converter applied to every pixel. */
        private UnitConverter unitConverter;

        /** Per-band data packer (when {@code dataPacking != NONE}); built after global stats are collected. */
        private DataPacker dataPacker;
    }
}
