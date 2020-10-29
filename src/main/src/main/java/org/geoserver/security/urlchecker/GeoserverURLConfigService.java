/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.ows.URLCheckers;
import org.geotools.util.logging.Logging;

/**
 * This class manages persistence of GeoserverURLChecker in Data Directory and registration in
 * URLCheckers SPI utility class
 */
public class GeoserverURLConfigService {

    static final Logger LOGGER =
            Logging.getLogger(GeoserverURLConfigService.class.getCanonicalName());

    private File workspaceDirectory;

    private File xmlFile;

    private XStreamPersister xp;

    GeoserverURLChecker geoserverURLChecker;

    private static GeoserverURLConfigService singleton;

    public GeoserverURLConfigService(
            GeoServerResourceLoader dataDir, XStreamPersisterFactory xstreamPersisterFactory)
            throws Exception {
        xp = initialize(xstreamPersisterFactory.createXMLPersister());
        Resource resource = dataDir.get("security//");
        workspaceDirectory = resource.dir();
        xmlFile = dataDir.find(workspaceDirectory, "urls.xml");
        if (xmlFile == null) {
            xmlFile = dataDir.createFile(workspaceDirectory, "urls.xml");
            xmlFile.createNewFile();
            // if does not exist..create from scratch and save
            geoserverURLChecker = createFromScatch();
            save(geoserverURLChecker, xmlFile);

        } else {

            geoserverURLChecker = read(xmlFile);
        }
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(
                    "GeoserverURLConfigService persited in file : " + xmlFile.getAbsolutePath());
        // register
        registerSPI(geoserverURLChecker);
    }

    private XStreamPersister initialize(XStreamPersister xstreamPersister) {
        xstreamPersister.getXStream().alias("GeoserverURLChecker", GeoserverURLChecker.class);
        xstreamPersister.getXStream().alias("urlEntry", URLEntry.class);
        xstreamPersister
                .getXStream()
                .allowTypes(new Class[] {GeoserverURLChecker.class, URLEntry.class});
        xstreamPersister
                .getXStream()
                .addImplicitCollection(GeoserverURLChecker.class, "regexList", URLEntry.class);
        return xstreamPersister;
    }

    private synchronized GeoserverURLChecker createFromScatch() {
        ArrayList<URLEntry> list = new ArrayList<URLEntry>();
        list.add(
                new URLEntry(
                        "generic",
                        "allow http connections and disk access",
                        "^(http|https|file)://.*$"));
        GeoserverURLChecker gsURLChecker = new GeoserverURLChecker(list);
        gsURLChecker.setEnabled(false);
        return gsURLChecker;
    }

    private void save(GeoserverURLChecker geoserverURLChecker, File outFile) throws Exception {

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            xp.save(geoserverURLChecker, fos);
        }
    }

    public synchronized GeoserverURLChecker save() throws Exception {
        // write to disk
        save(geoserverURLChecker, xmlFile);
        // read fresh copy
        this.geoserverURLChecker = reload();
        // update SPI factory with new instance
        registerSPI(geoserverURLChecker);
        return this.geoserverURLChecker;
    }

    public synchronized GeoserverURLChecker save(GeoserverURLChecker toSave) throws Exception {
        // write to disk
        save(toSave, xmlFile);
        // read fresh copy
        this.geoserverURLChecker = reload();
        // update SPI factory with new instance
        registerSPI(geoserverURLChecker);
        return this.geoserverURLChecker;
    }

    public synchronized GeoserverURLChecker removeAndsave(List<URLEntry> removeList)
            throws Exception {
        geoserverURLChecker.removeURLEntry(removeList);
        save();
        return this.geoserverURLChecker;
    }

    public synchronized GeoserverURLChecker addAndsave(URLEntry add) throws Exception {
        geoserverURLChecker.addURLEntry(add);
        save();
        return this.geoserverURLChecker;
    }

    private GeoserverURLChecker read(File inFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(inFile)) {
            GeoserverURLChecker unmarshalled = xp.load(fis, GeoserverURLChecker.class);
            if (unmarshalled.getRegexList() == null)
                unmarshalled.setRegexList(new ArrayList<URLEntry>());
            return unmarshalled;
        }
    }

    public synchronized GeoserverURLChecker reload() throws Exception {
        geoserverURLChecker = read(xmlFile);
        return geoserverURLChecker;
    }

    private synchronized void registerSPI(GeoserverURLChecker geoserverURLChecker) {
        // remove existing
        URLCheckers.removeURLChecker(geoserverURLChecker);
        URLCheckers.addURLChecker(geoserverURLChecker);
    }

    /** @return the geoserverURLChecker */
    public GeoserverURLChecker getGeoserverURLChecker() {
        return geoserverURLChecker;
    }

    /**
     * @return the geoserverURLChecker
     * @throws Exception
     */
    public GeoserverURLChecker getGeoserverURLCheckerCopy() throws Exception {
        return (GeoserverURLChecker) geoserverURLChecker.clone();
    }
}
