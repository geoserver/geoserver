/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractHTMLMessageConverter;
import org.geoserver.ogcapi.FreemarkerTemplateSupport;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class HTMLMapMessageConverter extends AbstractHTMLMessageConverter<HTMLMap> {

    static final Logger LOGGER = Logging.getLogger(HTMLMapMessageConverter.class);

    /**
     * Set of parameters that we can ignore, since they are not part of the OpenLayers WMS request
     */
    private static final Set<String> IGNORED_PARAMETERS =
            new HashSet<>(
                    Arrays.asList(
                            "REQUEST", "TILED", "BBOX", "SERVICE", "VERSION", "FORMAT", "WIDTH",
                            "HEIGHT", "SRS"));

    public HTMLMapMessageConverter(FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(HTMLMap.class, WMSInfo.class, templateSupport, geoServer);
    }

    @Override
    protected void writeInternal(HTMLMap htmlMap, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        GetMapRequest getMapRequest = htmlMap.getMapContent().getRequest();
        HashMap<String, Object> model = setupModel(getMapRequest);
        model.put("units", getUnits(htmlMap.getMapContent()));
        APIRequestInfo ri = APIRequestInfo.get();
        HttpServletRequest httpRequest = ri.getRequest();
        model.put("url", httpRequest.getRequestURL());
        model.put("parameters", getLayerParameter(getMapRequest.getRawKvp()));
        Charset defaultCharset = getDefaultCharset();
        if (outputMessage != null && outputMessage.getBody() != null && defaultCharset != null) {
            templateSupport.processTemplate(
                    null,
                    "htmlmap.ftl",
                    MapsService.class,
                    model,
                    new OutputStreamWriter(outputMessage.getBody(), defaultCharset),
                    defaultCharset);
        } else {
            LOGGER.warning(
                    "Either the default character set, output message or body was null, so the "
                            + "htmlmap.ftl template could not be processed.");
        }
    }

    /**
     * OL does support only a limited number of unit types, we have to try and return one of those,
     * otherwise the scale won't be shown. From the OL guide: possible values are "degrees" (or
     * "dd"), "m", "ft", "km", "mi", "inches".
     */
    protected String getUnits(WMSMapContent mapContent) {
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        // first rough approximation, meters for projected CRS, degrees for the
        // others
        String result = crs instanceof ProjectedCRS ? "m" : "degrees";
        try {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            // use the unicode escape sequence for the degree sign so its not
            // screwed up by different local encodings
            final String degreeSign = "\u00B0";
            if (null != unit)
                switch (unit) {
                    case degreeSign:
                    case "degrees":
                    case "dd":
                        result = "degrees";
                        break;
                    case "m":
                    case "meters":
                        result = "m";
                        break;
                    case "km":
                    case "kilometers":
                        result = "mi";
                        break;
                    case "in":
                    case "inches":
                        result = "inches";
                        break;
                    case "ft":
                    case "feets":
                        result = "ft";
                        break;
                    case "mi":
                    case "miles":
                        result = "mi";
                        break;
                    default:
                        break;
                }
        } catch (IndexOutOfBoundsException e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }
        return result;
    }

    /**
     * Returns a list of maps with the name and value of each parameter that we have to forward to
     * OpenLayers. Forwarded parameters are all the provided ones, besides a short set contained in
     * {@link #IGNORED_PARAMETERS}.
     */
    private List<Map<String, String>> getLayerParameter(Map<String, String> rawKvp) {
        List<Map<String, String>> result = new ArrayList<>(rawKvp.size());
        for (Map.Entry<String, String> en : rawKvp.entrySet()) {
            String paramName = en.getKey();

            if (IGNORED_PARAMETERS.contains(paramName.toUpperCase())) {
                continue;
            }

            // this won't work for multi-valued parameters, but we have none so
            // far (they are common just in HTML forms...)
            Map<String, String> map = new HashMap<>();

            map.put("name", paramName);
            map.put("value", en.getValue());
            result.add(map);
        }

        return result;
    }
}
