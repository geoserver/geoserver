/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.catalog.Catalog;

public class TriggerFileWatcherFactory {
    private static final Log logger = LogFactory.getLog(WFSNotify.class);
    final Catalog catalog;

    public TriggerFileWatcherFactory(Catalog c) {
        catalog = c;
    }

    public TriggerFileWatcher createTriggerFileWatcher() throws IOException {
        File file = null;

        try {
            file = catalog.getResourceLoader().find("data/triggers.xml");
        } catch(FileNotFoundException e) {
            logger
                .info("No triggers.xml found in GeoServer data directory\nI can look for it here: " +
                    (file = new File(catalog.getResourceLoader().getBaseDirectory(),
                        "data/triggers.xml")));
        }

        return new TriggerFileWatcher(60000, file == null ? null : file.toURI().toURL());
    }
}
