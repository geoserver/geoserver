/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.FakeTypes;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.LenientFeatureFactoryImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.FeatureTypeImpl;
import org.junit.Test;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.identity.Identifier;

public class ComplexGeoJsonWriterTest extends GeoServerSystemTestSupport {

    private static final ComplexType MINENAMETYPE_TYPE =
            new FeatureTypeImpl(
                    /* name: */ FakeTypes.Mine.NAME_MineNameType,
                    /* properties: */ FakeTypes.Mine.MINENAMETYPE_SCHEMA,
                    /* identified: */ null,
                    /* isAbstract: */ false,
                    /* restrictions: */ Collections.emptyList(),
                    /* superType: */ FakeTypes.ANYTYPE_TYPE,
                    /* description: */ null);

    private static final AttributeDescriptor MINENAME_DESCRIPTOR =
            new AttributeDescriptorImpl(
                    /* type: */ MINENAMETYPE_TYPE,
                    /* name: */ FakeTypes.Mine.NAME_MineName,
                    /* min: */ 1,
                    /* max: */ 1,
                    /* isNillable: */ false,
                    /* defaultValue: */ null);

    /*
     * Tests Null safety when attribute parameter is null in checkIfFeatureIsLinked util method.
     */
    @Test
    public void testCheckIfFeatureIsLinkedNullSafeAttr() {
        Property property = generateTestComplexAttribute();
        boolean checkIfFeatureIsLinked =
                ComplexGeoJsonWriter.checkIfFeatureIsLinked(property, null);
        assertFalse(checkIfFeatureIsLinked);
    }

    private ComplexAttribute generateTestComplexAttribute() {
        return new ComplexAttribute() {

            @Override
            public void setValue(Object newValue) {}

            @Override
            public boolean isNillable() {
                return false;
            }

            @Override
            public Map<Object, Object> getUserData() {
                return null;
            }

            @Override
            public Name getName() {
                return null;
            }

            @Override
            public Identifier getIdentifier() {
                return null;
            }

            @Override
            public AttributeDescriptor getDescriptor() {
                return null;
            }

            @Override
            public void validate() throws IllegalAttributeException {}

            @Override
            public void setValue(Collection<Property> values) {}

            @Override
            public Collection<? extends Property> getValue() {
                return null;
            }

            @Override
            public ComplexType getType() {
                return null;
            }

            @Override
            public Property getProperty(String name) {
                return null;
            }

            @Override
            public Property getProperty(Name name) {
                return null;
            }

            @Override
            public Collection<Property> getProperties(String name) {
                Property prop =
                        new Property() {

                            @Override
                            public void setValue(Object newValue) {}

                            @Override
                            public boolean isNillable() {
                                return false;
                            }

                            @Override
                            public Object getValue() {
                                return null;
                            }

                            @Override
                            public Map<Object, Object> getUserData() {
                                return null;
                            }

                            @Override
                            public PropertyType getType() {
                                return null;
                            }

                            @Override
                            public Name getName() {
                                return null;
                            }

                            @Override
                            public PropertyDescriptor getDescriptor() {
                                return null;
                            }
                        };
                ArrayList<Property> list = new ArrayList<>();
                list.add(prop);
                return list;
            }

            @Override
            public Collection<Property> getProperties(Name name) {
                return null;
            }

            @Override
            public Collection<Property> getProperties() {
                return null;
            }
        };
    }

    @Test
    public void testGeoJsonWriterOptions() {
        // test the default behaviour of the ComplexGeoJsonWriter
        // with respect to nested features
        Feature f = buildComplexFeature();
        StringWriter w = new StringWriter();
        GeoJSONBuilder jWriter = new GeoJSONBuilder(w);
        TestComplexGeoJsonWriter testWriter = new TestComplexGeoJsonWriter(jWriter);

        testWriter.encodeFeature(f);
        JSONObject jsonF = (JSONObject) JSONSerializer.toJSON(w.toString());
        JSONObject mineName = jsonF.getJSONObject("properties").getJSONObject("MineName");
        assertTrue(mineName.has("id"));
        assertTrue(mineName.has("geometry"));
        assertTrue(mineName.has("properties"));
        assertTrue(mineName.getJSONObject("properties").has("@featureType"));
    }

    @Test
    public void testGeoJsonWriterOptionsCustom() {
        // test behaviour of the ComplexGeoJsonWriter with a
        // ComplexGeoJsonWriterOptions object asking for the econding
        // of nested features as complex properties
        ComplexGeoJsonWriterOptions options =
                new ComplexGeoJsonWriterOptions() {
                    @Override
                    public boolean canHandle(List<FeatureCollection> features) {
                        return true;
                    }

                    @Override
                    public boolean encodeComplexAttributeType() {
                        return false;
                    }

                    @Override
                    public boolean encodeNestedFeatureAsProperty(ComplexType complexType) {
                        return true;
                    }
                };
        Feature f = buildComplexFeature();
        StringWriter w = new StringWriter();
        GeoJSONBuilder jWriter = new GeoJSONBuilder(w);
        TestComplexGeoJsonWriter testWriter = new TestComplexGeoJsonWriter(jWriter, options);

        testWriter.encodeFeature(f);
        JSONObject jsonF = (JSONObject) JSONSerializer.toJSON(w.toString());
        JSONObject mineName = jsonF.getJSONObject("properties").getJSONObject("MineName");
        assertFalse(mineName.has("id"));
        assertFalse(mineName.has("geometry"));
        assertFalse(mineName.has("properties"));
        assertFalse(mineName.has("@dataType"));
    }

    /** used to expose a public encodeFeature method for testing purposes * */
    class TestComplexGeoJsonWriter extends ComplexGeoJsonWriter {

        public TestComplexGeoJsonWriter(
                GeoJSONBuilder jsonWriter, ComplexGeoJsonWriterOptions settings) {
            super(jsonWriter, settings);
        }

        public TestComplexGeoJsonWriter(GeoJSONBuilder jsonWriter) {
            super(jsonWriter);
        }

        public void encodeFeature(Feature feature) {
            super.encodeFeature(feature, true);
        }
    }

    // build a complexFeature with a nested Feature
    private Feature buildComplexFeature() {
        ComplexFeatureBuilder fBuilder = new ComplexFeatureBuilder(FakeTypes.Mine.MINETYPE_TYPE);
        ComplexAttribute attr = getNestedFeature("a name", true);
        fBuilder.append(FakeTypes.Mine.NAME_mineName, attr);
        return fBuilder.buildFeature("id");
    }

    private static ComplexAttribute getNestedFeature(String name, boolean isPreferred) {
        ComplexFeatureBuilder complexFB = new ComplexFeatureBuilder(MINENAME_DESCRIPTOR);
        AttributeBuilder builder = new AttributeBuilder(new LenientFeatureFactoryImpl());
        builder.setDescriptor(FakeTypes.Mine.ISPREFERRED_DESCRIPTOR);
        Attribute isPrefAttr = builder.buildSimple("isPreferred_testId", isPreferred);
        builder.setDescriptor(FakeTypes.Mine.mineNAME_DESCRIPTOR);
        Attribute nameAttr = builder.buildSimple("mineName_testId", name);
        complexFB.append(FakeTypes.Mine.NAME_isPreferred, isPrefAttr);
        complexFB.append(FakeTypes.Mine.NAME_mineName, nameAttr);

        return complexFB.buildFeature(null);
    }
}
