/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.renderer;

import java.net.URI;
import java.net.URISyntaxException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tkunicki
 */
public class StyleEncoderTest {

    @Test
    public void testGenerateExternalGraphicURL() throws URISyntaxException {
        URI uri;

        uri = new URI("http://somehost:8080/geoserver/somedir/test.png");
        assertEquals("images/test.png", StyleEncoder.relativizeExternalGraphicImageResourceURI(uri));

        uri = new URI("somedir/test.png");
        assertEquals("images/test.png", StyleEncoder.relativizeExternalGraphicImageResourceURI(uri));

        uri = new URI("test.png");
        assertEquals("images/test.png", StyleEncoder.relativizeExternalGraphicImageResourceURI(uri));

    }

}
