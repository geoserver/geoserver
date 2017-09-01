package org.geoserver.wms.web.data;

import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/**
 * A separate test for this because it mysteriously causes a couple other style page tests to fail
 * when run in the same test, maybe due to Tester session stuff?
 *
 */
public class StyleEditPageRenameTest extends GeoServerWicketTestSupport {

    @Test
    public void testRenameDefaultStyle() {
        StyleInfo styleInfo = new StyleInfoImpl(null);
        styleInfo.setName("point");
        styleInfo.setFilename("test.sld");
        GeoServerApplication app = (GeoServerApplication) applicationContext.getBean("webApplication");
        WicketTester styleTest = new WicketTester(app, false);

        StyleEditPage page = new StyleEditPage(styleInfo);
        styleTest.startPage(page);
        styleTest.assertDisabled("styleForm:context:panel:name");
    }
}
