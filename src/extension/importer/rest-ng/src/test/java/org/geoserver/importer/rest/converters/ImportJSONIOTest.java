/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.*;
import org.geoserver.importer.rest.TransformTestSupport;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.rest.RequestInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.request.AbstractRequestAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ImportJSONIOTest extends ImporterTestSupport {
    private ImportJSONWriter writer;

    private ByteArrayOutputStream buf;

    private RequestAttributes oldAttributes;

    @Before
    public void prepareData() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        importer.createContext(new Directory(dir));
        
        RequestInfo info = new RequestInfo();
        info.setServletPath("servletPath");
        info.setBaseURL("baseURL");
        info.setPagePath("pagePath");

        newBuffer();
        writer = new ImportJSONWriter(importer, buf);

        oldAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new TransformTestSupport.MapRequestAttributes());

        RequestInfo.set(info);
    }

    @After
    public void cleanUp() {
        RequestContextHolder.setRequestAttributes(oldAttributes);
    }

    private ImportJSONReader reader() throws IOException {
        return new ImportJSONReader(importer, stream(buffer()));
    }

    private ImportJSONReader reader(JSONObject json) throws IOException {
        ImportJSONReader reader = new ImportJSONReader(importer, stream(buffer()));
        reader.json = json;
        return reader;
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
        Assert.assertNotNull(store);
        Assert.assertEquals("foobar", store.getName());
        Assert.assertEquals(getCatalog().getDefaultWorkspace().getName(), store.getWorkspace().getName());
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
        Assert.assertNotNull(task);

        TransformChain chain = task.getTransform();
        Assert.assertNotNull(chain);
        Assert.assertEquals(1, chain.getTransforms().size());
        DateFormatTransform dft = (DateFormatTransform) chain.getTransforms().get(0);
        Assert.assertEquals("foobar",dft.getField());
        Assert.assertEquals("yyyy-MM-dd",dft.getDatePattern().dateFormat().toPattern());
    }

    @Test
    public void testRemoteDataFreeAccess() throws IOException {
        ImportContext context = importer.registerContext(null);
        context.setData(new RemoteData("http://www.geoserver.org/data"));

        writer.context(context, true, 3);
        ByteArrayInputStream inbuf = new ByteArrayInputStream(buf.toByteArray());
        ImportContext readBack = new ImportJSONReader(importer, inbuf).context();

        Assert.assertEquals(context.getData(), readBack.getData());
    }

    @Test
    public void testRemoteDataFullDataAccess() throws IOException {
        ImportContext context = importer.registerContext(null);
        RemoteData data = new RemoteData("http://www.geoserver.org/data");
        data.setUsername("foo");
        data.setPassword("bar");
        data.setDomain("myDomain");
        context.setData(data);

        writer.context(context, true, 3);
        ByteArrayInputStream inbuf = new ByteArrayInputStream(buf.toByteArray());
        ImportContext readBack = new ImportJSONReader(importer, inbuf).context();

        Assert.assertEquals(context.getData(), readBack.getData());
    }
}
