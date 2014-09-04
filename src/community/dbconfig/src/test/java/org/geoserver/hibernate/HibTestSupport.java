/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.hibernate;

import org.h2.tools.DeleteDbFiles;
import org.hibernate.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class HibTestSupport {

    protected static XmlWebApplicationContext ctx;
    protected static SessionFactory sessionFactory;
    
    
    @BeforeClass
    public static void initAppContext() throws Exception {
        ctx = new XmlWebApplicationContext() {
            public String[] getConfigLocations() {
                return new String[]{
                    "file:src/main/resources/applicationContext.xml", 
                    "file:src/test/resources/applicationContext-test.xml"};
            }
        };
        ctx.refresh();
        
        sessionFactory = (SessionFactory) ctx.getBean("hibSessionFactory");
        HibUtil.setUpSession(sessionFactory, true);
    }
    
    @AfterClass
    public static void destroy() throws Exception {
        HibUtil.tearDownSession(sessionFactory, null);
        ctx.close();
        DeleteDbFiles.execute("target", "geoserver", false);
    }

}
