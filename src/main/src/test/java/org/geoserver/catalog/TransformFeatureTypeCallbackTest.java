/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.geoserver.data.test.CiteTestData.PRIMITIVEGEOFEATURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.util.SimpleInternationalString;
import org.junit.Before;
import org.junit.Test;

public class TransformFeatureTypeCallbackTest extends GeoServerSystemTestSupport {
    private static final String BOOLEAN_PROPERTY = "booleanProperty";
    private TransformFeatureTypeCallback transformer;
    private Catalog catalog;
    FeatureTypeInfo fti;
    SimpleFeatureSource notWrappedFs;

    @Before
    public void setUp() throws Exception {
        transformer = new TransformFeatureTypeCallback();
        catalog = getCatalog();
        fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        notWrappedFs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
    }

    @Test
    public void testForDescription() throws Exception {
        SimpleFeatureType notWrappedSchema = notWrappedFs.getSchema();
        assertNull(notWrappedSchema.getDescriptor(BOOLEAN_PROPERTY).getType().getDescription());
        List<AttributeTypeInfo> filteredAttributes =
                fti.attributes().stream()
                        .map(
                                at -> {
                                    if (BOOLEAN_PROPERTY.equals(at.getName()))
                                        at.setDescription(
                                                new SimpleInternationalString(
                                                        "A boolean property"));
                                    return at;
                                })
                        .collect(Collectors.toList());
        SimpleFeatureType wrappedSchema = getWrappedFeatureType(notWrappedFs, filteredAttributes);
        assertEquals(
                "A boolean property",
                wrappedSchema
                        .getDescriptor(BOOLEAN_PROPERTY)
                        .getType()
                        .getDescription()
                        .toString());
    }

    @Test
    public void testBlankDescriptionIsNull() throws Exception {
        List<AttributeTypeInfo> filteredAttributes =
                fti.attributes().stream()
                        .map(
                                at -> {
                                    if (BOOLEAN_PROPERTY.equals(at.getName()))
                                        at.setDescription(new SimpleInternationalString(""));
                                    return at;
                                })
                        .collect(Collectors.toList());
        SimpleFeatureType wrappedSchema = getWrappedFeatureType(notWrappedFs, filteredAttributes);
        assertNull(wrappedSchema.getDescriptor(BOOLEAN_PROPERTY).getType().getDescription());
    }

    private SimpleFeatureType getWrappedFeatureType(
            SimpleFeatureSource notWrappedFs, List<AttributeTypeInfo> filteredAttributes)
            throws IOException {
        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(null);
        resource.setName("alias");
        resource.setAttributes(filteredAttributes);
        SimpleFeatureSource wrappedFs =
                (SimpleFeatureSource) transformer.wrapFeatureSource(resource, notWrappedFs);
        SimpleFeatureType wrappedSchema = wrappedFs.getSchema();
        return wrappedSchema;
    }
}
