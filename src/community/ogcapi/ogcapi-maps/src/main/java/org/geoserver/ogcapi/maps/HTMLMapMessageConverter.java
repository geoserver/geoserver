/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractHTMLMessageConverter;
import org.geoserver.ogcapi.FreemarkerTemplateSupport;
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

    public HTMLMapMessageConverter(FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(HTMLMap.class, WMSInfo.class, templateSupport, geoServer);
    }

    @Override
    protected void writeInternal(HTMLMap htmlMap, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        HashMap<String, Object> model = setupModel(htmlMap.getMapContent().getRequest());
        model.put("units", getUnits(htmlMap.getMapContent()));
        APIRequestInfo ri = APIRequestInfo.get();
        HttpServletRequest request = ri.getRequest();
        model.put("url", request.getRequestURL());
        templateSupport.processTemplate(
                null,
                "htmlmap.ftl",
                MapsService.class,
                model,
                new OutputStreamWriter(outputMessage.getBody()));
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
}
