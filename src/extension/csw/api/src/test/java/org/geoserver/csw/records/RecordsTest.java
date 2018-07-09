/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.Collection;
import java.util.List;
import junit.framework.TestCase;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;

public class RecordsTest extends TestCase {

    /**
     * Trying to build <code>
     * <?xml version="1.0" encoding="ISO-8859-1"?>
     * <Record
     * xmlns="http://www.opengis.net/cat/csw/2.0.2"
     * xmlns:dc="http://purl.org/dc/elements/1.1/"
     * xmlns:dct="http://purl.org/dc/terms/"
     * xmlns:ows="http://www.opengis.net/ows"
     * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     * xsi:schemaLocation="http://www.opengis.net/cat/csw/2.0.2
     * ../../../csw/2.0.2/record.xsd">
     * <dc:identifier>00180e67-b7cf-40a3-861d-b3a09337b195</dc:identifier>
     * <dc:title>Image2000 Product 1 (at1) Multispectral</dc:title>
     * <dct:modified>2004-10-04 00:00:00</dct:modified>
     * <dct:abstract>IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.</dct:abstract>
     * <dc:type>dataset</dc:type>
     * <dc:subject>imagery</dc:subject>
     * <dc:subject>baseMaps</dc:subject>
     * <dc:subject>earthCover</dc:subject>
     * <dc:format>BIL</dc:format>
     * <dc:creator>Vanda Lima</dc:creator>
     * <dc:language>en</dc:language>
     * <ows:WGS84BoundingBox>
     * <ows:LowerCorner>14.05 46.46</ows:LowerCorner>
     * <ows:UpperCorner>17.24 48.42</ows:UpperCorner>
     * </ows:WGS84BoundingBox>
     * </Record>
     * </code>
     */
    public void testBuildCSWRecord() throws Exception {
        CSWRecordBuilder rb = new CSWRecordBuilder();
        rb.addElement("identifier", "00180e67-b7cf-40a3-861d-b3a09337b195");
        rb.addElement("title", "Image2000 Product 1 (at1) Multispectral");
        rb.addElement("modified", "2004-10-04 00:00:00");
        rb.addElement(
                "abstract",
                "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.");
        rb.addElement("type", "dataset");
        rb.addElement("subject", "imagery", "baseMaps", "earthCover");
        rb.addBoundingBox(
                new ReferencedEnvelope(14.05, 17.24, 46.46, 28.42, DefaultGeographicCRS.WGS84));
        Feature f = rb.build(null);

        assertRecordElement(f, "identifier", "00180e67-b7cf-40a3-861d-b3a09337b195");
        assertRecordElement(f, "title", "Image2000 Product 1 (at1) Multispectral");
        assertRecordElement(f, "modified", "2004-10-04 00:00:00");
        assertRecordElement(
                f,
                "abstract",
                "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.");
        assertRecordElement(f, "type", "dataset");
        assertRecordElement(f, "subject", "imagery", "baseMaps", "earthCover");
        assertBBox(
                f, new ReferencedEnvelope(14.05, 17.24, 46.46, 28.42, DefaultGeographicCRS.WGS84));
    }

    private void assertBBox(Feature f, ReferencedEnvelope... envelopes) throws Exception {
        Property p = f.getProperty(CSWRecordDescriptor.RECORD_BBOX_NAME);
        MultiPolygon geometry = (MultiPolygon) p.getValue();
        @SuppressWarnings("unchecked")
        List<ReferencedEnvelope> featureEnvelopes =
                (List<ReferencedEnvelope>)
                        p.getUserData().get(GenericRecordBuilder.ORIGINAL_BBOXES);
        ReferencedEnvelope total = null;
        for (int i = 0; i < envelopes.length; i++) {
            assertEquals(envelopes[i], featureEnvelopes.get(i));
            ReferencedEnvelope re = envelopes[i].transform(CSWRecordDescriptor.DEFAULT_CRS, true);
            if (total == null) {
                total = re;
            } else {
                total.expandToInclude(re);
            }
        }

        assertTrue(total.contains(geometry.getEnvelopeInternal()));
    }

    private void assertRecordElement(Feature f, String elementName, Object... values) {
        AttributeDescriptor identifierDescriptor = CSWRecordDescriptor.getDescriptor(elementName);
        Collection<Property> propertyList = f.getProperties(identifierDescriptor.getName());
        Property[] properties =
                (Property[]) propertyList.toArray(new Property[propertyList.size()]);
        assertEquals(properties.length, values.length);
        for (int i = 0; i < properties.length; i++) {
            ComplexAttribute cad = (ComplexAttribute) properties[i];
            assertEquals(identifierDescriptor, cad.getDescriptor());
            assertEquals(
                    values[i],
                    cad.getProperty(CSWRecordDescriptor.SIMPLE_LITERAL_VALUE).getValue());
        }
    }
}
