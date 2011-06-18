/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerTestSupport;

public class SLDValidatorTest extends GeoServerTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        GeoServerResourceLoader rl = getResourceLoader();
        copyToWebInf(rl, "gml/2.1.2.1/geometry.xsd");
        copyToWebInf(rl, "gml/2.1.2.1/feature.xsd");
        copyToWebInf(rl, "xlink/1.0.0/xlinks.xsd");
        copyToWebInf(rl, "filter/1.0.0/expr.xsd");
        copyToWebInf(rl, "filter/1.0.0/filter.xsd");
        copyToWebInf(rl, "sld/StyledLayerDescriptor.xsd");
        
    }
    
    void copyToWebInf(GeoServerResourceLoader rl, String file) throws IOException {
        File f = new File("../web/app/src/main/webapp/schemas/" + file);
        FileUtils.copyFile(f, rl.createFile("WEB-INF/schemas/"+file));
    }
    
    public void testValid() throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = validator.validateSLD(getClass().getResourceAsStream("valid.sld"),null);
        
        //showErrors(errors);
        assertTrue(errors.isEmpty());
    }
    
    public void testInvalid() throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = validator.validateSLD(getClass().getResourceAsStream("invalid.sld"),null);
        
        showErrors(errors);
        assertFalse(errors.isEmpty());
    }
    
    void showErrors(List errors) {
        for (Exception err : (List<Exception>)errors) {
            System.out.println(err.getMessage());
        }
    }
}
