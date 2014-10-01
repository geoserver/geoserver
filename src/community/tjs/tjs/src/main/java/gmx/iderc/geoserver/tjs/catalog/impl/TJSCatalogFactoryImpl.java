/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogFactory;
import org.geoserver.ows.util.OwsUtils;

import java.rmi.server.UID;

/**
 * @author root
 */
public class TJSCatalogFactoryImpl implements TJSCatalogFactory {

    TJSCatalogImpl catalog;

    static public String getIdForObject(Object o) {
        String uid = new UID().toString();
        return o.getClass().getSimpleName() + "-" + uid;
    }

    public Object setId(Object o) {
        if (OwsUtils.get(o, "id") == null) {
            OwsUtils.set(o, "id", getIdForObject(o));
            return o;
        } else {
            return o;
        }
    }

    public TJSCatalogFactoryImpl(TJSCatalogImpl catalog) {
        this.catalog = catalog;
    }

    public FrameworkInfo newFrameworkInfo() {
        return (FrameworkInfo) setId(new FrameworkInfoImpl(this.catalog));
    }

    public DatasetInfo newDataSetInfo() {
        return (DatasetInfo) setId(new DatasetInfoImpl(this.catalog));
    }

    public DataStoreInfo newDataStoreInfo() {
        return (DataStoreInfo) setId(new DataStoreInfoImpl(this.catalog));
    }

}
