/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import junit.framework.TestCase;

import org.geoserver.platform.GeoServerExtensions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class SecureObjectsTest {

    @BeforeClass
    public static void initAppContext() {
        // setup extensions so that we can do extension point lookups
        ApplicationContext ac = new ClassPathXmlApplicationContext(
                new String[] { "classpath:/securedObjectsContext.xml"});
        new GeoServerExtensions().setApplicationContext(ac);
    }

    @AfterClass
    public static void destroyAppContext() {
        new GeoServerExtensions().setApplicationContext(null);
    }
}
