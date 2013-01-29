/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import junit.framework.Assert;


public class FreemarkerTemplateUpdateTest extends CatalogRESTTestSupport {

	public void testUpdate() throws Exception {
        String fooTemplate = "/rest/templates/foo.ftl";
        String barTemplate = "/rest/templates/bar.ftl";
        
        String fooContent = "hello foo";
        String barContent = "hello bar";
        
        // PUT
        put(fooTemplate, fooContent).close();
        put(barTemplate, barContent).close();
        
        // GET
        Assert.assertEquals(fooContent, getAsString(fooTemplate).trim());
        Assert.assertEquals(barContent, getAsString(barTemplate).trim());
        
        fooContent = "goodbye foo";
        
        // PUT
        put(fooTemplate, fooContent).close();
        
        // GET
        Assert.assertEquals(fooContent, getAsString(fooTemplate).trim());
        Assert.assertEquals(barContent, getAsString(barTemplate).trim());
    }
}
