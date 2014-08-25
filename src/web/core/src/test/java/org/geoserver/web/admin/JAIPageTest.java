/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import javax.media.jai.registry.RenderedRegistryMode;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.resources.image.ImageUtilities;
import org.junit.Assert;
import org.junit.Test;

import com.sun.media.jai.mlib.MlibWarpRIF;
import com.sun.media.jai.opimage.WarpRIF;

public class JAIPageTest extends GeoServerWicketTestSupport {
    @Test
    public void testValues() {
        JAIInfo info = (JAIInfo) getGeoServerApplication()
            .getGeoServer()
            .getGlobal().getJAI();

        login();

        tester.startPage(JAIPage.class);
        tester.assertComponent("form:tileThreads", TextField.class);
        tester.assertModelValue("form:tileThreads", info.getTileThreads());
    }
    
    @Test
    public void testNativeWarp(){
        if(!ImageUtilities.isMediaLibAvailable()){
            // If medialib acceleration is not available, the test is not needed
            Assert.assertTrue(true);
            return;
        }
        JAIInfo info = (JAIInfo) getGeoServerApplication()
                .getGeoServer()
                .getGlobal().getJAI();
        
        // Ensure that by default Warp acceleration is set to false
        Assert.assertFalse(info.isAllowNativeWarp());
        
        login();
        // Ensure the page is rendered
        tester.startPage(JAIPage.class);
        tester.assertRenderedPage(JAIPage.class);
        
        // Set Native Warp enabled
        FormTester form = tester.newFormTester("form");
        form.setValue("allowNativeWarp", true);
        form.submit("submit");
        
        // Ensure no exception has been thrown
        tester.assertNoErrorMessage();
        
        info = (JAIInfo) getGeoServerApplication()
                .getGeoServer()
                .getGlobal().getJAI();
        
        // Check that Warp is enabled
        Assert.assertTrue(info.isAllowNativeWarp());
        
        // Ensure the factory is correctly registered
        Object factory = info.getJAI().getOperationRegistry().getFactory(RenderedRegistryMode.MODE_NAME, "Warp");
        Assert.assertTrue(factory instanceof MlibWarpRIF);
        
        // Unset Native Warp enabled
        
        // Render the page again
        tester.startPage(JAIPage.class);
        tester.assertRenderedPage(JAIPage.class);
        
        form = tester.newFormTester("form");
        form.setValue("allowNativeWarp", false);
        form.submit("submit");
        
        // Ensure no exception has been thrown
        tester.assertNoErrorMessage();
        
        info = (JAIInfo) getGeoServerApplication()
                .getGeoServer()
                .getGlobal().getJAI();
        
        // Check that Warp is enabled
        Assert.assertFalse(info.isAllowNativeWarp());
        
        // Ensure the factory is correctly registered
        factory = info.getJAI().getOperationRegistry().getFactory(RenderedRegistryMode.MODE_NAME, "Warp");
        Assert.assertTrue(factory instanceof WarpRIF);
    }
}
