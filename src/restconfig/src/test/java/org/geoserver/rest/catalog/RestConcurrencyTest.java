/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;

public class RestConcurrencyTest extends CatalogRESTTestSupport {

    static volatile Exception exception;
    volatile DispatcherServlet dispatcher;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // super.onSetUp(testData);
        exception = null;

        // Uncomment this and ... KA-BOOM!!!!
        // GeoServerConfigurationLock locker = (GeoServerConfigurationLock) applicationContext
        // .getBean("configurationLock");
        // locker.setEnabled(false);
    }

    protected void addPropertyDataStores(int typeCount) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < typeCount; i++) {
            String name = "pds" + i;
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
            writer.write("_=name:String,pointProperty:Point\n");
            writer.write(name + ".0='zero'|POINT(0 0)\n");
            writer.write(name + ".1='one'|POINT(1 1)\n");
            writer.flush();

            zout.putNextEntry(new ZipEntry(name + ".properties"));
            zout.write(bytes.toByteArray());
            bytes.reset();
        }

        zout.flush();
        zout.close();

        put(
                RestBaseController.ROOT_PATH
                        + "/workspaces/gs/datastores/pds/file.properties?configure=none",
                zbytes.toByteArray(),
                "application/zip");
    }

    @Override
    protected DispatcherServlet getDispatcher() throws Exception {
        if (dispatcher == null) {
            synchronized (this) {
                if (dispatcher == null) {
                    dispatcher = super.getDispatcher();
                }
            }
        }
        return dispatcher;
    }

    @Test
    public void testFeatureTypeConcurrency() throws Exception {
        int typeCount = 5;
        addPropertyDataStores(typeCount);
        ExecutorService es = Executors.newCachedThreadPool();
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < typeCount; i++) {
                futures.add(es.submit(new AddRemoveFeatureTypeWorker("gs", "pds", "pds" + i, 5)));
            }
            for (Future<Integer> future : futures) {
                future.get();
            }
        } finally {
            es.shutdownNow();
        }
    }

    class AddRemoveFeatureTypeWorker implements Callable<Integer> {

        final Logger LOGGER = Logging.getLogger(RestConcurrencyTest.class);

        String typeName;

        String workspace;

        String store;

        int loops;

        public AddRemoveFeatureTypeWorker(
                String workspace, String store, String typeName, int loops) {
            this.typeName = typeName;
            this.workspace = workspace;
            this.store = store;
            this.loops = loops;
        }

        @Override
        public Integer call() throws Exception {
            try {
                callInternal();
            } catch (Exception e) {
                exception = e;
                throw e;
            }

            return loops;
        }

        private void callInternal() throws Exception {
            login();
            String threadId = Thread.currentThread().getId() + " ";
            for (int i = 0; i < loops && exception == null; i++) {
                // add the type name
                String base =
                        RestBaseController.ROOT_PATH
                                + "/workspaces/"
                                + workspace
                                + "/datastores/"
                                + store
                                + "/featuretypes/";
                String xml =
                        "<featureType>"
                                + "<name>"
                                + typeName
                                + "</name>"
                                + "<nativeName>"
                                + typeName
                                + "</nativeName>"
                                + "<srs>EPSG:4326</srs>"
                                + "<nativeCRS>EPSG:4326</nativeCRS>"
                                + "<nativeBoundingBox>"
                                + "<minx>0.0</minx>"
                                + "<maxx>1.0</maxx>"
                                + "<miny>0.0</miny>"
                                + "<maxy>1.0</maxy>"
                                + "<crs>EPSG:4326</crs>"
                                + "</nativeBoundingBox>"
                                + "<store>"
                                + store
                                + "</store>"
                                + "</featureType>";

                LOGGER.info(threadId + "Adding " + typeName);
                MockHttpServletResponse response = postAsServletResponse(base, xml, "text/xml");

                assertEquals(201, response.getStatus());
                assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
                assertNotNull(response.getHeader("Location"));
                assertTrue(response.getHeader("Location").endsWith(base + typeName));

                // check it's there
                LOGGER.info(threadId + "Checking " + typeName);
                String resourcePath =
                        RestBaseController.ROOT_PATH + "/layers/" + workspace + ":" + typeName;
                response = getAsServletResponse(resourcePath + ".xml");
                assertEquals(200, response.getStatus());

                // reload
                LOGGER.info(threadId + "Reloading catalog");
                assertEquals(
                        200,
                        postAsServletResponse(RestBaseController.ROOT_PATH + "/reload", "")
                                .getStatus());

                // remove it
                LOGGER.info(threadId + "Removing layer");
                String deletePath = resourcePath + "?recurse=true";
                assertEquals(200, deleteAsServletResponse(deletePath).getStatus());
            }
        }
    }
}
