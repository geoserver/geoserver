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

/** Detachable model for a specific layer. */
public class LayerModel<T extends LayerInfo> extends LoadableDetachableModel<T> {

    private static final long serialVersionUID = 1619470274815042758L;

    private ResourceFilePanel resourceFilePanel;
    IModel workspace;
    IModel store;
    String name;

    private LayerModel(T store) {
        super(store);
        setObject(store);
    }

    public LayerModel(ResourceFilePanel resourceFilePanel, T layer) {
        this(layer);
        this.resourceFilePanel = resourceFilePanel;
    }

    public void setObject(T object) {
        super.setObject(object);
        if (object != null) {
            workspace =
                    new WorkspaceModel(
                            resourceFilePanel, object.getResource().getStore().getWorkspace());
            store = new StoreModel(resourceFilePanel, object.getResource().getStore());
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

        if (resourceFilePanel != null
                && resourceFilePanel.getLayers() != null
                && !resourceFilePanel.getLayers().isEmpty()
                && store.getObject() != null
                && resourceFilePanel
                        .getLayers()
                        .containsKey(((StoreInfo) store.getObject()).getName())) {
            for (LayerInfo ly :
                    resourceFilePanel.getLayers().get(((StoreInfo) store.getObject()).getName())) {
                if (ly.getName().equals(name)) {
                    return (T) ly;
                }
            }
        }

        LayerInfo li = GeoServerApplication.get().getCatalog().getLayerByName(name);
        if (li != null && li.getResource() != null && li.getResource().getStore() != null) {
            if (li.getResource()
                    .getStore()
                    .getName()
                    .equals(((StoreInfo) store.getObject()).getName())) {
                return (T) li;
            }
        }
        return getObject();
    }

    /* (non-Javadoc)
     * @see org.apache.wicket.model.LoadableDetachableModel#detach()
     */
    @Override
    public void detach() {
        super.detach();
        if (store != null) {
            store.detach();
        }
    }
}
