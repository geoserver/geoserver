package org.geoserver.wfsv.security;

import junit.framework.TestCase;

import org.geoserver.platform.GeoServerExtensions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class SecuredVersioningTest extends TestCase {
    @Override
    protected void setUp() throws Exception {
        // setup extensions so that we can do extension point lookups
        ApplicationContext ac = new ClassPathXmlApplicationContext(
                new String[] { "classpath:/versionedSecuredObjectContext.xml" });
        new GeoServerExtensions().setApplicationContext(ac);
    }
}
