/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import net.opengis.wfs.NativeType;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.xml.Parser;
import org.junit.Test;

public class NativeTypeParsingTest extends WFSTestSupport {

	@Test
    public void testParse() throws Exception {
        Parser p = new Parser(getXmlConfiguration11());
        NativeType nativ = (NativeType) p.parse(
            new ByteArrayInputStream("<wfs:Native safeToIgnore='true' xmlns:wfs='http://www.opengis.net/wfs'>here is some text</wfs:Native>".getBytes()));

        assertEquals("here is some text", nativ.getValue());
    }
}
