package org.geoserver.csw.records;

import java.util.Collection;

import junit.framework.TestCase;

import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.LenientFeatureFactoryImpl;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;

public class RecordsTest extends TestCase {

    /**
     * Trying to build
     * 
     * <code>
     * <?xml version="1.0" encoding="ISO-8859-1"?>
       <Record
          xmlns="http://www.opengis.net/cat/csw/2.0.2"
          xmlns:dc="http://purl.org/dc/elements/1.1/"
          xmlns:dct="http://purl.org/dc/terms/"
          xmlns:ows="http://www.opengis.net/ows"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.opengis.net/cat/csw/2.0.2
                              ../../../csw/2.0.2/record.xsd">
          <dc:identifier>00180e67-b7cf-40a3-861d-b3a09337b195</dc:identifier>
          <dc:title>Image2000 Product 1 (at1) Multispectral</dc:title>
          <dct:modified>2004-10-04 00:00:00</dct:modified>
          <dct:abstract>IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.</dct:abstract>
          <dc:type>dataset</dc:type>
          <dc:subject>imagery</dc:subject>
          <dc:subject>baseMaps</dc:subject>
          <dc:subject>earthCover</dc:subject>
          <dc:format>BIL</dc:format>
          <dc:creator>Vanda Lima</dc:creator>
          <dc:language>en</dc:language>
          <ows:WGS84BoundingBox>
             <ows:LowerCorner>14.05 46.46</ows:LowerCorner>
             <ows:UpperCorner>17.24 48.42</ows:UpperCorner>
          </ows:WGS84BoundingBox>
        </Record>
       </code>
     */
    public void testBuildCSWRecord() {
        ComplexFeatureBuilder fb = new ComplexFeatureBuilder(CSWRecordTypes.RECORD);
        AttributeBuilder ab = new AttributeBuilder(new LenientFeatureFactoryImpl());
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "identifier", "00180e67-b7cf-40a3-861d-b3a09337b195"));
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "title", "Image2000 Product 1 (at1) Multispectral"));
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "modified", "2004-10-04 00:00:00"));
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "abstract", "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems."));
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "type", "dataset"));
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "subject", "imagery"));
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "subject", "baseMaps"));
        fb.append(CSWRecordTypes.DC_ELEMENT_NAME,
                buildRecordElement(ab, "subject", "earthCover"));
        
        Feature f = fb.buildFeature(null);

        assertRecordElement(f, "identifier", "00180e67-b7cf-40a3-861d-b3a09337b195");
        assertRecordElement(f, "title", "Image2000 Product 1 (at1) Multispectral");
        assertRecordElement(f, "modified", "2004-10-04 00:00:00");
        assertRecordElement(f, "abstract", "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.");
        assertRecordElement(f, "type", "dataset");
        assertRecordElement(f, "subject", "imagery", "baseMaps", "earthCover");
    }

    private void assertRecordElement(Feature f, String elementName, Object... values) {
        AttributeDescriptor identifierDescriptor = getDescriptor(elementName);
        Collection<Property> propertyList = f.getProperties(identifierDescriptor.getName());
        Property[] properties = (Property[]) propertyList.toArray(new Property[propertyList.size()]);
        assertEquals(properties.length, values.length);
        for (int i = 0; i < properties.length; i++) {
            ComplexAttribute cad = (ComplexAttribute) properties[i];
            assertEquals(identifierDescriptor, cad.getDescriptor());
            assertEquals(values[i], cad.getProperty(CSWRecordTypes.SIMPLE_LITERAL_VALUE).getValue());

        }
    }

    private Attribute buildRecordElement(AttributeBuilder ab, String elementName, Object value) {
        AttributeDescriptor descriptor = getDescriptor(elementName);
        ab.setDescriptor(descriptor);
        ab.add(null, value, CSWRecordTypes.SIMPLE_LITERAL_VALUE);
        return ab.build();
    }

    private AttributeDescriptor getDescriptor(String elementName) {
        AttributeDescriptor identifierDescriptor = CSWRecordTypes.DC_DESCRIPTORS.get(elementName);
        if (identifierDescriptor == null) {
            identifierDescriptor = CSWRecordTypes.DCT_DESCRIPTORS.get(elementName);
        }
        return identifierDescriptor;
    }
}
