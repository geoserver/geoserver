/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.kvp.view.LayerParams;
import org.geoserver.ows.kvp.view.Parameter;
import org.geoserver.ows.kvp.view.ViewParamsRoot;
import org.geoserver.ows.kvp.view.XMLViewParamsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * XML view params format parser. Parses view parameters which are of the form:
 *
 * <p>{@literal <VP><PS><P n="mmsi">538008302,244060802,538008505</P><P
 * n="mmsi">22,44</P></PS><PS/><PS><P n="csvInput">acv,rrp;1,0;0,7;22,1</P></PS></VP>}
 */
public class XMLViewParamsFormatParser implements ViewParamsFormatParser {

    private static final Logger LOGGER = Logging.getLogger(XMLViewParamsFormatParser.class);
    public static final String XML_IDENTIFIER = "XML";

    @Override
    public String getIdentifier() {
        return XML_IDENTIFIER;
    }

    @Override
    public List<Object> parse(String value) throws Exception {
        List<Object> resultList = new ArrayList<>();
        ViewParamsRoot viewParams = XMLViewParamsUtils.parseViewParams(value);
        for (LayerParams layerParams : viewParams.getLayerParams()) {
            resultList.add(parseParams(layerParams));
        }
        return resultList;
    }

    private Map<String, Object> parseParams(LayerParams layerParams) throws Exception {
        Map<String, Object> paramsMap = new HashMap<>();
        for (Parameter parameter : layerParams.getParameters()) {
            parseValue(parameter, paramsMap);
        }
        return paramsMap;
    }

    private void parseValue(Parameter parameter, Map<String, Object> paramsMap) throws Exception {
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);
        String key = parameter.getName();
        String raw = StringUtils.isNotBlank(parameter.getValue()) ? parameter.getValue() : "true";
        Object parsed = null;
        for (KvpParser parser : parsers) {
            if (key.equalsIgnoreCase(parser.getKey())) {
                parsed = parser.parse(raw);
                if (parsed != null) {
                    break;
                }
            }
        }
        if (parsed == null) {
            if (LOGGER.isLoggable(Level.FINER))
                LOGGER.finer(
                        "Could not find kvp parser for: '" + key + "'. Storing as raw string.");
            parsed = raw;
        }
        paramsMap.put(key, parsed);
    }
}
