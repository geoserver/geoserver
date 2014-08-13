/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * @author root
 */
public class TJSBasePage extends GeoServerSecuredPage {

    protected TJSCatalog getTJSCatalog() {
        return TJSExtension.getTJSCatalog();
    }

}
