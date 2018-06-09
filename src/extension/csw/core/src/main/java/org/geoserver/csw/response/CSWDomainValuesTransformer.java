/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import net.opengis.cat.csw20.GetDomainType;
import net.opengis.cat.csw20.RequestBaseType;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.ows.v1_1.OWS;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a CloseableIterator<String> containing domain values into the specified XML Domain
 * Response
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class CSWDomainValuesTransformer extends AbstractRecordTransformer {

    static final String CSW_ROOT_LOCATION = "http://schemas.opengis.net/csw/2.0.2/";

    public CSWDomainValuesTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        super(request, canonicalSchemaLocation, null);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new CSWDomainValueTranslator(handler);
    }

    @Override
    public boolean canHandleRespose(CSWRecordsResult response) {
        return true;
    }

    class CSWDomainValueTranslator extends TranslatorSupport {

        public CSWDomainValueTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            CloseableIterator<String> response = (CloseableIterator<String>) o;

            AttributesImpl attributes = new AttributesImpl();
            addAttribute(attributes, "xmlns:csw", CSW.NAMESPACE);
            addAttribute(attributes, "xmlns:dc", DC.NAMESPACE);
            addAttribute(attributes, "xmlns:dct", DCT.NAMESPACE);
            addAttribute(attributes, "xmlns:ows", OWS.NAMESPACE);
            addAttribute(attributes, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

            String locationAtt = "xsi:schemaLocation";
            StringBuilder locationDef = new StringBuilder();
            locationDef.append(CSW.NAMESPACE).append(" ");
            locationDef.append(cswSchemaLocation("CSW-discovery.xsd"));
            addAttribute(attributes, locationAtt, locationDef.toString());

            start("csw:GetDomainResponse", attributes);

            String domainValuesElement = "csw:DomainValues";
            AttributesImpl domainValuesElementAtts = new AttributesImpl();
            addAttribute(domainValuesElementAtts, "type", "csw:Record");
            start(domainValuesElement, domainValuesElementAtts);

            if (((GetDomainType) request).getParameterName() != null
                    && !((GetDomainType) request).getParameterName().isEmpty()) {
                String parameterNameElement = "csw:ParameterName";
                element(parameterNameElement, ((GetDomainType) request).getParameterName());
            } else if (((GetDomainType) request).getPropertyName() != null
                    && !((GetDomainType) request).getPropertyName().isEmpty()) {
                String propertyNameElement = "csw:PropertyName";
                element(propertyNameElement, ((GetDomainType) request).getPropertyName());
            }

            String valuesElementType = "csw:ListOfValues";
            start(valuesElementType);

            while (response.hasNext()) {
                String value = response.next();
                element("csw:Value", value);
            }

            end(valuesElementType);

            end(domainValuesElement);
            end("csw:GetDomainResponse");
        }
    }

    public void addAttribute(AttributesImpl attributes, String name, Object value) {
        if (value != null) {
            attributes.addAttribute(
                    "",
                    name,
                    name,
                    "",
                    value instanceof String ? (String) value : String.valueOf(value));
        }
    }

    private String cswSchemaLocation(String schema) {
        if (canonicalSchemaLocation) {
            return CSW_ROOT_LOCATION + schema;
        } else {
            return buildSchemaURL(request.getBaseUrl(), "csw/2.0.2/" + schema);
        }
    }
}
