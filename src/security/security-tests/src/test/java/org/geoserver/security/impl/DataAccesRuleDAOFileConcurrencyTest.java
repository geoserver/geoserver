/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Paths;

/** Same as {@link DataAccessRuleDAOTest} but using an actual property file */
public class DataAccesRuleDAOFileConcurrencyTest extends DataAccesRuleDAOConcurrencyTest {

    @Override
    protected DataAccessRuleDAO buildDAO() throws Exception {
        // make a nice little catalog that does always tell us stuff is there
        Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspaceByName(anyObject()))
                .andReturn(new WorkspaceInfoImpl())
                .anyTimes();
        expect(catalog.getLayerByName((String) anyObject()))
                .andReturn(new LayerInfoImpl())
                .anyTimes();
        replay(catalog);

        // base rules
        Properties props = new Properties();
        props.put("mode", "CHALLENGE");
        props.put("*.*.r", "*");

        File dir = new File("target/layers-concurrency");
        FileUtils.deleteQuietly(dir);
        dir.mkdir();

        try (FileOutputStream fos = new FileOutputStream(new File(dir, "layers.properties"))) {
            props.store(fos, null);
        }

        FileSystemResourceStore store = new FileSystemResourceStore(dir);
        return new DataAccessRuleDAO(catalog, store.get(Paths.BASE));
    }

    @Override
    protected int getLoops() {
        // less loops, this is hitting the file system
        return DEFAULT_LOOPS * 5;
    }
}
