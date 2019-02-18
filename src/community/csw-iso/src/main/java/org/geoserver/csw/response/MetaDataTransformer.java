/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.util.Collection;
import java.util.List;
import net.opengis.cat.csw20.RequestBaseType;
import org.geoserver.csw.records.AbstractRecordDescriptor;
import org.geoserver.csw.records.GenericRecordBuilder;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.platform.ServiceException;
import org.geotools.data.complex.util.ComplexFeatureConstants;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a FeatureCollection containing {@link MetaDataDescriptor} features into the specified XML
 * according to the chosen profile, brief, summary or full
 *
 * @author Niels Charlier
 */
public class MetaDataTransformer extends AbstractRecordTransformer {

    static final String CSW_ROOT_LOCATION = "http://schemas.opengis.net/csw/2.0.2/";

    public MetaDataTransformer(RequestBaseType request, boolean canonicalSchemaLocation) {
        super(request, canonicalSchemaLocation, MetaDataDescriptor.NAMESPACES);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new MetaDataTranslator(handler);
    }

    @Override
    public boolean canHandleRespose(CSWRecordsResult response) {
        return true;
    }

    class MetaDataTranslator extends AbstractRecordTranslator {

        public MetaDataTranslator(ContentHandler handler) {
            super(handler);
        }

        public void encode(CSWRecordsResult response, Feature f) {
            encodeProperty(f, f);
        }

        protected void encodeProperties(Feature f, Collection<Property> properties) {
            if (properties != null) {
                for (Property property : properties) {
                    encodeProperty(f, property);
                }
            }
        }

        private void encodeProperty(Feature f, Property p) {
            if (p instanceof ComplexAttribute) {

                String prefix =
                        MetaDataDescriptor.NAMESPACES.getPrefix(p.getName().getNamespaceURI());

                AttributesImpl atts = new AttributesImpl();
                for (Property p2 : ((ComplexAttribute) p).getProperties()) {
                    if (p2.getName().getLocalPart().substring(0, 1).equals("@")) {
                        String name = p2.getName().getLocalPart().substring(1);
                        atts.addAttribute("", name, name, "", p2.getValue().toString());
                    }
                }

                start(prefix + ":" + p.getName().getLocalPart(), atts);

                Property pSimple =
                        ((ComplexAttribute) p).getProperty(ComplexFeatureConstants.SIMPLE_CONTENT);
                if (pSimple != null) {
                    chars(pSimple.getValue().toString());
                }

                for (PropertyDescriptor pd : ((ComplexType) p.getType()).getDescriptors()) {
                    if (!pd.getName().getLocalPart().substring(0, 1).equals("@")) {
                        encodeProperties(f, ((ComplexAttribute) p).getProperties(pd.getName()));
                        @SuppressWarnings("unchecked")
                        Collection<PropertyDescriptor> substitionGroup =
                                (Collection<PropertyDescriptor>)
                                        pd.getUserData().get("substitutionGroup");
                        if (substitionGroup != null) {
                            for (PropertyDescriptor pdSub : substitionGroup) {
                                encodeProperties(
                                        f, ((ComplexAttribute) p).getProperties(pdSub.getName()));
                            }
                        }
                    }
                }

                end(prefix + ":" + p.getName().getLocalPart());

            } else if (MetaDataDescriptor.RECORD_BBOX_NAME.equals(p.getName())) {

                // grab the original bounding boxes from the user data (the geometry is an
                // aggregate)
                List<ReferencedEnvelope> originalBoxes =
                        (List<ReferencedEnvelope>)
                                p.getUserData().get(GenericRecordBuilder.ORIGINAL_BBOXES);
                for (ReferencedEnvelope re : originalBoxes) {
                    try {
                        ReferencedEnvelope wgs84re =
                                re.transform(
                                        CRS.decode(AbstractRecordDescriptor.DEFAULT_CRS_NAME),
                                        true);

                        String minx = String.valueOf(wgs84re.getMinX());
                        String miny = String.valueOf(wgs84re.getMinY());
                        String maxx = String.valueOf(wgs84re.getMaxX());
                        String maxy = String.valueOf(wgs84re.getMaxY());

                        start("gmd:EX_GeographicBoundingBox");
                        start("gmd:westBoundLongitude");
                        element("gco:Decimal", miny);
                        end("gmd:westBoundLongitude");
                        start("gmd:eastBoundLongitude");
                        element("gco:Decimal", maxy);
                        end("gmd:eastBoundLongitude");
                        start("gmd:southBoundLatitude");
                        element("gco:Decimal", minx);
                        end("gmd:southBoundLatitude");
                        start("gmd:northBoundLatitude");
                        element("gco:Decimal", maxx);
                        end("gmd:northBoundLatitude");
                        end("gmd:EX_GeographicBoundingBox");
                    } catch (Exception e) {
                        throw new ServiceException("Failed to encode the current record: " + f, e);
                    }
                }

            } else {
                encodeSimpleLiteral(p);
            }
        }

        private void encodeSimpleLiteral(Property p) {
            String value = p.getValue().toString();
            Name dn = p.getDescriptor().getName();
            String name = dn.getLocalPart();
            String prefix = MetaDataDescriptor.NAMESPACES.getPrefix(dn.getNamespaceURI());
            AttributesImpl attributes = new AttributesImpl();
            element(prefix + ":" + name, value, attributes);
        }
    }
}
