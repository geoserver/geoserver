/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.groovy;

import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.wps.WpsHook;
import org.geotools.data.Parameter;

/**
 * The GroovyWpsHook converts GeoScript input and output types into GeoTools types, converts input
 * parameters from GeoTools to GeoScript values, and then converts results from GeoScript to
 * GeoTools values.
 */
public class GroovyWpsHook extends WpsHook {

    public GroovyWpsHook(ScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public Map<String, Object> run(Map<String, Object> input, ScriptEngine engine)
            throws ScriptException {
        // Convert GeoTools input values into GeoScript values
        Map<String, Object> newInput = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object newValue = convertGeoToolsToGeoScript(value);
            newInput.put(key, newValue);
        }
        // The Script expects GeoScript values
        Map<String, Object> results = super.run(newInput, engine);
        // Convert GeoScript result values into GeoTools values
        Map<String, Object> newResults = new HashMap<String, Object>();
        try {
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Object newValue = convertGeoScriptToGeoTools(value);
                newResults.put(key, newValue);
            }
        } catch (Exception ex) {
            throw new ScriptException(ex);
        }
        return newResults;
    }

    @Override
    public Map<String, Parameter<?>> getInputs(ScriptEngine engine) throws ScriptException {
        Map<String, Parameter<?>> inputs = super.getInputs(engine);
        // Convert all input Parameters from GeoScript types to GeoTools Types
        for (Map.Entry<String, Parameter<?>> entry : inputs.entrySet()) {
            String key = entry.getKey();
            Parameter param = entry.getValue();
            Class clazz =
                    geoscript.process.Process.convertGeoScriptToGeoToolsClass(param.getType());
            param = changeParamType(param, clazz);
            inputs.put(key, param);
        }
        return inputs;
    }

    @Override
    public Map<String, Parameter<?>> getOutputs(ScriptEngine engine) throws ScriptException {
        Map<String, Parameter<?>> outputs = super.getOutputs(engine);
        // Convert all output Parameters from GeoScript types to GeoTools Types
        for (Map.Entry<String, Parameter<?>> entry : outputs.entrySet()) {
            String key = entry.getKey();
            Parameter param = entry.getValue();
            Class clazz =
                    geoscript.process.Process.convertGeoScriptToGeoToolsClass(param.getType());
            param = changeParamType(param, clazz);
            outputs.put(key, param);
        }
        return outputs;
    }

    /**
     * Convert a GeoTools object to a GeoScript object.
     *
     * @param value The Object
     * @return A GeoScript object if it is possible
     */
    protected Object convertGeoToolsToGeoScript(Object value) {
        if (value instanceof org.locationtech.jts.geom.Geometry) {
            return geoscript.geom.Geometry.wrap((org.locationtech.jts.geom.Geometry) value);
        } else if (value instanceof org.geotools.geometry.jts.ReferencedEnvelope) {
            return new geoscript.geom.Bounds((org.geotools.geometry.jts.ReferencedEnvelope) value);
        } else if (value instanceof org.geotools.feature.FeatureCollection) {
            return new geoscript.layer.Layer((org.geotools.feature.FeatureCollection) value);
        } else {
            return value;
        }
    }

    /**
     * Convert a GeoScript object to a GeoTools object.
     *
     * @param value The Object
     * @return A GeoTools object if it is possible
     */
    protected Object convertGeoScriptToGeoTools(Object value) throws Exception {
        if (value instanceof geoscript.geom.Geometry) {
            // org.locationtech.jts.geom.Geometry
            return ((geoscript.geom.Geometry) value).getG();
        } else if (value instanceof geoscript.geom.Bounds) {
            // org.geotools.geometry.jts.ReferencedEnvelope
            return ((geoscript.geom.Bounds) value).getEnv();
        } else if (value instanceof geoscript.layer.Layer) {
            // org.geotools.data.FeatureSource
            return ((geoscript.layer.Layer) value).getFs().getFeatures();
        } else if (value instanceof geoscript.layer.Cursor) {
            // org.geotools.feature.FeatureCollection
            return ((geoscript.layer.Cursor) value).getCol();
        } else {
            return value;
        }
    }

    /**
     * Create a new Parameter with all of the values of the given Parameter except for it's Type
     *
     * @param param A Parameter
     * @param type The new Class type
     * @return A new Parameter
     */
    private Parameter changeParamType(Parameter param, Class type) {
        return new Parameter(
                param.getName(),
                type,
                param.getTitle(),
                param.getDescription(),
                param.isRequired(),
                param.getMinOccurs(),
                param.getMaxOccurs(),
                param.getDefaultValue(),
                null);
    }
}
