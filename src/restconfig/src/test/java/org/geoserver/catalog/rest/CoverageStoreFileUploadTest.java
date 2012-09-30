/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static junit.framework.Assert.*;

import java.io.ByteArrayInputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.geotools.data.DataUtilities;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class CoverageStoreFileUploadTest extends CatalogRESTTestSupport {

    @Test
    public void testWorldImageUploadZipped() throws Exception {
        URL zip = getClass().getResource( "test-data/usa.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/usa/file.worldimage", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        assertEquals( "coverageStore", d.getDocumentElement().getNodeName());

    }
}
