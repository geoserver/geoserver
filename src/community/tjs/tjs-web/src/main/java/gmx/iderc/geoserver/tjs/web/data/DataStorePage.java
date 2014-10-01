/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.web.TJSSelectionRemovalLink;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

import java.util.logging.Level;
import java.util.logging.Logger;

import static gmx.iderc.geoserver.tjs.web.data.DataStoreProvider.*;

/**
 * Page listing all the available frameworks. Follows the usual filter/sort/page approach,
 * provides ways to bulk delete frameworks and to add new ones
 */
@SuppressWarnings("serial")
public class DataStorePage extends GeoServerSecuredPage {
    DataStoreProvider provider = new DataStoreProvider();
    GeoServerTablePanel<DataStoreInfo> table;
    GeoServerDialog dialog;
    TJSSelectionRemovalLink removal;
    static final Logger LOGGER = Logger.getLogger(DataStorePage.class.getName());

    public DataStorePage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table = new GeoServerTablePanel<DataStoreInfo>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                                                        Property<DataStoreInfo> property) {
                if (property == NAME) {
                    return dataStoreLink(id, itemModel);
                } else if (property == TYPE) {
                    return new Label(id, TYPE.getModel(itemModel));
//                    return featureTypeLink(id, itemModel);
                } else if (property == ENABLED) {
                    DataStoreInfo DataStoreInfo = (DataStoreInfo) itemModel.getObject();
                    // ask for enabled() instead of isEnabled() to account for disabled resource/store
                    boolean enabled = DataStoreInfo.getEnabled();
                    ResourceReference icon = enabled ? icons.getEnabledIcon() : icons.getDisabledIcon();
                    Fragment f = new Fragment(id, "iconFragment", DataStorePage.this);
                    f.add(new Image("dataStoreIcon", icon));
                    return f;
                }
                throw new IllegalArgumentException("Don't know a property named " + property.getName());
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(table.getSelection().size() > 0);
                target.addComponent(removal);
            }

        };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
        LOGGER.log(Level.INFO, "Terminando Render de la pagina de DataStores");
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", NewTJSDataPage.class));

        // the removal button
        header.add(removal = new TJSSelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    private Component dataStoreLink(String id, final IModel model) {
        IModel nameModel = NAME.getModel(model);
//        return new Label(id, NAME.getModel(nameModel));
        return new SimpleBookmarkableLink(id, DataStoreEditPage.class, nameModel,
                                                 "name", (String) nameModel.getObject());
    }

//    private Component featureTypeLink(String id, final IModel model) {
//        DataStoreInfo DataStoreInfo = (DataStoreInfo) model.getObject();
//        IModel layerNameModel = NAME.getModel(model);
//        FeatureTypeInfo ftInfo = DataStoreInfo.getFeatureType();
//        if (ftInfo != null){
//            String nsName = ftInfo.getNamespace().getName();
//            String layerName = (String) layerNameModel.getObject();
//            return new SimpleBookmarkableLink(id, ResourceConfigurationPage.class, layerNameModel,
//                    ResourceConfigurationPage.NAME, layerName, ResourceConfigurationPage.WORKSPACE, nsName);
//        }else{
//            return null;
//        }
//    }

/*
    private Component storeLink(String id, final IModel model) {
        IModel storeModel = STORE.getModel(model);
        String wsName = (String) WORKSPACE.getModel(model).getObject();
        String storeName = (String) storeModel.getObject();
        StoreInfo store = getCatalog().getStoreByName(wsName, storeName, StoreInfo.class);
        if(store instanceof DataStoreInfo) {
            return new SimpleBookmarkableLink(id, DataAccessEditPage.class, storeModel,
                    DataAccessEditPage.STORE_NAME, storeName,
                    DataAccessEditPage.WS_NAME, wsName);
        } else if (store instanceof WMSStoreInfo) {
            return new SimpleBookmarkableLink(id, WMSStoreEditPage.class, storeModel,
                    DataAccessEditPage.STORE_NAME, storeName,
                    DataAccessEditPage.WS_NAME, wsName);
        } else {
            return new SimpleBookmarkableLink(id, CoverageStoreEditPage.class, storeModel,
                    DataAccessEditPage.STORE_NAME, storeName,
                    DataAccessEditPage.WS_NAME, wsName);
        }
    }
*/

}
