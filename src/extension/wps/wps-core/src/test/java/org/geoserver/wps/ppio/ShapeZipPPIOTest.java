/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import org.geoserver.util.ZipTestUtil;
import org.junit.Test;

public class ShapeZipPPIOTest {

    @Test
    public void testDecodeBadEntryName() throws Exception {
        try (InputStream input = ZipTestUtil.getZipSlipInput()) {
            new ShapeZipPPIO(null, null, null, null).decode(input);
            fail("Expected decompression to fail");
        } catch (IOException e) {
            assertThat(e.getMessage(), startsWith("Entry is outside of the target directory"));
        }
    }
}
