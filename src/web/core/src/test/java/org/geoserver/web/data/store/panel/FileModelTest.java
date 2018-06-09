/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.model.Model;
import org.geoserver.data.test.MockData;
import org.geoserver.web.util.MapModel;
import org.junit.Before;
import org.junit.Test;

public class FileModelTest {

    File root;

    @Before
    public void init() throws IOException {
        root = File.createTempFile("file", "test", new File("target"));
        root.delete();
        root.mkdirs();
    }

    @Test
    public void testAbsolute() throws IOException {
        // create test file
        File f = new File("target/fileModelTest.xml");
        try {
            f.createNewFile();

            FileModel model = new FileModel(new Model<String>(), root);
            model.setObject(f.getAbsolutePath());
            String path = (String) model.getObject();
            assertEquals("file://" + f.getAbsolutePath(), path);
        } finally {
            f.delete();
        }
    }

    @Test
    public void testAbsoluteToRelative() throws IOException {
        // pick up an existing file

        File data = new File(root, "data");
        File cite = new File(data, MockData.CITE_PREFIX);
        File buildings = new File(cite, "Buildings.properties");

        FileModel model = new FileModel(new Model<String>(), root);
        model.setObject(buildings.getAbsolutePath());
        String path = (String) model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }

    @Test
    public void testRelativeUnmodified() throws IOException {
        FileModel model = new FileModel(new Model<String>(), root);
        model.setObject("file:data/cite/Buildings.properties");
        String path = (String) model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }

    @Test
    public void testURL() throws IOException {
        Map map = new HashMap();
        map.put("url", new URL("file:data/cite/Buildings.properties"));
        MapModel mapModel = new MapModel(map, "url");
        FileModel model = new FileModel(mapModel, root);
        String path = (String) model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }
}
