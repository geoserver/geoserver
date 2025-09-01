/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.junit.Assert;
import org.junit.Test;

public class XStreamPersisterTest {

    @Test
    public void testXStreamNumberIgnored() throws IOException {
        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        XStreamPersister xp = factory.createXMLPersister();
        // Register number type
        xp.registerBriefMapComplexType("number", Number.class);
        // Set Integer to be ignored by XStream type lookup
        xp.addBackwardsBriefIgnored(Integer.class);

        DataStoreInfoImpl dataStoreInfo = new DataStoreInfoImpl(null);
        MetadataMap metadata = dataStoreInfo.getMetadata();
        metadata.put("number", 5);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xp.save(dataStoreInfo, out);

        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        // Check that number was just cast to String
        Assert.assertTrue(result.contains("<entry key=\"number\">5</entry>"));
    }

    @Test
    public void testXStreamNumber() throws IOException {
        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        XStreamPersister xp = factory.createXMLPersister();
        // Register number type
        xp.registerBriefMapComplexType("number", Number.class);

        DataStoreInfoImpl dataStoreInfo = new DataStoreInfoImpl(null);
        MetadataMap metadata = dataStoreInfo.getMetadata();
        metadata.put("number", 5);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xp.save(dataStoreInfo, out);

        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        // Check that number was converted using XStream
        Assert.assertTrue(result.contains("<entry key=\"number\">\n" + "      <number>5</number>\n" + "    </entry>"));
    }
}
