/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.rest.PageInfo;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ImportJSONIOTest extends ImporterTestSupport {
    private ImportJSONWriter writer;

    private ByteArrayOutputStream buf;

    @Before
    public void prepareData() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        importer.createContext(new Directory(dir));
        
        PageInfo info = new PageInfo();
        info.setBasePath("basePath");
        info.setBaseURL("baseURL");
        info.setPagePath("pagePath");
        info.setRootPath("rootPath");

        newBuffer();
        writer = new ImportJSONWriter(importer, info, buf);
    }

    private ImportJSONReader reader() throws IOException {
        return new ImportJSONReader(importer, stream(buffer()));
    }

    private ImportJSONReader reader(JSONObject obj) {
        return new ImportJSONReader(importer, obj);
    }

    private void newBuffer() {
        buf = new ByteArrayOutputStream();
    }

    private JSONObject buffer() {
        return JSONObject.fromObject(new String(buf.toByteArray()));
    }

    private InputStream stream(JSONObject json) {
        return new ByteArrayInputStream(json.toString().getBytes());
    }

    @Test
    public void testSettingTargetStore() throws IOException {
        ImportTask task = importer.getContext(0).getTasks().get(0);
        writer.task(task, true, 1);

        // update with new target
        JSONObject json = buffer();
        JSONObject target = new JSONObject();
        JSONObject dataStore = new JSONObject();
        JSONObject workspace = new JSONObject();
        dataStore.put("name", "foobar");
        workspace.put("name", getCatalog().getDefaultWorkspace().getName());
        dataStore.put("workspace", workspace);
        target.put("dataStore", dataStore);
        json.getJSONObject("task").put("target", target);

        ImportTask parsed = reader(json).task();
        StoreInfo store = parsed.getStore();
        assertNotNull(store);
        assertEquals("foobar", store.getName());
        assertEquals(getCatalog().getDefaultWorkspace().getName(), store.getWorkspace().getName());
    }

    @Test
    public void testAddingDateTransform() throws IOException {
        ImportTask task = importer.getContext(0).getTasks().get(0);
        writer.task(task, true, 1);
        
        // update with transform
        JSONObject json = buffer();

        JSONArray transforms = 
            json.getJSONObject("task").getJSONObject("transformChain").getJSONArray("transforms");
        JSONObject dateTransform = new JSONObject();
        dateTransform.put("type", "dateFormatTransform");
        dateTransform.put("field", "foobar");
        dateTransform.put("format", "yyyy-MM-dd");
        transforms.add(dateTransform);

        //hack, remove href
        json.getJSONObject("task").getJSONObject("target").remove("href");

        task = reader(json).task();
        assertNotNull(task);

        TransformChain chain = task.getTransform();
        assertNotNull(chain);
        assertEquals(1, chain.getTransforms().size());
        DateFormatTransform dft = (DateFormatTransform) chain.getTransforms().get(0);
        assertEquals("foobar",dft.getField());
        assertEquals("yyyy-MM-dd",dft.getDatePattern().dateFormat().toPattern());

    }
}
