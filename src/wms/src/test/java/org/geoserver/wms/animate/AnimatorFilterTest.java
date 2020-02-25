/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.animate;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.Filter;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Some unit and functional tests for animator filter
 *
 * @author Tom Kunicki, Boundless
 */
public class AnimatorFilterTest extends WMSTestSupport {

    @Override
    public List<Filter> getFilters() {
        return Arrays.asList((Filter) new AnimatorFilter());
    }

    @Test
    public void testDefaults() throws Exception {

        String requestURL = "cite/wms/animate?aparam=layers&avalues=MapNeatline,Buildings,Lakes";

        MockHttpServletResponse resp = getAsServletResponse(requestURL);

        // check mime type
        assertEquals("image/gif", resp.getContentType());
        // check for multiple (3) frames
        assertEquals(3, extractImageCountFromGIF(resp));
    }

    @Test
    public void testGEOS_6006() throws Exception {

        String requestURL =
                "cite/wms/animate?request=getmap&aparam=layers&avalues=MapNeatline,Buildings,Lakes";

        MockHttpServletResponse resp = getAsServletResponse(requestURL);

        // check mime type
        assertEquals("image/gif", resp.getContentType());
        // check for multiple (3) frames
        assertEquals(3, extractImageCountFromGIF(resp));
    }

    private int extractImageCountFromGIF(MockHttpServletResponse response) throws IOException {
        InputStream is = null;
        ImageInputStream iis = null;
        ImageReader r = null;
        try {
            is = getBinaryInputStream(response);
            iis = ImageIO.createImageInputStream(is);
            r = ImageIO.getImageReadersBySuffix("gif").next();
            r.setInput(iis);
            return r.getNumImages(true);
        } finally {
            if (r != null) {
                r.dispose();
            }
            if (iis != null) {
                iis.close();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
