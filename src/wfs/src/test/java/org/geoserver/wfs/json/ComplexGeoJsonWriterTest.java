/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.identity.Identifier;

public class ComplexGeoJsonWriterTest {

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
                ArrayList<Property> list = new ArrayList<Property>();
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
}
