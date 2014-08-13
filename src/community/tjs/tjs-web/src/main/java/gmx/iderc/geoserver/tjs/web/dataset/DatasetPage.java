/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.dataset;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.web.TJSSelectionRemovalLink;
import gmx.iderc.geoserver.tjs.web.data.DataStoreEditPage;
import gmx.iderc.geoserver.tjs.web.framework.FrameworkEditPage;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static gmx.iderc.geoserver.tjs.web.dataset.DatasetProvider.*;

//import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;

/**
 * Page listing all the available frameworks. Follows the usual filter/sort/page approach,
 * provides ways to bulk delete frameworks and to add new ones
 */
@SuppressWarnings("serial")
public class DatasetPage extends GeoServerSecuredPage {
    DatasetProvider provider = new DatasetProvider();
    GeoServerTablePanel<DatasetInfo> table;
    GeoServerDialog dialog;
    TJSSelectionRemovalLink removal;
    static final Logger LOGGER = Logger.getLogger(DatasetPage.class.getName());

    public DatasetPage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table = new GeoServerTablePanel<DatasetInfo>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                                                        Property<DatasetInfo> property) {
                if (property == NAME) {
                    return datasetLink(id, itemModel);
                } else if (property == TITLE) {
                    return new Label(id, TITLE.getModel(itemModel));
                } else if (property == ORGANIZATION) {
                    return new Label(id, ORGANIZATION.getModel(itemModel));
                } else if (property == REF_DATE) {
                    Date refDate = (Date) (REF_DATE.getModel(itemModel).getObject());
                    if (refDate == null)
                        return new Label(id);
                    else
                        return new Label(id, DateFormatUtils.format(refDate, "dd-MM-yyyy"));
                } else if (property == VERSION) {
                    return new Label(id, VERSION.getModel(itemModel));
                } else if (property == DATASTORE) {
                    return dataStoreLink(id, itemModel);
                } else if (property == FRAMEWORK) {
                    return frameworkLink(id, itemModel);
                } else if (property == ENABLED) {
                    // ask for enabled() instead of isEnabled() to account for disabled resource/store
                    boolean enabled = ((Boolean) ENABLED.getModel(itemModel).getObject()).booleanValue();
                    ResourceReference icon = enabled ? icons.getEnabledIcon() : icons.getDisabledIcon();
                    Fragment f = new Fragment(id, "iconFragment", DatasetPage.this);
                    f.add(new Image("datasetIcon", icon));
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
        LOGGER.log(Level.INFO, "Terminando Render de la pagina de Datasets");
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", NewTJSDatasetPage.class));

        // the removal button
        header.add(removal = new TJSSelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    private Component frameworkLink(String id, final IModel model) {
        IModel frameworkModel = FRAMEWORK.getModel(model);
        String frameworkName = (String) frameworkModel.getObject();
        return new SimpleBookmarkableLink(id, FrameworkEditPage.class, frameworkModel,
                                                 "name", frameworkName);
    }

    private Component datasetLink(String id, final IModel model) {
        IModel nameModel = NAME.getModel(model);
        IModel dataStoreModel = new PropertyModel(model, "dataStore");
        DataStoreInfo dsi = (DataStoreInfo) dataStoreModel.getObject();
        PageParameters params = new PageParameters();
        params.add("dataStoreId", dsi.getId());
        params.add("datasetName", nameModel.getObject().toString());
        return new SimpleBookmarkableLink(id, DatasetEditPage.class, nameModel,
                                                 params);
    }

    private Component dataStoreLink(String id, final IModel model) {
        IModel dataStoreModel = DATASTORE.getModel(model);
        String dataStoreName = (String) dataStoreModel.getObject();
        return new SimpleBookmarkableLink(id, DataStoreEditPage.class, dataStoreModel,
                                                 "name", dataStoreName);
    }

//    private Component featureTypeLink(String id, final IModel model) {
//        DatasetInfo datasetInfo = (DatasetInfo) model.getObject();
//        IModel layerNameModel = NAME.getModel(model);
//        FeatureTypeInfo ftInfo = datasetInfo.getFeatureType();
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
