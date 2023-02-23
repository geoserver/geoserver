/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.geoserver.importer.rest.ImportTransformTest.BASEPATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.junit.Before;
import org.junit.Test;

public class AttributesToPointGeometryTransformTest extends ImporterTestSupport {

    DataStoreInfo store;

    @Before
    public void setupTransformContext() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        ImportTask importTask = context.getTasks().get(0);

        AttributesToPointGeometryTransform transform =
                new AttributesToPointGeometryTransform("LAT", "LON", "point", true);

        importTask.addTransform(transform);
        importer.changed(importTask);
    }

    /**
     * Tests if all fields processed correctly by writer
     *
     * @throws Exception
     */
    @Test
    public void testGetTransform() throws Exception {
        int id = lastId();
        JSON json = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms/0");

        assertTrue(json instanceof JSONObject);

        JSONObject jsonObject = (JSONObject) json;

        assertEquals("LAT", jsonObject.get("latField"));
        assertEquals("LON", jsonObject.get("lngField"));
        assertEquals("point", jsonObject.get("pointFieldName"));
        assertTrue(jsonObject.getBoolean("preserveGeometry"));
    }
}
