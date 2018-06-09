/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.geoserver.config.util.SecureXStream.ForbiddenClassExceptionEx;
import org.geoserver.util.PropertyRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SecureXStreamTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    @Rule
    public PropertyRule whitelistProperty = PropertyRule.system("GEOSERVER_XSTREAM_WHITELIST");

    @Test
    public void testPropertyCanAllow() throws Exception {
        // Check that additional whitelist entries can be added via a system property.

        whitelistProperty.setValue("org.easymock.**");

        SecureXStream xs = new SecureXStream();

        // Check that a class in the package deserializes
        Object o = xs.fromXML("<" + org.easymock.Capture.class.getCanonicalName() + " />");
        assertThat(o, instanceOf(org.easymock.Capture.class));

        // Check that a class from elsewhere still causes an exception
        exception.expect(ForbiddenClassExceptionEx.class);
        xs.fromXML("<" + org.hamcrest.core.AllOf.class.getCanonicalName() + " />");
    }

    @Test
    public void testPropertyCanAllowMultiple() throws Exception {
        // Check that additional whitelist entries can be added via a system property.

        whitelistProperty.setValue("org.easymock.**; org.junit.**");

        SecureXStream xs = new SecureXStream();

        // Check that a class in the first package deserializes
        Object o1 = xs.fromXML("<" + org.easymock.Capture.class.getCanonicalName() + " />");
        assertThat(o1, instanceOf(org.easymock.Capture.class));

        // Check that a class in the second package deserializes
        Object o2 = xs.fromXML("<" + org.junit.rules.TestName.class.getCanonicalName() + " />");
        assertThat(o2, instanceOf(org.junit.rules.TestName.class));

        // Check that a class from elsewhere still causes an exception
        exception.expect(ForbiddenClassExceptionEx.class);
        xs.fromXML("<" + org.hamcrest.core.AllOf.class.getCanonicalName() + " />");
    }

    @Test
    public void testErrorMessage() throws Exception {
        SecureXStream xs = new SecureXStream();
        try {
            xs.fromXML("<" + org.easymock.Capture.class.getCanonicalName() + " />");
        } catch (ForbiddenClassExceptionEx e) {
            assertEquals(
                    "Unauthorized class found, see logs for more details on how to handle it: "
                            + "org.easymock.Capture",
                    e.getMessage());
        }
    }
}
