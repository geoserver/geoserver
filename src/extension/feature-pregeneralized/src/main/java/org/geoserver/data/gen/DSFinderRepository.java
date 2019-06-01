/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.data.gen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import org.apache.log4j.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geotools.data.DataStore;
import org.geotools.data.Repository;
import org.opengis.feature.type.Name;

/**
 * Implementation of {@link Repository}
 *
 * <p>The class makes a lookup in the GeoServer catalog.
 *
 * <p>If nothing is found, the class interprets the data source name as a file name or an URL for a
 * property file containing the data source creation parameters
 *
 * <p>For shape files ending with .shp or SHP, the shape file could be passed as name
 *
 * @author Christian Mueller
 */
public class DSFinderRepository extends org.geotools.data.gen.DSFinderRepository {

    protected URL getURLForLocation(String location) throws IOException {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File f =
                Resources.find(
                        Resources.fromURL(Files.asResource(loader.getBaseDirectory()), location),
                        true);
        URL url = null;
        if (f.exists()) {
            url = f.toURI().toURL();
        } else {
            url = new URL(location);
        }
        url = new URL(URLDecoder.decode(url.toExternalForm(), "UTF8"));
        return url;
    }

    @Override
    public DataStore dataStore(Name name) {
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        DataStoreInfo info =
                catalog.getDataStoreByName(name.getNamespaceURI(), name.getLocalPart());
        if (info != null) {
            try {
                return (DataStore) info.getDataStore(null);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        Logger.getLogger(this.getClass().getName())
                .info("Not in Geoserver catalog: " + name.toString());
        return super.dataStore(name);
    }
}
