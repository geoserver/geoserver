/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogPersistence;

import java.io.File;
import java.util.HashMap;

/**
 * @author root
 */
public class TJSExtensionTestSupport {

    static HashMap<String, TJSCatalog> testcatalogs = new HashMap<String, TJSCatalog>();

    public static TJSCatalog getTJSCatalog(File dataDirectory) {
        String key = dataDirectory.toString();
        TJSCatalog catalog = null;
        if (!testcatalogs.containsKey(key)) {
            catalog = TJSCatalogPersistence.load(dataDirectory);
            for (FrameworkInfo framework : catalog.getFrameworks()) {
                framework.setCatalog(catalog);
            }
            testcatalogs.put(key, catalog);
        } else {
            catalog = testcatalogs.get(key);
        }
        return catalog;
    }

}
