/* (c) 2013 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class FeatureDataConverterTest {

    @Test
    public void testXMLUnsafeAttributeRenaming() {
        SimpleFeatureType badatts = buildFeatureTypeWithXMLUnsafeAtts();
        badatts = FeatureDataConverter.DEFAULT.convertType(badatts, null, null, null);

        assertEquals("_123_number_first", badatts.getAttributeDescriptors().get(0).getLocalName());
        assertEquals("i_has_spaces", badatts.getAttributeDescriptors().get(1).getLocalName());
    }

    @Test
    public void testPostgisConversion() {
        SimpleFeatureType t =
                FeatureDataConverter.TO_POSTGIS.convertType(
                        buildFeatureTypeWithXMLUnsafeAtts(), null, null, null);
        assertEquals("_123_number_first", t.getAttributeDescriptors().get(0).getLocalName());
        assertEquals("i_has_spaces", t.getAttributeDescriptors().get(1).getLocalName());
    }

    SimpleFeatureType buildFeatureTypeWithXMLUnsafeAtts() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("badatts");

        AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
        attBuilder.setBinding(String.class);
        typeBuilder.add(attBuilder.buildDescriptor("123_number_first"));
        attBuilder.setBinding(String.class);
        typeBuilder.add(attBuilder.buildDescriptor("i has spaces"));

        return typeBuilder.buildFeatureType();
    }

    @Test
    public void testLayerNameFromTask() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("badname");
        SimpleFeatureType badname = typeBuilder.buildFeatureType();

        ImportTask task = new ImportTask();
        LayerInfo layer = new LayerInfoImpl();
        ResourceInfo resource = new FeatureTypeInfoImpl((Catalog) null);
        layer.setResource(resource);
        layer.setName("goodname");
        task.setLayer(layer);

        badname = FeatureDataConverter.DEFAULT.convertType(badname, null, null, task);

        assertEquals("goodname", badname.getName().getLocalPart());
    }
}
