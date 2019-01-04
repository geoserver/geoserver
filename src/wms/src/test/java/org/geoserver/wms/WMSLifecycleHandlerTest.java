/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.SystemUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.renderer.style.FontCache;
import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Test;

/** Tests automatic font registration */
public class WMSLifecycleHandlerTest extends WMSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Test
    public void testOTFFontRegistration() throws IOException {
        // loading fonts causes Java to open a Channel on the file, but there is no way to
        // release it from client code, thus the test will fail to delete filse on Windows
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        // in case this font is already on the machine we cannot run a meaningful test
        FontCache fontCache = FontCache.getDefaultInstance();
        String fontName = "League Mono Regular";
        Assume.assumeThat(fontCache.getFont(fontName), CoreMatchers.nullValue());

        // copy over the font
        Resource styles = getDataDirectory().get("styles");
        String fontFileName = "LeagueMono-Regular.otf";
        try (InputStream is = WMSLifecycleHandlerTest.class.getResourceAsStream(fontFileName)) {
            assertNotNull(is);
            Resources.copy(is, styles, fontFileName);
        }

        // force a reset
        getGeoServer().reset();

        // now the font should be in font cache
        Font theFont = fontCache.getFont(fontName);
        assertThat(fontCache.getAvailableFonts().toString(), theFont, CoreMatchers.notNullValue());
    }
}
