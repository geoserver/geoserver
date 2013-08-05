/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

public class ImporterTest extends ImporterTestSupport {

    public void testCreateContextSingleFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();
        
        ImportContext context = importer.createContext(file);
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(file, task.getData());
    }

    public void testCreateContextDirectoryHomo() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        Directory d = new Directory(dir);
        ImportContext context = importer.createContext(d);
        assertEquals(2, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(d.part("archsites"), task.getData());

        task = context.getTasks().get(1);
        assertEquals(d.part("bugsites"), task.getData());
    }

    public void testCreateContextDirectoryHetero() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("geotiff/EmissiveCampania.tif.bz2", dir);

        Directory d = new Directory(dir);
        
        ImportContext context = importer.createContext(d);
        assertEquals(2, context.getTasks().size());
        
        // cannot ensure order of tasks due to hashing
        HashSet files = new HashSet();
        files.add(context.getTasks().get(0).getData());
        files.add(context.getTasks().get(1).getData());        
        assertTrue(files.containsAll(d.getFiles()));
    }

    public void testCreateContextFromArchive() throws Exception {
        File file = file("shape/archsites_epsg_prj.zip");
        Archive arch = new Archive(file);
        
        ImportContext context = importer.createContext(arch);
        assertEquals(1, context.getTasks().size());
    }

    public void testCreateContextIgnoreHidden() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        FileUtils.touch(new File(dir, ".DS_Store"));

        ImportContext context = importer.createContext(new Directory(dir));
        assertEquals(1, context.getTasks().size());
    }
}
