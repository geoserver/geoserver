/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import org.geoserver.wps.transmute.ComplexTransmuter;
import org.geoserver.wps.transmute.DoubleTransmuter;
import org.geoserver.wps.transmute.GML2LineStringTransmuter;
import org.geoserver.wps.transmute.GML2LinearRingTransmuter;
import org.geoserver.wps.transmute.GML2MultiLineStringTransmuter;
import org.geoserver.wps.transmute.GML2MultiPointTransmuter;
import org.geoserver.wps.transmute.GML2MultiPolygonTransmuter;
import org.geoserver.wps.transmute.GML2PointTransmuter;
import org.geoserver.wps.transmute.GML2PolygonTransmuter;
import org.geoserver.wps.transmute.LiteralTransmuter;
import org.geoserver.wps.transmute.Transmuter;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.type.Name;

/**
 * Class for parsing and encoding inputs and results to processes
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class DataTransformer {
    private List<Transmuter> transmuters = new ArrayList<Transmuter>();
    private Map<Class<?>, Transmuter> defaultTransmuters = new HashMap<Class<?>, Transmuter>();
    private Map<String, Parameter<?>> inputParameters;
    private String urlBase = null;

    /** Constructor takes server base URL */
    public DataTransformer(String urlBase) {
        this.urlBase = urlBase;

        /* In order to allow true multiple output formats the container for the transmuters needs to
         * be changed to allow many-to-many Class <-> Transmuter mappings.
         */

        // Map Java types to transmuters
        this.defaultTransmuters.put(Double.class, new DoubleTransmuter());
        this.defaultTransmuters.put(MultiPolygon.class, new GML2MultiPolygonTransmuter());
        this.defaultTransmuters.put(Polygon.class, new GML2PolygonTransmuter());
        this.defaultTransmuters.put(Geometry.class, new GML2PolygonTransmuter());
        this.defaultTransmuters.put(MultiPoint.class, new GML2MultiPointTransmuter());
        this.defaultTransmuters.put(Point.class, new GML2PointTransmuter());
        this.defaultTransmuters.put(LinearRing.class, new GML2LinearRingTransmuter());
        this.defaultTransmuters.put(LineString.class, new GML2LineStringTransmuter());
        this.defaultTransmuters.put(MultiLineString.class, new GML2MultiLineStringTransmuter());

        // Add all default transmuters to master transmuters list
        this.transmuters.addAll(this.defaultTransmuters.values());
    }

    /** Returns Map of parsed inputs ready for execution */
    @SuppressWarnings("unchecked")
    public Map<String, Object> decodeInputs(
            final List<InputType> inputs, final Map<String, Parameter<?>> parameters) {
        Map<String, Object> inputMap = new HashMap<String, Object>();

        this.inputParameters = parameters;

        for (InputType input : inputs) {
            String identifier = input.getIdentifier().getValue();

            Object decoded = null;

            if (null != input.getData()) {
                // Decode inline data
                decoded = this.decodeInputData(input);
            }

            if (null != input.getReference()) {
                // Fetch external resource
                decoded = this.decodeReferenceData(identifier, input.getReference());
            }

            if (inputMap.containsKey(identifier)) {
                if (inputMap.get(identifier) instanceof List) {
                    List<Object> list = (List<Object>) inputMap.get(identifier);
                    list.add(decoded);
                } else {
                    List<Object> list = new ArrayList<Object>();
                    list.add(inputMap.get(identifier));
                    inputMap.put(identifier, list);
                }
            } else {
                inputMap.put(identifier, decoded);
            }
        }

        return inputMap;
    }

    /** Fetches and decodes external data references */
    private Object decodeReferenceData(
            final String identifier, final InputReferenceType reference) {
        Object data = null;
        URL url = null;
        Parameter<?> param = this.inputParameters.get(identifier);
        ComplexTransmuter transmuter = (ComplexTransmuter) this.getDefaultTransmuter(param.type);

        try {
            url = new URL(reference.getHref());
        } catch (MalformedURLException e) {
            throw new WPSException("NoApplicableCode", "Malformed parameter URL.");
        }

        try {
            data = transmuter.decode(url.openStream());
        } catch (IOException e) {
            throw new WPSException("NoApplicableCode", "IOException.");
        }

        return data;
    }

    private Object decodeInputData(final InputType input) {
        Object output = null;
        DataType data = input.getData();

        String parameterName = input.getIdentifier().getValue();
        Parameter<?> parameter = this.inputParameters.get(parameterName);

        try {
            if (null != data.getLiteralData()) {
                output = this.decodeLiteralData(data.getLiteralData(), parameter.type);
            }

            if (null != data.getComplexData()) {
                output = this.decodeComplexData(data.getComplexData(), parameter.type);
            }
        } catch (Exception e) {
            throw new WPSException("InvalidParameterValue", parameterName);
        }

        if (null != data.getBoundingBoxData()) {
            // Parse bounding box data
            throw new WPSException("NoApplicableCode", "Unimplemented");
        }

        return output;
    }

    private Object decodeComplexData(final ComplexDataType input, final Class<?> type) {
        Object data = input.getData().get(0);

        return data;
    }

    private Object decodeLiteralData(final LiteralDataType input, final Class<?> type) {
        Object data = null;

        LiteralTransmuter transmuter = (LiteralTransmuter) this.getDefaultTransmuter(type);

        data = transmuter.decode(input.getValue());

        return data;
    }

    /** Attempt to find ComplexTransmuter for given Java type and schema */
    public ComplexTransmuter getComplexTransmuter(final Class<?> type, final String schema) {
        for (Transmuter transmuter : this.transmuters) {
            if (false == transmuter instanceof ComplexTransmuter) {
                continue;
            }

            if (false
                    == ((ComplexTransmuter) transmuter)
                            .getSchema(this.urlBase)
                            .equalsIgnoreCase(schema)) {
                continue;
            }

            if (type != transmuter.getType()) {
                continue;
            }

            return (ComplexTransmuter) transmuter;
        }

        throw new WPSException(
                "NoApplicableCode", "Could not find ComplexTransmuter for '" + schema + "'.");
    }

    /** Return default a transmuter for a given Java type */
    public Transmuter getDefaultTransmuter(final Class<?> type) {
        Transmuter transmuter = this.defaultTransmuters.get(type);

        if (null == transmuter) {
            throw new WPSException(
                    "NoApplicableCode",
                    "No default transmuter registered for type " + type.toString() + "'.");
        }

        return transmuter;
    }

    /** Tests if all inputs and outputs of a Process are transmutable */
    public boolean isTransmutable(ProcessFactory pf, Name name) {
        for (Parameter<?> param : pf.getParameterInfo(name).values()) {
            try {
                this.getDefaultTransmuter(param.type);
            } catch (Exception e) {
                return false;
            }
        }

        for (Parameter<?> param : pf.getResultInfo(name, null).values()) {
            try {
                this.getDefaultTransmuter(param.type);
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }
}
