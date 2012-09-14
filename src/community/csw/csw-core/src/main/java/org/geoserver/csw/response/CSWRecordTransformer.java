/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.util.Collection;
import java.util.Set;

import net.opengis.cat.csw20.RequestBaseType;

import org.geoserver.csw.records.CSWRecordTypes;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.Name;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a FeatureCollection containing {@link CSWRecordTypes#RECORD} features into the specified
 * XML according to the chosen profile, brief, summary or full
 * 
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordTransformer extends AbstractRecordTransformer {

    static final String CSW_ROOT_LOCATION = "http://schemas.opengis.net/csw/2.0.2/";

    public CSWRecordTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        super(request, canonicalSchemaLocation);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new CSWRecordTranslator(handler);
    }
    
    @Override
    public boolean canHandleRespose(CSWRecordsResult response) {
        return true;
    }

    class CSWRecordTranslator extends AbstractRecordTranslator {

        public CSWRecordTranslator(ContentHandler handler) {
            super(handler);
        }

        public void encode(CSWRecordsResult response, Feature f) {
            String element = "csw:" + getRecordElement(response);
            start(element);
            Set<Name> elements = getElements(response);
            
            // encode all elements besides bbox
            for (Property p : f.getProperties()) {
                if (elements == null || elements.contains(p.getName())) {
                    if (p.getType() == CSWRecordTypes.SIMPLE_LITERAL) {
                        ComplexAttribute sl = (ComplexAttribute) p;
                        String scheme = (String) sl.getProperty("scheme").getValue();
                        String value = (String) sl.getProperty("value").getValue();
                        Name dn = p.getDescriptor().getName();
                        String name = dn.getLocalPart();
                        String prefix = CSWRecordTypes.NAMESPACES.getPrefix(dn.getNamespaceURI());
                        if (scheme == null) {
                            element(prefix + ":" + name, value);
                        } else {
                            AttributesImpl attributes = new AttributesImpl();
                            addAttribute(attributes, "scheme", scheme);
                            element(prefix + ":" + name, value, attributes);
                        }
                    } else if (CSWRecordTypes.RECORD_BBOX_NAME.equals(p.getName())) {
                        // skip it for the moment, it is constrained to be last
                    } else if(CSWRecordTypes.RECORD_GEOMETRY_NAME.equals(p.getName())) {
                        // skip it, we only use it for filtering
                    } else {
                        throw new IllegalArgumentException("Don't know how to encode property " + p
                                + " in record " + f);
                    }
                }
            }
            
            // encode the bbox if present
            if(elements == null || elements.contains(CSWRecordTypes.RECORD_BBOX_NAME)) {
                Collection<Property> bboxes = f.getProperties(CSWRecordTypes.RECORD_BBOX_NAME);
                if(bboxes != null) {
                    for (Property p : bboxes) {
                        try {
                            ReferencedEnvelope re = (ReferencedEnvelope) p.getValue();
                            ReferencedEnvelope wgs84re = re.transform(
                                    CRS.decode("EPSG:4326", true), true);

                            String minx = String.valueOf(wgs84re.getMinX());
                            String miny = String.valueOf(wgs84re.getMinY());
                            String maxx = String.valueOf(wgs84re.getMaxX());
                            String maxy = String.valueOf(wgs84re.getMaxY());

                            start("ows:WGS84BoundingBox");
                            element("ows:LowerCorner", minx + " " + miny);
                            element("ows:UpperCorner", maxx + " " + maxy);
                            end("ows:WGS84BoundingBox");
                        } catch (Exception e) {
                            throw new ServiceException("Failed to encode the current record: " + f,
                                    e);
                        }
                    }
                }

            }
            end(element);
        }

        private String getRecordElement(CSWRecordsResult response) {
            switch (response.getElementSet()) {
            case BRIEF:
                return "BriefRecord";
            case SUMMARY:
                return "SummaryRecord";
            default:
                return "Record";
            }
        }

        private Set<Name> getElements(CSWRecordsResult response) {
            switch (response.getElementSet()) {
            case BRIEF:
                return CSWRecordTypes.BRIEF_ELEMENTS;
            case SUMMARY:
                return CSWRecordTypes.SUMMARY_ELEMENTS;
            default:
                return null;
            }
        }

    }

    

}
