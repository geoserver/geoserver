/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.util;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * @author $Author: Alessio Fabiani (alessio.fabiani@geo-solutions.it)
 * @author $Author: Simone Giannecchini (simone.giannecchini@geo-solutions.it)
 */
public class CoverageUtils {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(CoverageUtils.class.toString());
    public static final int TRANSPARENT = 0;
    public static final int OPAQUE = 1;

    public static GeneralParameterValue[] getParameters(ParameterValueGroup params) {
        final List parameters = new ArrayList();
        final String readGeometryKey = AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString();

        if ((params != null) && (params.values().size() > 0)) {
            List list = params.values();
            final Iterator it = list.iterator();
            while (it.hasNext()) {
                final ParameterValue val = (ParameterValue) it.next();

                if (val != null) {
                    final ParameterDescriptor descr = (ParameterDescriptor) val.getDescriptor();
                    final String _key = descr.getName().toString();

                    if ("namespace".equals(_key)) {
                        // skip namespace as it is *magic* and
                        // appears to be an entry used in all dataformats?
                        //
                        continue;
                    }

                    // IGNORING READ_GRIDGEOMETRY2D param
                    if (_key.equalsIgnoreCase(readGeometryKey)) {
                        continue;
                    }
                    final Object value = val.getValue();

                    parameters.add(
                            new DefaultParameterDescriptor(_key, value.getClass(), null, value)
                                    .createValue());
                }
            }

            return (!parameters.isEmpty())
                    ? (GeneralParameterValue[])
                            parameters.toArray(new GeneralParameterValue[parameters.size()])
                    : null;
        } else {
            return null;
        }
    }

    public static GeneralParameterValue[] getParameters(ParameterValueGroup params, Map values) {
        return getParameters(params, values, false);
    }

    public static GeneralParameterValue[] getParameters(
            ParameterValueGroup params, Map values, boolean readGeom) {
        final List<ParameterValue<?>> parameters = new ArrayList<ParameterValue<?>>();
        final String readGeometryKey = AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString();

        if ((params != null) && (params.values().size() > 0)) {
            final List<GeneralParameterValue> elements = params.values();
            for (GeneralParameterValue elem : elements) {
                final ParameterValue<?> val = (ParameterValue<?>) elem;

                if (val != null) {
                    final ParameterDescriptor<?> descr = val.getDescriptor();
                    final String _key = descr.getName().toString();

                    if ("namespace".equals(_key)) {
                        // skip namespace as it is *magic* and
                        // appears to be an entry used in all dataformats?
                        //
                        continue;
                    }

                    // /////////////////////////////////////////////////////////
                    //
                    // request param for better management of coverage
                    //
                    // /////////////////////////////////////////////////////////
                    if (_key.equalsIgnoreCase(readGeometryKey) && !readGeom) {
                        // IGNORING READ_GRIDGEOMETRY2D param
                        continue;
                    }

                    // /////////////////////////////////////////////////////////
                    //
                    // format specific params
                    //
                    // /////////////////////////////////////////////////////////
                    final Object value = CoverageUtils.getCvParamValue(_key, val, values);
                    parameters.add(
                            new DefaultParameterDescriptor(_key, descr.getValueClass(), null, value)
                                    .createValue());
                }
            }

            return (!parameters.isEmpty())
                    ? (GeneralParameterValue[])
                            parameters.toArray(new GeneralParameterValue[parameters.size()])
                    : new GeneralParameterValue[0];
        } else {
            return new GeneralParameterValue[0];
        }
    }

    public static Map getParametersKVP(ParameterValueGroup params) {
        final Map parameters = new HashMap();
        final String readGeometryKey = AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString();

        if ((params != null) && (params.values().size() > 0)) {
            final List list = params.values();
            final Iterator it = list.iterator();
            while (it.hasNext()) {
                final ParameterValue val = (ParameterValue) it.next();

                if (val != null) {
                    final ParameterDescriptor descr = (ParameterDescriptor) val.getDescriptor();

                    final String _key = descr.getName().toString();

                    if ("namespace".equals(_key)) {
                        // skip namespace as it is *magic* and
                        // appears to be an entry used in all dataformats?
                        //
                        continue;
                    }

                    // /////////////////////////////////////////////////////////
                    //
                    // request param for better management of coverage
                    //
                    // /////////////////////////////////////////////////////////
                    if (_key.equalsIgnoreCase(readGeometryKey)) {
                        // IGNORING READ_GRIDGEOMETRY2D param
                        continue;
                    }

                    Object value = val.getValue();
                    String text = "";

                    if (value == null) {
                        text = null;
                    } else if (value instanceof String) {
                        text = (String) value;
                    } else {
                        text = value.toString();
                    }

                    parameters.put(_key, (text != null) ? text : "");
                }
            }

            return parameters;
        } else {
            return parameters;
        }
    }

    /** */
    public static Object getCvParamValue(
            final String key, ParameterValue param, final List paramValues, final int index) {
        Object value = null;

        try {
            if (key.equalsIgnoreCase("crs")) {
                if ((getParamValue(paramValues, index) != null)
                        && (((String) getParamValue(paramValues, index)).length() > 0)) {
                    if ((paramValues.get(index) != null)
                            && (((String) paramValues.get(index)).length() > 0)) {
                        value = CRS.parseWKT((String) paramValues.get(index));
                    }
                } else {
                    LOGGER.info("Unable to find a crs for the coverage param, using EPSG:4326");
                    value = CRS.decode("EPSG:4326");
                }
            } else if (key.equalsIgnoreCase("envelope")) {
                if ((getParamValue(paramValues, index) != null)
                        && (((String) getParamValue(paramValues, index)).length() > 0)) {
                    String tmp = (String) getParamValue(paramValues, index);

                    if ((tmp.indexOf("[") > 0) && (tmp.indexOf("]") > tmp.indexOf("["))) {
                        tmp = tmp.substring(tmp.indexOf("[") + 1, tmp.indexOf("]")).trim();
                        tmp = tmp.replaceAll(",", "");

                        String[] strCoords = tmp.split(" ");
                        double[] coords = new double[strCoords.length];

                        if (strCoords.length == 4) {
                            for (int iT = 0; iT < 4; iT++) {
                                coords[iT] = Double.parseDouble(strCoords[iT].trim());
                            }

                            value =
                                    (org.opengis.geometry.Envelope)
                                            new GeneralEnvelope(
                                                    new double[] {coords[0], coords[1]},
                                                    new double[] {coords[2], coords[3]});
                        }
                    }
                }
            } else {
                Class[] clArray = {getParamValue(paramValues, index).getClass()};
                Object[] inArray = {getParamValue(paramValues, index)};
                value = param.getValue().getClass().getConstructor(clArray).newInstance(inArray);
            }

            // Intentionally generic exception catched
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }

            value = null;

            // errors.add("paramValue[" + i + "]",
            // new ActionError("error.dataFormatEditor.param.parse", key,
            // getParamValue(i).getClass(), e));
        }

        return value;
    }

    private static String getParamValue(final List paramValues, final int index) {
        return (String) paramValues.get(index);
    }

    /** */
    public static Object getCvParamValue(final String key, ParameterValue param, final Map params) {
        Object value = null;

        try {
            if (key.equalsIgnoreCase("crs")) {
                if ((params.get(key) != null) && (((String) params.get(key)).length() > 0)) {
                    value = CRS.parseWKT((String) params.get(key));
                } else {
                    LOGGER.info("Unable to find a crs for the coverage param, using EPSG:4326");
                    value = CRS.decode("EPSG:4326");
                }
            } else if (key.equalsIgnoreCase("envelope")) {
                if ((params.get(key) != null) && (((String) params.get(key)).length() > 0)) {
                    String tmp = (String) params.get(key);

                    if ((tmp.indexOf("[") > 0) && (tmp.indexOf("]") > tmp.indexOf("["))) {
                        tmp = tmp.substring(tmp.indexOf("[") + 1, tmp.indexOf("]")).trim();
                        tmp = tmp.replaceAll(",", "");

                        String[] strCoords = tmp.split(" ");
                        double[] coords = new double[strCoords.length];

                        if (strCoords.length == 4) {
                            for (int iT = 0; iT < 4; iT++) {
                                coords[iT] = Double.parseDouble(strCoords[iT].trim());
                            }

                            value =
                                    (org.opengis.geometry.Envelope)
                                            new GeneralEnvelope(
                                                    new double[] {coords[0], coords[1]},
                                                    new double[] {coords[2], coords[3]});
                        }
                    }
                }
            } else if (key.equalsIgnoreCase(
                    AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
                if ((params.get(key) != null)
                        && params.get(key) instanceof String
                        && (((String) params.get(key)).length() > 0)) {
                    String tmp = (String) params.get(key);

                    if ((tmp.indexOf("[") > 0) && (tmp.indexOf("]") > tmp.indexOf("["))) {
                        tmp = tmp.substring(tmp.indexOf("[") + 1, tmp.indexOf("]")).trim();
                        tmp = tmp.replaceAll(",", "");

                        String[] strCoords = tmp.split(" ");
                        double[] coords = new double[strCoords.length];

                        if (strCoords.length == 4) {
                            for (int iT = 0; iT < 4; iT++) {
                                coords[iT] = Double.parseDouble(strCoords[iT].trim());
                            }

                            value =
                                    (org.opengis.geometry.Envelope)
                                            new GeneralEnvelope(
                                                    new double[] {coords[0], coords[1]},
                                                    new double[] {coords[2], coords[3]});
                        }
                    }
                } else if ((params.get(key) != null)
                        && params.get(key) instanceof GeneralGridGeometry) {
                    value = params.get(key);
                }
            } else {
                final Class<? extends Object> target = param.getDescriptor().getValueClass();
                if (key.equalsIgnoreCase("InputTransparentColor")
                        || key.equalsIgnoreCase("OutputTransparentColor")) {
                    if (params.get(key) != null) {
                        value = Color.decode((String) params.get(key));
                    } else {
                        Class[] clArray = {Color.class};
                        Object[] inArray = {params.get(key)};
                        value = target.getConstructor(clArray).newInstance(inArray);
                    }
                } else if (key.equalsIgnoreCase("BackgroundValues")) {
                    if (params.get(key) != null) {
                        String temp = (String) params.get(key);
                        String[] elements = temp.split(",");
                        final double[] backgroundValues = new double[elements.length];
                        for (int i = 0; i < elements.length; i++)
                            backgroundValues[i] = Double.valueOf(elements[i]);
                        value = backgroundValues;
                    }
                } else if (key.equalsIgnoreCase("InputImageThresholdValue")) {
                    if (params.get(key) != null) {
                        String temp = (String) params.get(key);
                        value = Double.valueOf(temp);
                    }
                } else if (key.equalsIgnoreCase("Filter")) {
                    Object sfilter = params.get(key);
                    if (sfilter != null) {
                        if (sfilter instanceof String) {
                            value = ECQL.toFilter((String) sfilter);
                        } else if (sfilter instanceof Filter) {
                            value = (Filter) sfilter;
                        }
                    } else {
                        value = param.getValue();
                    }

                } else {
                    value = params.get(key);
                    if (value != null) {
                        Object converted = Converters.convert(value, target);
                        if (converted == null) {
                            throw new RuntimeException(
                                    "Failed to convert " + value + " to " + target.getName());
                        } else {
                            value = converted;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
            value = param.getValue();
        }

        return value;
    }

    /**
     * Merges the provided parameter in the read parameters, provided it's included in the specified
     * descriptors with one of the aliases
     *
     * @param parameterDescriptors The parameter descriptors of the reader
     * @param readParameters The current set of reader parameters
     */
    public static GeneralParameterValue[] mergeParameter(
            List<GeneralParameterDescriptor> parameterDescriptors,
            GeneralParameterValue[] readParameters,
            Object value,
            String... parameterAliases) {
        // setup a param name alias set
        Set<String> aliases = new HashSet<String>(Arrays.asList(parameterAliases));

        // scan all the params looking for the one we want to add
        for (GeneralParameterDescriptor pd : parameterDescriptors) {
            // in case of match of any alias add a param value to the lot
            if (aliases.contains(pd.getName().getCode())) {
                final ParameterValue pv = (ParameterValue) pd.createValue();
                pv.setValue(value);

                // if it's in the list already, override
                int existingPvIndex = -1;
                for (int i = 0; i < readParameters.length && existingPvIndex < 0; i++) {
                    GeneralParameterValue oldPv = readParameters[i];
                    if (aliases.contains(oldPv.getDescriptor().getName().getCode())) {
                        existingPvIndex = i;
                    }
                }

                if (existingPvIndex >= 0) {
                    GeneralParameterValue[] clone =
                            new GeneralParameterValue[readParameters.length];
                    System.arraycopy(readParameters, 0, clone, 0, readParameters.length);
                    clone[existingPvIndex] = pv;
                    readParameters = clone;

                } else {
                    // add to the list
                    GeneralParameterValue[] clone =
                            new GeneralParameterValue[readParameters.length + 1];
                    System.arraycopy(readParameters, 0, clone, 0, readParameters.length);
                    clone[readParameters.length] = pv;
                    readParameters = clone;
                }

                // leave
                break;
            }
        }

        return readParameters;
    }
}
