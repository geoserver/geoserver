/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vividsolutions.jts.util.Assert;

public class FreemarkerTemplateUpdateTest extends CatalogRESTTestSupport {

    @Test
	public void testUpdate() throws Exception {
        String fooTemplate = "/rest/templates/foo.ftl";
        String barTemplate = "/rest/templates/bar.ftl";
        
        String fooContent = "hello foo - longer than bar";
        String barContent = "hello bar";
        
        // PUT
        put(fooTemplate, fooContent).close();
        put(barTemplate, barContent).close();
        
        // GET
        assertEquals(fooContent, getAsString(fooTemplate).trim());
        assertEquals(barContent, getAsString(barTemplate).trim());
        
        fooContent = "goodbye foo";
        
        // PUT
        put(fooTemplate, fooContent).close();
        
        // GET
        assertEquals(fooContent, getAsString(fooTemplate).trim());
        assertEquals(barContent, getAsString(barTemplate).trim());
    }
}
