/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogPersistence;

/**
 * @author root
 */
public class TJSExtension {

    public static final String TJS_TEMP_WORKSPACE = "__temp";
    static TJSCatalog catalog;

    public static TJSCatalog getTJSCatalog() {
        if (catalog == null) {
            catalog = TJSCatalogPersistence.load();
        }
        return catalog;
    }
}
