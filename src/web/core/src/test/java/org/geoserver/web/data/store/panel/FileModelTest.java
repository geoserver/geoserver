package org.geoserver.web.data.store.panel;

import java.io.File;
import java.io.IOException;

import org.apache.wicket.model.Model;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

public class FileModelTest extends GeoServerTestSupport {

    public void testAbsolute() throws IOException {
        // create test file
        File f = new File("target/fileModelTest.xml");
        try {
            f.createNewFile();
            
            FileModel model = new FileModel(new Model<String>());
            model.setObject(f.getAbsolutePath());
            String path = (String) model.getObject();
            assertEquals("file://" + f.getAbsolutePath(), path);
        } finally {
            f.delete();
        }
    }
    
    public void testAbsoluteToRelative() throws IOException {
        // pick up an existing file
        File root = getDataDirectory().root();
        File data = new File(root, "data");
        File cite = new File(data, MockData.CITE_PREFIX);
        File buildings = new File(cite, "Buildings.properties");
        
        FileModel model = new FileModel(new Model<String>());
        model.setObject(buildings.getAbsolutePath());
        String path = (String) model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }
    
    public void testRelativeUnmodified() throws IOException {
        FileModel model = new FileModel(new Model<String>());
        model.setObject("file:data/cite/Buildings.properties");
        String path = (String) model.getObject();
        assertEquals("file:data/cite/Buildings.properties", path);
    }
}
