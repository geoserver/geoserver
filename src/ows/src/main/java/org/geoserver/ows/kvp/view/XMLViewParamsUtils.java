/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public final class XMLViewParamsUtils {

    static final XmlMapper XML_MAPPER = new XmlMapper();

    private XMLViewParamsUtils() {}

    /**
     * Parses the XML string into a ViewParamsRoot entity bean.
     *
     * @param xmlStr the XML string to parse
     * @return the parsed instance
     */
    public static ViewParamsRoot parseViewParams(String xmlStr) {
        ViewParamsRoot viewParamsRoot = null;
        try {
            viewParamsRoot = XML_MAPPER.readValue(xmlStr, ViewParamsRoot.class);
            return viewParamsRoot;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
