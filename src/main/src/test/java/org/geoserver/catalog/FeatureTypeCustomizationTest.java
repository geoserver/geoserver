/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.geoserver.data.test.CiteTestData.PRIMITIVEGEOFEATURE;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class FeatureTypeCustomizationTest extends GeoServerSystemTestSupport {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    private static final Filter ID_03 = FF.id(FF.featureId("PrimitiveGeoFeature.f003"));
    private static final String BOOLEAN_PROPERTY = "booleanProperty";
    private static final String CURVE_PROPERTY = "curveProperty";
    private static final String TRUE_OR_FALSE = "trueOrFalse";

    @Before
    public void resetFeatureTypes() throws IOException {
        revertLayer(PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testHideAttributes() throws IOException {
        // setup
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        Set<String> exclude = new HashSet<>(Arrays.asList(CURVE_PROPERTY, "uriProperty"));
        List<AttributeTypeInfo> filteredAttributes =
                fti.attributes().stream()
                        .filter(at -> !exclude.contains(at.getName()))
                        .collect(Collectors.toList());
        fti.getAttributes().clear();
        fti.getAttributes().addAll(filteredAttributes);
        catalog.save(fti);

        // test feature type
        FeatureType schema = fti.getFeatureType();
        assertNull(schema.getDescriptor(CURVE_PROPERTY));
        assertNotNull(schema.getDescriptor("name"));

        // test the feature source
        SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
        assertEquals(fs.getSchema(), schema);
        // grab a feature that used to have the curve property
        SimpleFeature feature = DataUtilities.first(fs.getFeatures(ID_03));
        assertNull(feature.getAttribute(CURVE_PROPERTY));
        assertEquals("name-f003", feature.getAttribute("name"));

        // did not lose the ability to write
        assertThat(fs, CoreMatchers.instanceOf(SimpleFeatureStore.class));
    }

    @Test
    public void testChangeBinding() throws Exception {
        // switch one property type from boolean to string
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        List<AttributeTypeInfo> filteredAttributes =
                fti.attributes().stream()
                        .map(
                                at -> {
                                    if (BOOLEAN_PROPERTY.equals(at.getName()))
                                        at.setBinding(String.class);
                                    return at;
                                })
                        .collect(Collectors.toList());
        fti.getAttributes().clear();
        fti.getAttributes().addAll(filteredAttributes);
        catalog.save(fti);

        // test attribute type has been remapped
        FeatureType schema = fti.getFeatureType();
        assertNotNull(schema.getDescriptor(CURVE_PROPERTY));
        PropertyDescriptor booleanProperty = schema.getDescriptor(BOOLEAN_PROPERTY);
        assertNotNull(booleanProperty);
        assertEquals(String.class, booleanProperty.getType().getBinding());

        // test the feature source
        SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
        assertEquals(fs.getSchema(), schema);
        SimpleFeature feature = DataUtilities.first(fs.getFeatures(ID_03));
        assertNotNull(feature.getAttribute(CURVE_PROPERTY));
        assertEquals("name-f003", feature.getAttribute("name"));
        // testing it's the "true" string, not Boolean.TRUE
        assertEquals("true", feature.getAttribute(BOOLEAN_PROPERTY));

        // did not lose the ability to write (debatable)
        assertThat(fs, CoreMatchers.instanceOf(SimpleFeatureStore.class));
    }

    @Test
    public void testRename() throws Exception {
        // rename one property
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        List<AttributeTypeInfo> filteredAttributes =
                fti.attributes().stream()
                        .map(
                                at -> {
                                    if (BOOLEAN_PROPERTY.equals(at.getName())) {
                                        at.setName(TRUE_OR_FALSE);
                                        at.setSource(BOOLEAN_PROPERTY);
                                    }
                                    return at;
                                })
                        .collect(Collectors.toList());
        fti.getAttributes().clear();
        fti.getAttributes().addAll(filteredAttributes);
        catalog.save(fti);

        // test attribute type has been remapped
        FeatureType schema = fti.getFeatureType();
        assertNull(schema.getDescriptor(BOOLEAN_PROPERTY));
        PropertyDescriptor trueFalseProperty = schema.getDescriptor(TRUE_OR_FALSE);
        assertNotNull(trueFalseProperty);
        assertEquals(Boolean.class, trueFalseProperty.getType().getBinding());

        // test the feature source
        SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
        assertEquals(fs.getSchema(), schema);
        SimpleFeature feature = DataUtilities.first(fs.getFeatures(ID_03));
        assertEquals("name-f003", feature.getAttribute("name"));
        assertNull(feature.getAttribute(BOOLEAN_PROPERTY)); // not found
        assertEquals(Boolean.TRUE, feature.getAttribute(TRUE_OR_FALSE));

        // did not lose the ability to write (debatable)
        assertThat(fs, CoreMatchers.instanceOf(SimpleFeatureStore.class));
    }

    @Test
    public void testExpression() throws Exception {
        // rename one property
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        List<AttributeTypeInfo> attributes = new ArrayList<>(fti.attributes());
        AttributeTypeInfo prefixedNameAttribute = catalog.getFactory().createAttribute();
        final String PREFIXED_NAME = "prefixedName";
        prefixedNameAttribute.setName(PREFIXED_NAME);
        prefixedNameAttribute.setSource("Concatenate('gs-', name)");
        attributes.add(prefixedNameAttribute);
        fti.getAttributes().clear();
        fti.getAttributes().addAll(attributes);
        catalog.save(fti);

        // test attribute type has been added
        FeatureType schema = fti.getFeatureType();
        PropertyDescriptor prefixedProperty = schema.getDescriptor(PREFIXED_NAME);
        assertNotNull(prefixedProperty);
        assertEquals(String.class, prefixedProperty.getType().getBinding());

        // test the feature source
        SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
        assertEquals(fs.getSchema(), schema);
        SimpleFeature feature = DataUtilities.first(fs.getFeatures(ID_03));
        assertEquals("name-f003", feature.getAttribute("name"));
        assertEquals("gs-name-f003", feature.getAttribute(PREFIXED_NAME));

        // filtering on the concatenated string also works
        Filter filter = FF.equals(FF.property(PREFIXED_NAME), FF.literal("gs-name-f003"));
        SimpleFeature filteredFeature = DataUtilities.first(fs.getFeatures(filter));
        assertEquals("name-f003", filteredFeature.getAttribute("name"));

        // still writable, can compute all mandatory attributes
        assertThat(fs, CoreMatchers.instanceOf(SimpleFeatureStore.class));
    }

    @Test
    public void testDuplicateAttributes() throws Exception {
        // add one duplicate attribute
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        List<AttributeTypeInfo> attributes = new ArrayList<>(fti.attributes());
        AttributeTypeInfo duplicate = catalog.getFactory().createAttribute();
        String name = attributes.get(0).getName();
        duplicate.setName(name);
        attributes.add(duplicate);
        fti.getAttributes().clear();
        fti.getAttributes().addAll(attributes);
        try {
            catalog.save(fti);
        } catch (ValidationException e) {
            assertThat(
                    e.getMessage(),
                    allOf(containsString("multiple definitions"), containsString("description")));
        }
    }

    @Test
    public void testInvalidCQL() throws Exception {
        // use an invalid CQL expression, with unbalanced literals
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        List<AttributeTypeInfo> attributes = new ArrayList<>(fti.attributes());
        AttributeTypeInfo wrongExpression = catalog.getFactory().createAttribute();
        wrongExpression.setName("wrongCQL");
        wrongExpression.setSource("Concatenate(name, 'incomplete)");
        attributes.add(wrongExpression);
        fti.getAttributes().clear();
        fti.getAttributes().addAll(attributes);
        try {
            catalog.save(fti);
        } catch (ValidationException e) {
            assertThat(e.getMessage(), containsString("Invalid CQL"));
        }
    }

    @Test
    public void testAttributeNotFound() throws Exception {
        // use an invalid CQL expression, with unbalanced literals
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        List<AttributeTypeInfo> attributes = new ArrayList<>(fti.attributes());
        AttributeTypeInfo attributeNotThere = catalog.getFactory().createAttribute();
        attributeNotThere.setName("test");
        attributeNotThere.setSource("Concatenate(one, two, 'suffix')");
        attributes.add(attributeNotThere);
        fti.getAttributes().clear();
        fti.getAttributes().addAll(attributes);
        try {
            catalog.save(fti);
        } catch (ValidationException e) {
            assertThat(
                    e.getMessage(),
                    allOf(containsString("attributes unavailable"), containsString("one, two")));
        }
    }

    @Test
    public void testInvalidConversion() throws Exception {
        // change one attribute so that it has an inconvertible target type
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(PRIMITIVEGEOFEATURE));
        List<AttributeTypeInfo> attributes = new ArrayList<>(fti.attributes());
        attributes.get(0).setBinding(MyOddBinding.class);
        fti.getAttributes().clear();
        fti.getAttributes().addAll(attributes);
        try {
            catalog.save(fti);
        } catch (ValidationException e) {
            assertThat(
                    e.getMessage(),
                    allOf(
                            containsString("unable to convert"),
                            containsString("java.lang.String"),
                            containsString("$MyOddBinding")));
        }
    }

    /** A class that {@link org.geotools.util.Converters} can't know how to handle */
    private final class MyOddBinding {}
}
