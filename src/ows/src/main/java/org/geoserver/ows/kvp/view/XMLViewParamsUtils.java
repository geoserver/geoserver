/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp.view;

import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.xml.XmlMapper;

public final class XMLViewParamsUtils {

    static final XmlMapper XML_MAPPER = XmlMapper.xmlBuilder().build();

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
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldError e2) {
            // Jackson XML 2.13.x issue workaround
            throw new RuntimeException(
                    "Incompatible Jackson XML version detected. Please ensure Jackson XML 2.14.x is used.", e2);
        }
    }
}
