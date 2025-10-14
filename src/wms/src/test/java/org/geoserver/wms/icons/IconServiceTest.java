/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.style.Style;
import org.junit.Test;

public class IconServiceTest extends IconTestSupport {

    @Test
    public void testHandle() throws Exception {
        Style style = style(featureTypeStyle(rule(Filter.INCLUDE, grayCircle())));

        StyleInfo s = createNiceMock(StyleInfo.class);
        expect(s.getStyle()).andReturn(style);

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getStyleByName("foo")).andReturn(s);

        replay(s, cat);

        dispatch("/icon/foo", "0.0.0=", cat);
    }

    @Test
    public void testHandleMultipleFTS() throws Exception {
        Style style = style(
                featureTypeStyle(rule(Filter.INCLUDE, grayCircle())),
                featureTypeStyle(rule(Filter.INCLUDE, redStar())));

        StyleInfo s = createNiceMock(StyleInfo.class);
        expect(s.getStyle()).andReturn(style);

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getStyleByName("foo-bar")).andReturn(s);

        replay(s, cat);

        dispatch("/icon/foo-bar", "1.0.0=", cat);
    }

    @Test
    public void testAcceptedNames() throws Exception {
        // test a name with non word characters
        Style style = style(featureTypeStyle(rule(Filter.INCLUDE, grayCircle())));

        StyleInfo s = createNiceMock(StyleInfo.class);
        expect(s.getStyle()).andReturn(style);

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getStyleByName("foo-bar")).andReturn(s);

        replay(s, cat);

        dispatch("/icon/foo-bar", "0.0.0=", cat);
    }

    @SuppressWarnings("PMD.CloseResource")
    void dispatch(String path, String q, Catalog cat) throws Exception {
        HttpServletRequest req = createNiceMock(HttpServletRequest.class);
        expect(req.getPathInfo()).andReturn(path);
        expect(req.getQueryString()).andReturn(q);

        ServletOutputStream out = createNiceMock(ServletOutputStream.class);

        HttpServletResponse res = createMock(HttpServletResponse.class);
        expect(res.getOutputStream()).andReturn(out).anyTimes();
        res.setContentType("image/png");
        expectLastCall();

        replay(req, res, out);

        IconService service = new IconService(cat);
        service.handleRequestInternal(req, res);

        verify(cat);
        verify(res);
    }
}
