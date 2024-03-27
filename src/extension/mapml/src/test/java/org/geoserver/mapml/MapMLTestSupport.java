/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXBException;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.wms.WMSTestSupport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** MapML test support class */
public class MapMLTestSupport extends WMSTestSupport {
    /**
     * Get the WMS response as a MapML object
     *
     * @param name the name of the layer
     * @param kvp the key value pairs
     * @param locale the locale
     * @param bbox the bounding box
     * @param srs the SRS
     * @param styles the styles
     * @param cqlFilter the CQL filter
     * @return the MapML object
     * @throws Exception if an error occurs
     */
    protected Mapml getWMSAsMapML(
            String name,
            Map kvp,
            Locale locale,
            String bbox,
            String srs,
            String styles,
            String cqlFilter,
            boolean isFeatureRepresentation)
            throws Exception {
        MockHttpServletRequest request =
                getMapMLWMSRequest(
                        name, kvp, locale, bbox, srs, styles, cqlFilter, isFeatureRepresentation);
        MockHttpServletResponse response = dispatch(request);
        return mapml(response);
    }

    protected String getWMSAsMapMLString(
            String name,
            Map kvp,
            Locale locale,
            String bbox,
            String srs,
            String styles,
            String cqlFilter,
            boolean isFeatureRepresentation)
            throws Exception {
        MockHttpServletRequest request =
                getMapMLWMSRequest(
                        name, kvp, locale, bbox, srs, styles, cqlFilter, isFeatureRepresentation);
        MockHttpServletResponse response = dispatch(request);
        return response.getContentAsString();
    }

    /**
     * Get the response as a MapML object
     *
     * @param path the path to the resource
     * @return the MapML object
     * @throws Exception if an error occurs
     */
    protected Mapml getAsMapML(final String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        return mapml(response);
    }

    /**
     * Convert the response to a MapML object
     *
     * @param response the response
     * @return the MapML object
     * @throws JAXBException if an error occurs
     * @throws UnsupportedEncodingException if an error occurs
     */
    protected Mapml mapml(MockHttpServletResponse response)
            throws JAXBException, UnsupportedEncodingException {
        MapMLEncoder encoder = new MapMLEncoder();
        StringReader reader = new StringReader(response.getContentAsString());
        Mapml mapml = null;
        try {
            mapml = encoder.decode(reader);
        } catch (DataBindingException e) {
            fail("MapML response is not valid XML");
        }
        return mapml;
    }

    /**
     * Get a MapML request
     *
     * @param name the name of the layer
     * @param kvp the key value pairs
     * @param locale the locale
     * @param bbox the bounding box
     * @param srs the SRS
     * @param styles the styles
     * @param cql the CQL filter
     * @return the request
     * @throws Exception if an error occurs
     */
    protected MockHttpServletRequest getMapMLWMSRequest(
            String name,
            Map kvp,
            Locale locale,
            String bbox,
            String srs,
            String styles,
            String cql,
            boolean isFeatureRepresentation)
            throws Exception {
        String path = null;
        cql = cql != null ? cql : "";
        MockHttpServletRequest request = null;
        String formatOptions =
                isFeatureRepresentation
                        ? MapMLConstants.MAPML_FEATURE_FO + ":true"
                        : MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION + ":image/png";
        if (kvp != null) {
            path = "wms";
            request = createRequest(path, kvp);
        } else {
            path =
                    "wms?LAYERS="
                            + name
                            + "&STYLES="
                            + (styles != null ? styles : "")
                            + "&FORMAT="
                            + MapMLConstants.MAPML_MIME_TYPE
                            + "&SERVICE=WMS&VERSION=1.3.0"
                            + "&REQUEST=GetMap"
                            + "&SRS="
                            + srs
                            + "&BBOX="
                            + (bbox != null ? bbox : "0,0,1,1")
                            + "&WIDTH=150"
                            + "&HEIGHT=150"
                            + "&cql_filter="
                            + cql
                            + "&format_options="
                            + formatOptions;
            request = createRequest(path);
        }

        return request;
    }
}
