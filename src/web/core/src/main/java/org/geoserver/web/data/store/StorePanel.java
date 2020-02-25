/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.geoserver.web.data.store.StoreProvider.ENABLED;
import static org.geoserver.web.data.store.StoreProvider.NAME;
import static org.geoserver.web.data.store.StoreProvider.WORKSPACE;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.ConfirmationAjaxLink;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/**
 * Panel listing the configured StoreInfo object on a table
 *
 * @author Justin Deoliveira
 * @author Gabriel Roldan
 * @version $Id$
 * @see StorePage
 * @see StoreProvider
 */
@SuppressWarnings("serial")
public class StorePanel extends GeoServerTablePanel<StoreInfo> {

    private static final long serialVersionUID = 5957961031378924960L;

    private ModalWindow popupWindow;

    public StorePanel(String id, StoreProvider provider, boolean selectable) {
        super(id, provider, selectable);

        // the popup window for messages
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);
    }

    private Catalog getCatalog() {
        return GeoServerApplication.get().getCatalog();
    }

    @Override
    protected Component getComponentForProperty(
            String id, IModel<StoreInfo> itemModel, Property<StoreInfo> property) {

        final CatalogIconFactory icons = CatalogIconFactory.get();

        if (property == StoreProvider.DATA_TYPE) {
            final StoreInfo storeInfo = (StoreInfo) itemModel.getObject();

            PackageResourceReference storeIcon = icons.getStoreIcon(storeInfo);

            Fragment f = new Fragment(id, "iconFragment", this);
            f.add(new Image("storeIcon", storeIcon));

            return f;
        } else if (property == WORKSPACE) {
            return workspaceLink(id, itemModel);
        } else if (property == NAME) {
            return storeNameLink(id, itemModel);
        } else if (property == ENABLED) {
            final StoreInfo storeInfo = (StoreInfo) itemModel.getObject();
            PackageResourceReference enabledIcon;
            if (storeInfo.isEnabled()) {
                enabledIcon = icons.getEnabledIcon();
            } else {
                enabledIcon = icons.getDisabledIcon();
            }
            Fragment f = new Fragment(id, "iconFragment", this);
            f.add(new Image("storeIcon", enabledIcon));
            return f;
        } else if (property == StoreProvider.MODIFIED_TIMESTAMP) {
            return new DateTimeLabel(id, StoreProvider.MODIFIED_TIMESTAMP.getModel(itemModel));
        } else if (property == StoreProvider.CREATED_TIMESTAMP) {
            return new DateTimeLabel(id, StoreProvider.CREATED_TIMESTAMP.getModel(itemModel));
        }
        return null;
    }

    private Component storeNameLink(String id, final IModel itemModel) {
        String wsName = (String) WORKSPACE.getModel(itemModel).getObject();
        IModel storeNameModel = NAME.getModel(itemModel);
        String storeName = (String) storeNameModel.getObject();
        StoreInfo store = getCatalog().getStoreByName(wsName, storeName, StoreInfo.class);
        if (store instanceof DataStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    DataAccessEditPage.class,
                    storeNameModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else if (store instanceof CoverageStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    CoverageStoreEditPage.class,
                    storeNameModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else if (store instanceof WMSStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    WMSStoreEditPage.class,
                    storeNameModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else if (store instanceof WMTSStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    WMTSStoreEditPage.class,
                    storeNameModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else {
            throw new RuntimeException("Don't know what to do with this store " + store);
        }
    }

    private Component workspaceLink(String id, IModel itemModel) {
        IModel nameModel = WORKSPACE.getModel(itemModel);
        return new SimpleBookmarkableLink(
                id, WorkspaceEditPage.class, nameModel, "name", (String) nameModel.getObject());
    }

    protected Component removeLink(String id, final IModel itemModel) {
        StoreInfo info = (StoreInfo) itemModel.getObject();

        ResourceModel resRemove = new ResourceModel("removeStore", "Remove");

        ParamResourceModel confirmRemove =
                new ParamResourceModel("confirmRemoveStoreX", this, info.getName());

        SimpleAjaxLink linkPanel =
                new ConfirmationAjaxLink(id, null, resRemove, confirmRemove) {
                    public void onClick(AjaxRequestTarget target) {
                        getCatalog().remove((StoreInfo) itemModel.getObject());
                        target.add(StorePanel.this);
                    }
                };
        return linkPanel;
    }
}
