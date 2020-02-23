/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.DEFAULT_BOOLEAN_VALUE;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.DEFAULT_NUMERICAL_VALUE;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.DEFAULT_STRING_VALUE;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.FIXED_TABLE_FIXED_NUM_ROWS;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.FIXED_TABLE_NUM_COLS;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.FIXED_TABLE_NUM_ROWS;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.MAX_NUMERICAL_VALUE;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.MIN_NUMERICAL_VALUE;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.MULTIPLE_INPUT_TYPE;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.NUMERICAL_VALUE_TYPE;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.PARAMETER_MANDATORY;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.PARENT_PARAMETER_NAME;
import static org.geoserver.wps.sextante.SextanteProcessFactoryConstants.SHAPE_TYPE;

import org.locationtech.jts.geom.Envelope;
import es.unex.sextante.additionalInfo.AdditionalInfo;
import es.unex.sextante.additionalInfo.AdditionalInfoBoolean;
import es.unex.sextante.additionalInfo.AdditionalInfoFixedTable;
import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoRasterLayer;
import es.unex.sextante.additionalInfo.AdditionalInfoString;
import es.unex.sextante.additionalInfo.AdditionalInfoTableField;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.NullParameterAdditionalInfoException;
import es.unex.sextante.outputs.Output;
import es.unex.sextante.outputs.OutputRasterLayer;
import es.unex.sextante.outputs.OutputTable;
import es.unex.sextante.outputs.OutputText;
import es.unex.sextante.outputs.OutputVectorLayer;
import es.unex.sextante.parameters.ParameterBoolean;
import es.unex.sextante.parameters.ParameterFixedTable;
import es.unex.sextante.parameters.ParameterMultipleInput;
import es.unex.sextante.parameters.ParameterNumericalValue;
import es.unex.sextante.parameters.ParameterRasterLayer;
import es.unex.sextante.parameters.ParameterString;
import es.unex.sextante.parameters.ParameterTableField;
import es.unex.sextante.parameters.ParameterVectorLayer;
import java.awt.RenderingHints.Key;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.text.Text;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * A process factory that wraps a SEXTANTE algorithm and can be used to get information about it and
 * create the corresponding process
 *
 * @author volaya
 */
public class SextanteProcessFactory implements ProcessFactory {
    public static final String SEXTANTE_NAMESPACE = "sxt";
    public static final String SEXTANTE_GRID_ENVELOPE = "gridEnvelope";
    public static final String SEXTANTE_GRID_CELL_SIZE = "gridCellSize";

    private static final Logger LOGGER = Logging.getLogger(SextanteProcessFactory.class);

    private Set<Name> names = new HashSet<Name>();

    /** Constructs a process factory based on the full SEXTANTE algorithms set */
    public SextanteProcessFactory() {
        Sextante.initialize();

        int algorithmsCount = Sextante.getAlgorithmsCount();
        LOGGER.info("Sextante loaded; it provides " + algorithmsCount + " algorithms!");

        // Register the algorithms loaded.
        HashMap<String, HashMap<String, GeoAlgorithm>> algorithmsHash = Sextante.getAlgorithms();
        Set<Name> result = new HashSet<Name>();

        for (HashMap<String, GeoAlgorithm> itemOb : algorithmsHash.values()) {
            for (Entry<String, GeoAlgorithm> entry : itemOb.entrySet()) {
                result.add(new NameImpl(SEXTANTE_NAMESPACE, entry.getValue().getCommandLineName()));
            }
        }
        names = Collections.unmodifiableSet(result);

        // Register this factory in the singleton Processors manager.
        org.geotools.process.Processors.addProcessFactory(this);
    }

    public InternationalString getTitle() {
        return new SimpleInternationalString("Sextante");
    }

    public Set<Name> getNames() {
        return names;
    }

    void checkName(Name name) {
        if (name == null) throw new NullPointerException("Process name cannot be null");
        if (!names.contains(name))
            throw new IllegalArgumentException("Unknown process '" + name + "'");
    }

    /**
     * Creates a geotools process which wraps a SEXTANTE geoalgorithm
     *
     * @param alg the SEXTANTE geoalgorithm to wrap
     */
    public Process create(Name name) throws IllegalArgumentException {
        checkName(name);
        try {
            return new SextanteProcess(
                    Sextante.getAlgorithmFromCommandLineName(name.getLocalPart()));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error occurred cloning the prototype " + "algorithm... this should not happen",
                    e);
        }
    }

    public InternationalString getDescription(Name name) {
        checkName(name);
        return Text.text(Sextante.getAlgorithmFromCommandLineName(name.getLocalPart()).getName());
    }

    public InternationalString getTitle(Name name) {
        return getDescription(name);
    }

    public String getName(Name name) {
        checkName(name);
        GeoAlgorithm algorithm = Sextante.getAlgorithmFromCommandLineName(name.getLocalPart());
        String sClass = algorithm.getClass().getName();
        int iLast = sClass.lastIndexOf(".");
        String sCommandName = sClass.substring(iLast + 1, sClass.length() - "Algorithm".length());
        return "Sextante" + sCommandName;
    }

    public boolean supportsProgress(Name name) {
        checkName(name);
        GeoAlgorithm algorithm = Sextante.getAlgorithmFromCommandLineName(name.getLocalPart());
        return algorithm.isDeterminatedProcess();
    }

    public String getVersion(Name name) {
        checkName(name);
        return "1.0.0";
    }

    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        checkName(name);
        GeoAlgorithm algorithm = Sextante.getAlgorithmFromCommandLineName(name.getLocalPart());

        ParametersSet paramSet = algorithm.getParameters();
        Map<String, Parameter<?>> paramInfo = new LinkedHashMap<String, Parameter<?>>();

        boolean hasRasterInput = false;
        for (int i = 0; i < paramSet.getNumberOfParameters(); i++) {
            es.unex.sextante.parameters.Parameter param = paramSet.getParameter(i);
            String title = param.getParameterDescription();
            String description = title;
            try {
                String td = param.getParameterAdditionalInfo().getTextDescription();
                if (td != null) {
                    description += " - " + td;
                }

                // TODO: for numeric data we can specify default value and
                // range, that should be useful
            } catch (NullParameterAdditionalInfoException e) {
                // fine
            }

            hasRasterInput =
                    hasRasterInput
                            || IRasterLayer.class.isAssignableFrom(param.getParameterClass());
            paramInfo.put(
                    param.getParameterName(),
                    new Parameter(
                            param.getParameterName(),
                            mapToGeoTools(param.getParameterClass()),
                            Text.text(title),
                            Text.text(description),
                            getAdditionalInfoMap(param)));
        }

        // check if there is any raster output
        boolean hasRasterOutput = false;
        OutputObjectsSet ooset = algorithm.getOutputObjects();
        for (int i = 0; i < ooset.getOutputObjectsCount(); i++) {
            Output output = ooset.getOutput(i);
            if (output instanceof OutputRasterLayer) {
                hasRasterOutput = true;
                break;
            }
        }

        // if there is any input or output raster we also need the user to specify
        // the grid structure, though we can get it from the first raster if there
        // are raster inputs, meaning in that case we'll grab it from
        if (hasRasterInput || hasRasterOutput) {
            // create a grid envelope, required only if there is no raster input we can
            // get the same info from
            if (hasRasterInput) {
                paramInfo.put(
                        SEXTANTE_GRID_ENVELOPE,
                        new Parameter(
                                SEXTANTE_GRID_ENVELOPE,
                                Envelope.class,
                                Text.text("Grid bounds (defaults to the bounds of the inputs)"),
                                Text.text("The real world coordinates bounding the grid"),
                                false,
                                0,
                                1,
                                null,
                                null));
                paramInfo.put(
                        SEXTANTE_GRID_CELL_SIZE,
                        new Parameter(
                                SEXTANTE_GRID_CELL_SIZE,
                                Double.class,
                                Text.text("Cell size (defaults to the size of the first input)"),
                                Text.text("The cell size in real world units"),
                                false,
                                0,
                                1,
                                null,
                                null));
            } else {
                paramInfo.put(
                        SEXTANTE_GRID_ENVELOPE,
                        new Parameter(
                                SEXTANTE_GRID_ENVELOPE,
                                Envelope.class,
                                Text.text("Grid bounds"),
                                Text.text("The real world coordinates bounding the grid"),
                                true,
                                1,
                                1,
                                null,
                                null));
                paramInfo.put(
                        SEXTANTE_GRID_CELL_SIZE,
                        new Parameter(
                                SEXTANTE_GRID_CELL_SIZE,
                                Double.class,
                                Text.text("Cell size"),
                                Text.text("The cell size in real world units"),
                                true,
                                1,
                                1,
                                null,
                                null));
            }
        }

        return paramInfo;
    }

    /**
     * Map Sextante common types into GeoTools common types
     *
     */
    protected Class mapToGeoTools(Class parameterClass) {
        if (IVectorLayer.class.isAssignableFrom(parameterClass)) {
            return FeatureCollection.class;
        } else if (IRasterLayer.class.isAssignableFrom(parameterClass)) {
            return GridCoverage2D.class;
        } else if (ITable.class.isAssignableFrom(parameterClass)) {
            return FeatureCollection.class;
        } else {
            return parameterClass;
        }
    }

    /**
     * Returns a map with additional info about a given parameter. It takes a SEXTANTE parameter and
     * produces a map suitable to be added to a GeoTools parameter
     *
     * @param param the parameter to take the additional information from
     * @return a Map with additional info about the parameter. Keys used to identify each element
     *     are defined in {@see SextanteProcessFactoryConstants}
     */
    private Map getAdditionalInfoMap(es.unex.sextante.parameters.Parameter param) {

        HashMap map = new HashMap();

        AdditionalInfo ai;
        try {
            ai = param.getParameterAdditionalInfo();
        } catch (NullParameterAdditionalInfoException e) {
            // we return an empty map if could not access the additional information
            return map;
        }

        if (param instanceof ParameterRasterLayer) {
            AdditionalInfoRasterLayer airl = (AdditionalInfoRasterLayer) ai;
            map.put(PARAMETER_MANDATORY, airl.getIsMandatory());
        }
        if (param instanceof ParameterVectorLayer) {
            AdditionalInfoVectorLayer aivl = (AdditionalInfoVectorLayer) ai;
            map.put(PARAMETER_MANDATORY, aivl.getIsMandatory());
            map.put(SHAPE_TYPE, aivl.getShapeType());
        }
        if (param instanceof ParameterVectorLayer) {
            AdditionalInfoVectorLayer aiv = (AdditionalInfoVectorLayer) ai;
            map.put(PARAMETER_MANDATORY, aiv.getIsMandatory());
        }
        if (param instanceof ParameterString) {
            AdditionalInfoString ais = (AdditionalInfoString) ai;
            map.put(DEFAULT_STRING_VALUE, ais.getDefaultString());
        }
        if (param instanceof ParameterNumericalValue) {
            AdditionalInfoNumericalValue ainv = (AdditionalInfoNumericalValue) ai;
            map.put(DEFAULT_NUMERICAL_VALUE, ainv.getDefaultValue());
            map.put(MAX_NUMERICAL_VALUE, ainv.getMaxValue());
            map.put(MIN_NUMERICAL_VALUE, ainv.getMinValue());
            map.put(NUMERICAL_VALUE_TYPE, ainv.getType());
        }
        if (param instanceof ParameterBoolean) {
            AdditionalInfoBoolean aib = (AdditionalInfoBoolean) ai;
            map.put(DEFAULT_BOOLEAN_VALUE, aib.getDefaultValue());
        }
        if (param instanceof ParameterMultipleInput) {
            AdditionalInfoMultipleInput aimi = (AdditionalInfoMultipleInput) ai;
            map.put(MULTIPLE_INPUT_TYPE, aimi.getDataType());
            map.put(PARAMETER_MANDATORY, aimi.getIsMandatory());
        }
        if (param instanceof ParameterFixedTable) {
            AdditionalInfoFixedTable aift = (AdditionalInfoFixedTable) ai;
            map.put(FIXED_TABLE_NUM_COLS, aift.getColsCount());
            map.put(FIXED_TABLE_NUM_ROWS, aift.getRowsCount());
            map.put(FIXED_TABLE_FIXED_NUM_ROWS, aift.isNumberOfRowsFixed());
        }
        if (param instanceof ParameterTableField) {
            AdditionalInfoTableField aitf = (AdditionalInfoTableField) ai;
            map.put(PARENT_PARAMETER_NAME, aitf.getParentParameterName());
        }

        return map;
    }

    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> inputs)
            throws IllegalArgumentException {
        checkName(name);
        GeoAlgorithm algorithm = Sextante.getAlgorithmFromCommandLineName(name.getLocalPart());

        Class outputClass = null;
        OutputObjectsSet ooset = algorithm.getOutputObjects();
        Map<String, Parameter<?>> outputInfo = new HashMap<String, Parameter<?>>();

        for (int i = 0; i < ooset.getOutputObjectsCount(); i++) {
            Output output = ooset.getOutput(i);

            if (output instanceof OutputVectorLayer) {
                outputClass = FeatureCollection.class;
            } else if (output instanceof OutputRasterLayer) {
                outputClass = GridCoverage2D.class;
            } else if (output instanceof OutputTable) {
                outputClass = FeatureCollection.class;
            } else if (output instanceof OutputText) {
                outputClass = String.class;
            } else {
                throw new IllegalArgumentException(
                        "Don't know how to handle output of type" + output.getClass());
            }

            outputInfo.put(
                    output.getName(),
                    new Parameter(
                            output.getName(),
                            outputClass,
                            Text.text(output.getDescription()),
                            Text.text(output.getDescription())));
        }

        return outputInfo;
    }

    @Override
    public String toString() {
        return "SextanteFactory";
    }

    public boolean isAvailable() {
        return true;
    }

    public Map<Key, ?> getImplementationHints() {
        return Collections.EMPTY_MAP;
    }
}
