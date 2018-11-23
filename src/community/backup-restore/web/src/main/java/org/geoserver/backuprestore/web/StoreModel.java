/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/** Detachable model for a specific store. */
public class StoreModel<T extends StoreInfo> extends LoadableDetachableModel<T> {

    private static final long serialVersionUID = -2008934085507417813L;

    private ResourceFilePanel resourceFilePanel;
    IModel workspace;
    String name;

    private StoreModel(T store) {
        super(store);
        setObject(store);
    }

    public StoreModel(ResourceFilePanel resourceFilePanel, T store) {
        this(store);
        this.resourceFilePanel = resourceFilePanel;
    }

    public void setObject(T store) {
        super.setObject(store);
        if (store != null) {
            workspace = new WorkspaceModel(resourceFilePanel, store.getWorkspace());
            name = store.getName();
        } else {
            name = null;
        }
    };

    @Override
    protected T load() {
        if (workspace == null) {
            return null;
        }
        if (name == null) {
            return null;
        }

        if (resourceFilePanel != null
                && resourceFilePanel.getStores() != null
                && !resourceFilePanel.getStores().isEmpty()
                && workspace.getObject() != null
                && resourceFilePanel
                        .getStores()
                        .containsKey(((WorkspaceInfo) workspace.getObject()).getName())) {
            for (StoreInfo st :
                    resourceFilePanel
                            .getStores()
                            .get(((WorkspaceInfo) workspace.getObject()).getName())) {
                if (st.getName().equals(name)) {
                    return (T) st;
                }
            }
        }

        StoreInfo store =
                (T)
                        GeoServerApplication.get()
                                .getCatalog()
                                .getStoreByName(
                                        (WorkspaceInfo) workspace.getObject(),
                                        name,
                                        StoreInfo.class);
        if (store != null) {
            return (T) store;
        }
        return getObject();
    }

    /* (non-Javadoc)
     * @see org.apache.wicket.model.LoadableDetachableModel#detach()
     */
    @Override
    public void detach() {
        super.detach();
        if (workspace != null) {
            workspace.detach();
        }
    }
}
