/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import java.util.Map;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.GIFMapResponse;
import org.geoserver.wms.style.PaletteParser;
import org.geotools.filter.function.EnvFunction;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;

/**
 * Integrates the ncWMS extension parameters into the env approach normally chosen by GeoServer to
 * add dynamic parameters (the ones used by the colormap rendering transformation)
 */
public class GetMapNcWmsCallback extends AbstractDispatcherCallback {

    private static String COLORSCALERANGE = "COLORSCALERANGE";

    private static String NUMCOLORBANDS = "NUMCOLORBANDS";

    private static String ABOVEMAXCOLOR = "ABOVEMAXCOLOR";

    private static String BELOWMINCOLOR = "BELOWMINCOLOR";

    private static String LOGSCALE = "LOGSCALE";

    private static String OPACITY = "OPACITY";

    private static String ANIMATION = "ANIMATION";

    private GIFMapResponse gifResponse;

    public GetMapNcWmsCallback(GIFMapResponse gifResponse) {
        this.gifResponse = gifResponse;
    }

    /*
     * The choice of serviceDispatcher is not random, it allows the EnvironmentInjectionCallback to setup the env vars in the local map (which happens
     * by calling setLocalValues() and wiping out any previous setting)
     */
    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Map kvp = request.getKvp();
        Map rawKvp = request.getRawKvp();

        if (kvp.containsKey(COLORSCALERANGE)) {
            String scaleRangeSpec = (String) rawKvp.get(COLORSCALERANGE);
            NumberRange<Double> range = parseColorScaleRange(scaleRangeSpec);
            EnvFunction.setLocalValue(PaletteParser.RANGE_MIN, range.getMinimum());
            EnvFunction.setLocalValue(PaletteParser.RANGE_MAX, range.getMaximum());
        }
        if (kvp.containsKey(OPACITY)) {
            String str = (String) rawKvp.get(OPACITY);
            Integer value = Converters.convert(str, Integer.class);
            if ((value == null && str != null && !str.trim().isEmpty())
                    || (value != null && (value < 0 || value > 100))) {
                throw new ServiceException(
                        "Expected a int value between 0 and 100 for OPACITY but found '"
                                + str
                                + "' instead",
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "OPACITY");
            }
            EnvFunction.setLocalValue(PaletteParser.OPACITY, value / 100f);
        }
        if (kvp.containsKey(ANIMATION)) {
            String str = (String) rawKvp.get(ANIMATION);
            Boolean animate = Converters.convert(str, Boolean.class);
            if ((animate == null && str != null && !str.trim().isEmpty())) {
                throw new ServiceException(
                        "Expected a boolean value for ANIMATION but found '" + str + "' instead",
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "ANIMATION");
            }
            if (animate) {
                String format = (String) rawKvp.get("format");
                if (!gifResponse.getOutputFormats().contains(format)) {
                    throw new ServiceException(
                            "Animation is supported only with image/gif output format",
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "format");
                } else if (!GIFMapResponse.IMAGE_GIF_SUBTYPE_ANIMATED.equals(format)) {
                    // switch to animated mode
                    rawKvp.put("format", GIFMapResponse.IMAGE_GIF_SUBTYPE_ANIMATED);
                    kvp.put("format", GIFMapResponse.IMAGE_GIF_SUBTYPE_ANIMATED);
                }
            }
        }
        mapParameter(kvp, rawKvp, NUMCOLORBANDS, PaletteParser.NUMCOLORS, Integer.class);
        mapParameter(kvp, rawKvp, BELOWMINCOLOR, PaletteParser.COLOR_BEFORE, String.class);
        mapParameter(kvp, rawKvp, ABOVEMAXCOLOR, PaletteParser.COLOR_AFTER, String.class);
        mapParameter(kvp, rawKvp, LOGSCALE, PaletteParser.LOGSCALE, String.class);

        return service;
    }

    /**
     * Maps a parameter needing at most a simple type conversion from the kvp map to the env
     * function
     */
    private void mapParameter(
            Map kvp,
            Map rawKvp,
            String ncWmsParameter,
            String paletteParameter,
            Class targetClass) {
        if (kvp.containsKey(ncWmsParameter)) {
            String str = (String) rawKvp.get(ncWmsParameter);
            Object value = Converters.convert(str, targetClass);
            if (value == null && str != null && !str.trim().isEmpty()) {
                throw new ServiceException(
                        "Expected a value of type "
                                + targetClass.getSimpleName()
                                + " for "
                                + ncWmsParameter
                                + " but found '"
                                + str
                                + "' instead",
                        ServiceException.INVALID_PARAMETER_VALUE,
                        ncWmsParameter);
            }
            EnvFunction.setLocalValue(paletteParameter, value);
        }
    }

    private NumberRange<Double> parseColorScaleRange(String scaleRangeSpec) {
        String[] elements = scaleRangeSpec.split("\\s*,\\s*");
        if (elements.length != 2) {
            throw new ServiceException(
                    COLORSCALERANGE
                            + " must be specified as 'min,max' where min and max are numbers",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    COLORSCALERANGE);
        }
        double min = parseDouble(elements[0], COLORSCALERANGE);
        double max = parseDouble(elements[1], COLORSCALERANGE);
        return NumberRange.create(min, max);
    }

    private double parseDouble(String element, String locator) {
        try {
            return Double.parseDouble(element);
        } catch (NumberFormatException e) {
            throw new ServiceException(
                    "Expected a number but got '" + element + "'",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    locator);
        }
    }
}
