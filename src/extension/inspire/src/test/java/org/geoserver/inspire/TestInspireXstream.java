/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.wfs.WFSInfoImpl;
import org.junit.Assert;
import org.junit.Test;

public class TestInspireXstream {

    @Test
    public void testUniqueResourceIdentifiersXStreamListConverterIgnored() throws IOException {
        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        XStreamPersister xp = factory.createXMLPersister();
        // Register list type
        xp.registerBriefMapComplexType("list", List.class);
        // Set UniqueResourceIdentifiers to be ignored by XStream type lookup
        xp.addBackwardsBriefIgnored(UniqueResourceIdentifiers.class);

        WFSInfoImpl wfsInfo = new WFSInfoImpl();
        MetadataMap metadata = wfsInfo.getMetadata();
        metadata.put("inspire.spatialDatasetIdentifier", new UniqueResourceIdentifiers());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xp.save(wfsInfo, out);

        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        Assert.assertTrue(result.contains("<entry key=\"inspire.spatialDatasetIdentifier\"></entry>"));
    }

    @Test
    public void testUniqueResourceIdentifiersXStreamListConverterUsed() throws IOException {
        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        XStreamPersister xp = factory.createXMLPersister();
        // Register list type
        xp.registerBriefMapComplexType("list", List.class);

        WFSInfoImpl wfsInfo = new WFSInfoImpl();
        MetadataMap metadata = wfsInfo.getMetadata();
        metadata.put("inspire.spatialDatasetIdentifier", new UniqueResourceIdentifiers());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xp.save(wfsInfo, out);

        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        Assert.assertTrue(
                result.contains(
                        """
                <entry key="inspire.spatialDatasetIdentifier">
                      <list>
                        <size>0</size>
                      </list>
                    </entry>"""));
    }
}
