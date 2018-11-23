/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.StoreModel;

/** Detachable model for a specific layer. */
@SuppressWarnings("serial")
public class LayerModel<T extends LayerInfo> extends LoadableDetachableModel<T> {

    IModel store;
    String name;

    public LayerModel(T store) {
        super(store);
        setObject(store);
    }

    public void setObject(T object) {
        super.setObject(object);
        if (object != null) {
            store = new StoreModel(object.getResource().getStore());
            name = object.getName();
        } else {
            name = null;
        }
    };

    @Override
    protected T load() {
        if (store == null) {
            return null;
        }
        if (name == null) {
            return null;
        }
        LayerInfo li = GeoServerApplication.get().getCatalog().getLayerByName(name);
        if (li.getResource() != null && li.getResource().getStore() != null) {
            if (li.getResource()
                    .getStore()
                    .getName()
                    .equals(((StoreInfo) store.getObject()).getName())) {
                return (T) li;
            }
        }
        return null;
    }
}
