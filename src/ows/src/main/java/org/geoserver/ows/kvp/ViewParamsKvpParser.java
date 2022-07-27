/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Parses view parameters with the selected format parser or using the default:
 *
 * <pre>VIEWPARAMS=opt1:val1,val2;opt2:val1;opt3:...[,opt1:val1,val2;opt2:val1;opt3:...]</pre>
 *
 * @see FormatOptionsKvpParser
 */
public class ViewParamsKvpParser extends KvpParser implements ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(ViewParamsKvpParser.class);

    private static final String VIEW_PARAMS_FORMAT_PARAMETER_NAME = "viewParamsFormat";
    /** application context used to lookup KvpParsers */
    ApplicationContext applicationContext;

    public ViewParamsKvpParser() {
        super("viewparams", List.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object parse(String value) throws Exception {
        ViewParamsFormatParser formatParser = getRequestFormatParser();
        LOGGER.log(Level.FINE, "Using selected format parser: {0}", formatParser);
        return formatParser.parse(value);
    }

    private ViewParamsFormatParser getRequestFormatParser() {
        List<ViewParamsFormatParser> formatParsers =
                GeoServerExtensions.extensions(ViewParamsFormatParser.class, applicationContext);
        String formatParserParameterValue = getFormatParserParameterValue();
        LOGGER.log(Level.FINE, "Using format parser identifier: {0}", formatParserParameterValue);
        return formatParsers.stream()
                .filter(fp -> fp.getIdentifier().equals(formatParserParameterValue))
                .findFirst()
                .orElseThrow(
                        () ->
                                new ServiceException(
                                        "Selected viewParamsFormat is not available as implementation on GeoServer. "
                                                + "viewParamsFormat value is '"
                                                + getRequestFormatParserParameterValue()
                                                + "'",
                                        ServiceException.INVALID_PARAMETER_VALUE,
                                        "viewParamsFormat"));
    }

    private String getFormatParserParameterValue() {
        String formatParserParameterValue = getRequestFormatParserParameterValue();
        // set the default if not other implementation found
        if (StringUtils.isBlank(formatParserParameterValue)) {
            formatParserParameterValue =
                    CharSeparatedViewParamsFormatParser.CHAR_SEPARATED_IDENTIFIER;
        }
        return formatParserParameterValue;
    }

    private String getRequestFormatParserParameterValue() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null || request.getRawKvp() == null)
            return getSerlvetRequestFormatParserParameterValue();
        Map<String, Object> rawKvp = request.getRawKvp();
        if (mapContainsKey(VIEW_PARAMS_FORMAT_PARAMETER_NAME, rawKvp)) {
            return (String) mapGet(VIEW_PARAMS_FORMAT_PARAMETER_NAME, rawKvp);
        }
        return getSerlvetRequestFormatParserParameterValue();
    }

    private String getSerlvetRequestFormatParserParameterValue() {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null
                || servletRequestAttributes.getRequest() == null
                || servletRequestAttributes.getRequest().getParameterMap() == null) return null;
        HttpServletRequest request = servletRequestAttributes.getRequest();
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (mapContainsKey(VIEW_PARAMS_FORMAT_PARAMETER_NAME, parameterMap)) {
            String[] strings = mapGet(VIEW_PARAMS_FORMAT_PARAMETER_NAME, parameterMap);
            return getFirstExisting(strings);
        }
        return null;
    }

    private boolean mapContainsKey(String key, Map<String, ?> map) {
        return map.keySet().stream()
                .anyMatch(k -> k.trim().toLowerCase().equals(key.trim().toLowerCase()));
    }

    private <T> T mapGet(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(e -> e.getKey().trim().toLowerCase().equals(key.trim().toLowerCase()))
                .map(e -> e.getValue())
                .findFirst()
                .orElse(null);
    }

    private String getFirstExisting(String[] strArray) {
        if (strArray == null) return null;
        for (String value : strArray) {
            if (StringUtils.isNotBlank(value)) return value;
        }
        return null;
    }
}
