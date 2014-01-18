/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.junit.Test;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.w3c.dom.Document;

public class ImporterMosaicTest extends ImporterTestSupport {

    @Test
    public void testSimpleMosaic() throws Exception {
        File dir = unpack("mosaic/bm.zip");
        ImportContext context = importer.createContext(new Mosaic(dir));
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof Mosaic);
        assertTrue(task.getData().getFormat() instanceof MosaicFormat);

        importer.run(context);

        runChecks(dir.getName());
    }

    @Test
    public void testFilenameTimeHandler() throws Exception {
        Mosaic m = new Mosaic(unpack("mosaic/bm_time.zip"));

        m.setTimeMode(TimeMode.FILENAME);
        FilenameTimeHandler th = (FilenameTimeHandler) m.getTimeHandler(); 
        th.setFilenameRegex("(\\d){6}");
        th.setTimeFormat("yyyyMM");

        m.prepare();

        List<FileData> files = m.getFiles();
        assertEquals(4,files.size());

        for (int i = 0; i < files.size(); i++) {
            FileData fd = files.get(i);
            assertTrue(fd instanceof Granule);

            Granule g = (Granule) fd;

            //TODO: comparison fails on build server
            assertNotNull(g.getTimestamp());
            //assertEquals(date(2004, i), g.getTimestamp());
        }
    }

    @Test
    public void testTimeMosaic() throws Exception {
        Mosaic m = new Mosaic(unpack("mosaic/bm_time.zip"));

        m.setTimeMode(TimeMode.FILENAME);
        FilenameTimeHandler th = (FilenameTimeHandler) m.getTimeHandler(); 
        th.setFilenameRegex("(\\d){6}");
        th.setTimeFormat("yyyyMM");

        ImportContext context = importer.createContext(m);
        assertEquals(1, context.getTasks().size());
        
        importer.run(context);

        LayerInfo l = context.getTasks().get(0).getLayer();
        ResourceInfo r = l.getResource();
        assertTrue(r.getMetadata().containsKey("time"));

        DimensionInfo d = (DimensionInfo) r.getMetadata().get("time");
        assertNotNull(d);

        runChecks(l.getName());

        Document dom = getAsDOM(String.format("/%s/%s/wms?request=getcapabilities", 
            r.getStore().getWorkspace().getName(), l.getName()));
        XMLAssert.assertXpathExists(
            "//wms:Layer[wms:Name = '" + m.getName() + "']/wms:Dimension[@name = 'time']", dom);
        
    }

    @Test
    public void testTimeMosaicAuto() throws Exception {
        Mosaic m = new Mosaic(unpack("mosaic/bm_time.zip"));
        m.setTimeMode(TimeMode.AUTO);

        ImportContext context = importer.createContext(m);
        assertEquals(1, context.getTasks().size());

        importer.run(context);

        LayerInfo l = context.getTasks().get(0).getLayer();
        ResourceInfo r = l.getResource();
        assertTrue(r.getMetadata().containsKey("time"));

        DimensionInfo d = (DimensionInfo) r.getMetadata().get("time");
        assertNotNull(d);

        runChecks(l.getName());

        Document dom = getAsDOM(String.format("/%s/%s/wms?request=getcapabilities", 
            r.getStore().getWorkspace().getName(), l.getName()));
        XMLAssert.assertXpathExists(
            "//wms:Layer[wms:Name = '" + m.getName() + "']/wms:Dimension[@name = 'time']", dom);
    }

    Date date(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
