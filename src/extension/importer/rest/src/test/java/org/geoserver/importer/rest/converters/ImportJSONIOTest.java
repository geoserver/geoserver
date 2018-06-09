/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.RemoteData;
import org.geoserver.importer.rest.TransformTestSupport;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.rest.RequestInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class ImportJSONIOTest extends ImporterTestSupport {

    private ImportJSONWriter writer;
    FlushableJSONBuilder builder;

    private ImportJSONReader reader;

    private ByteArrayOutputStream outputStream;

    private RequestAttributes oldAttributes;

    @Before
    public void prepareData() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        importer.createContext(new Directory(dir));

        RequestInfo info = new RequestInfo();
        info.setServletPath("servletPath");
        info.setBaseURL("baseURL");
        info.setPagePath("pagePath");

        newOutputStreamAndBuilder();

        writer = new ImportJSONWriter(importer);
        reader = new ImportJSONReader(importer);

        oldAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new TransformTestSupport.MapRequestAttributes());

        RequestInfo.set(info);
    }

    @After
    public void cleanUp() {
        RequestContextHolder.setRequestAttributes(oldAttributes);
    }

    private void newOutputStreamAndBuilder() {
        outputStream = new ByteArrayOutputStream();
        builder = new FlushableJSONBuilder(outputStream);
    }

    /**
     * Parse json from {@link #outputStream}.
     *
     * @return json representation of text
     */
    private JSONObject parseJson(ByteArrayOutputStream buffer) {
        String text = new String(buffer.toByteArray());
        return JSONObject.fromObject(text);
    }

    @Test
    public void testSettingTargetStore() throws IOException {
        ImportTask task = importer.getContext(0).getTasks().get(0);
        writer.task(builder, task, true, 1);

        // update with new target
        JSONObject target = new JSONObject();
        JSONObject dataStore = new JSONObject();
        JSONObject workspace = new JSONObject();
        dataStore.put("name", "foobar");
        workspace.put("name", getCatalog().getDefaultWorkspace().getName());
        dataStore.put("workspace", workspace);
        target.put("dataStore", dataStore);

        JSONObject json = parseJson(outputStream);
        json.getJSONObject("task").put("target", target);

        ImportTask parsed = reader.task(json);
        StoreInfo store = parsed.getStore();
        Assert.assertNotNull(store);
        Assert.assertEquals("foobar", store.getName());
        Assert.assertEquals(
                getCatalog().getDefaultWorkspace().getName(), store.getWorkspace().getName());
    }

    @Test
    public void testAddingDateTransform() throws IOException {
        ImportTask task = importer.getContext(0).getTasks().get(0);
        writer.task(builder, task, true, 1);

        // update with transform
        JSONObject json = parseJson(outputStream);

        JSONArray transforms =
                json.getJSONObject("task")
                        .getJSONObject("transformChain")
                        .getJSONArray("transforms");
        JSONObject dateTransform = new JSONObject();
        dateTransform.put("type", "dateFormatTransform");
        dateTransform.put("field", "foobar");
        dateTransform.put("format", "yyyy-MM-dd");
        transforms.add(dateTransform);

        // hack, remove href
        json.getJSONObject("task").getJSONObject("target").remove("href");

        ImportJSONReader reader = new ImportJSONReader(importer);
        task = reader.task(json);
        Assert.assertNotNull(task);

        TransformChain<?> chain = task.getTransform();
        Assert.assertNotNull(chain);
        Assert.assertEquals(1, chain.getTransforms().size());
        DateFormatTransform dft = (DateFormatTransform) chain.getTransforms().get(0);
        Assert.assertEquals("foobar", dft.getField());
        Assert.assertEquals("yyyy-MM-dd", dft.getDatePattern().dateFormat().toPattern());
    }

    @Test
    public void testRemoteDataFreeAccess() throws IOException {
        ImportContext context = importer.registerContext(null);
        context.setData(new RemoteData("http://www.geoserver.org/data"));
        writer.context(builder, context, true, 3);

        JSONObject json = parseJson(outputStream);
        ImportContext readBack = reader.context(json);

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

        writer.context(builder, context, true, 3);
        ByteArrayInputStream inbuf = new ByteArrayInputStream(outputStream.toByteArray());
        ImportContext readBack = reader.context(reader.parse(inbuf));

        Assert.assertEquals(context.getData(), readBack.getData());
    }
}
