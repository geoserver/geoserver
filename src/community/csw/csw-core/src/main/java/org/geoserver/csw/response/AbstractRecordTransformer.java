/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;

import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordByIdType;
import net.opengis.cat.csw20.RequestBaseType;

import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.ows.OWS;
import org.geotools.util.Converters;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a FeatureCollection containing {@link CSWRecordDescriptor#RECORD} features into the specified
 * XML according to the chosen profile, brief, summary or full
 * 
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractRecordTransformer extends AbstractCSWTransformer {

    public AbstractRecordTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        super(request, canonicalSchemaLocation);
    }
    
    /**
     * Returns true if the specified response can be handled by this transformer (it should
     * check the requested schema and the feature's type)
     */
    public abstract boolean canHandleRespose(CSWRecordsResult response);

    abstract class AbstractRecordTranslator extends AbstractCSWTranslator {

        public AbstractRecordTranslator(ContentHandler handler) {
            super(handler);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            final CSWRecordsResult response = (CSWRecordsResult) o;

            AttributesImpl attributes = new AttributesImpl();
            addAttribute(attributes, "xmlns:csw", CSW.NAMESPACE);
            addAttribute(attributes, "xmlns:dc", DC.NAMESPACE);
            addAttribute(attributes, "xmlns:dct", DCT.NAMESPACE);
            addAttribute(attributes, "xmlns:ows", OWS.NAMESPACE);
            addAttribute(attributes, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

            if(request instanceof GetRecordByIdType) {
                String locationAtt = "xsi:schemaLocation";
                StringBuilder locationDef = new StringBuilder();
                locationDef.append(CSW.NAMESPACE).append(" ");
                locationDef.append(cswSchemaLocation("CSW-discovery.xsd"));
                addAttribute(attributes, locationAtt, locationDef.toString());

                start("csw:GetRecordByIdResponse", attributes);
                encodeRecords(response);
                end("csw:GetRecordByIdResponse");
            } else {
                addAttribute(attributes, "version", "2.0.2");
                String locationAtt = "xsi:schemaLocation";
                StringBuilder locationDef = new StringBuilder();
                locationDef.append(CSW.NAMESPACE).append(" ");
                locationDef.append(cswSchemaLocation("record.xsd"));
                addAttribute(attributes, locationAtt, locationDef.toString());

                start("csw:GetRecordsResponse", attributes);

                attributes = new AttributesImpl();
                addAttribute(attributes, "timestamp",
                        Converters.convert(response.getTimestamp(), String.class));
                element("csw:SearchStatus", null, attributes);
                
                if(response.getElementSet() == null) {
                    response.setElementSet(ElementSetType.FULL);
                }

                attributes = new AttributesImpl();
                addAttribute(attributes, "numberOfRecordsMatched", response.getNumberOfRecordsMatched());
                addAttribute(attributes, "numberOfRecordsReturned",
                        response.getNumberOfRecordsReturned());
                addAttribute(attributes, "nextRecord", response.getNextRecord());
                addAttribute(attributes, "recordSchema", response.getRecordSchema());
                addAttribute(attributes, "elementSet", response.getElementSet());
                start("csw:SearchResults", attributes);

                encodeRecords(response);

                end("csw:SearchResults");
                end("csw:GetRecordsResponse");
            }

        }

        private void encodeRecords(final CSWRecordsResult response) {
            // encode the records
            if(response.getRecords() != null) {
                try {
                    response.getRecords().accepts(new FeatureVisitor() {
      
                        @Override
                        public void visit(Feature feature) {
                            encode(response, feature);
                        }
                    }, null);
                } catch (IOException e) {
                    throw new ServiceException("Failed to encoder records", e);
                }
            }
        }

        /**
         * Encodes the feature in the desired xml format (e.g., csw:Record, ISO, ebRIM)
         * 
         * @param response
         * @param f
         */
        protected abstract void encode(CSWRecordsResult response, Feature f);

    }

}
