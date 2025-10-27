/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import static org.junit.Assert.assertFalse;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.rest.catalog.DataStoreController;
import org.geoserver.rest.security.DataAccessController;
import org.geoserver.rest.security.RuleMap;
import org.junit.Test;
import org.springframework.http.MediaType;

/** Test Class for ByteArrayMessageConverter */
public class ByteArrayMessageConverterTest {

    private ByteArrayMessageConverter converter = new ByteArrayMessageConverter();

    @Test
    public void testReadingNotSupported() {
        assertFalse(converter.canRead(DataStoreInfo.class, DataStoreController.class, MediaType.APPLICATION_XML));
        assertFalse(converter.canRead(
                TypeUtils.parameterize(RuleMap.class, String.class, String.class),
                DataAccessController.class,
                MediaType.APPLICATION_XML));
    }
}
