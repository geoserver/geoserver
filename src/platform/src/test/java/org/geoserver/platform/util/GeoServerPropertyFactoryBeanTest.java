/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform.util;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import org.easymock.EasyMock;
import org.geoserver.util.PropertyRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.ApplicationContext;

public class GeoServerPropertyFactoryBeanTest {

    public static final String PROPERTY_NAME = "FOO";

    public @Rule PropertyRule foo = PropertyRule.system(PROPERTY_NAME);
    public @Rule ExpectedException exception = ExpectedException.none();

    GeoServerPropertyFactoryBean<String> factory;

    @Before
    public void setUp() {
        factory =
                new GeoServerPropertyFactoryBean<String>(PROPERTY_NAME) {

                    @Override
                    protected String createInstance(String propertyValue) {
                        if (propertyValue.equals("UNKNOWN")) return null;
                        return "Bean: " + propertyValue;
                    }

                    @Override
                    public Class<?> getObjectType() {
                        return String.class;
                    }
                };
        ApplicationContext context = EasyMock.createMock(ApplicationContext.class);
        EasyMock.replay(context);
        factory.setApplicationContext(context);
    }

    @Test
    public void testGetBean() throws Exception {
        factory.setDefaultValue("Default");
        foo.setValue("testValue1");

        assertThat(factory.createInstance(), equalTo("Bean: testValue1"));
    }

    @Test
    public void testGetDefault() throws Exception {
        factory.setDefaultValue("Default");

        assertThat(factory.createInstance(), equalTo("Bean: Default"));
    }

    @Test
    public void testGetUnsetDefault() throws Exception {
        exception.expect(IllegalStateException.class);
        factory.createInstance();
    }

    @Test
    public void testGetBadDefault() throws Exception {
        exception.expect(IllegalStateException.class);
        factory.setDefaultValue("UNKNOWN");
        factory.createInstance();
    }

    @Test
    public void testFallBackToDefault() throws Exception {
        factory.setDefaultValue("Default");
        foo.setValue("UNKNOWN");

        assertThat(factory.createInstance(), equalTo("Bean: Default"));
    }
}
