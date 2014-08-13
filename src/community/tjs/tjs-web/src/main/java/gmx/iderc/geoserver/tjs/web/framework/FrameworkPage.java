/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.framework;

import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.web.TJSSelectionRemovalLink;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

import java.util.logging.Level;
import java.util.logging.Logger;

import static gmx.iderc.geoserver.tjs.web.framework.FrameworkProvider.*;

/**
 * Page listing all the available frameworks. Follows the usual filter/sort/page approach,
 * provides ways to bulk delete frameworks and to add new ones
 */
@SuppressWarnings("serial")
public class FrameworkPage extends GeoServerSecuredPage {
    FrameworkProvider provider = new FrameworkProvider();
    GeoServerTablePanel<FrameworkInfo> table;
    GeoServerDialog dialog;
    TJSSelectionRemovalLink removal;

    public FrameworkPage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table = new GeoServerTablePanel<FrameworkInfo>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                                                        Property<FrameworkInfo> property) {
                if (property == FEATURE_TYPE) {
                    return featureTypeLink(id, itemModel);
                } else if (property == NAME) {
                    return frameworkLink(id, itemModel);
                } else if (property == ENABLED) {
                    FrameworkInfo frameworkInfo = (FrameworkInfo) itemModel.getObject();
                    // ask for enabled() instead of isEnabled() to account for disabled resource/store
                    boolean enabled = frameworkInfo.getEnabled();
                    ResourceReference icon = enabled ? icons.getEnabledIcon() : icons.getDisabledIcon();
                    Fragment f = new Fragment(id, "iconFragment", FrameworkPage.this);
                    f.add(new Image("frameworkIcon", icon));
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
        Logger.getLogger(FrameworkPage.class.getName()).log(Level.INFO, "Terminando Render de la pagina de Frameworks");
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", FrameworkNewPage.class));

        // the removal button
        header.add(removal = new TJSSelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    private Component frameworkLink(String id, final IModel model) {
        IModel nameModel = NAME.getModel(model);
        return new SimpleBookmarkableLink(id, FrameworkEditPage.class, nameModel,
                                                 "name", (String) nameModel.getObject());
    }

    private Component featureTypeLink(String id, final IModel model) {
        FrameworkInfo frameworkInfo = (FrameworkInfo) model.getObject();
        IModel layerNameModel = NAME.getModel(model);
        FeatureTypeInfo ftInfo = frameworkInfo.getFeatureType();
        if (ftInfo != null) {
            String nsName = ftInfo.getNamespace().getName();
            String layerName = (String) layerNameModel.getObject();
            return new SimpleBookmarkableLink(id, ResourceConfigurationPage.class, layerNameModel,
                                                     ResourceConfigurationPage.NAME, layerName, ResourceConfigurationPage.WORKSPACE, nsName);
        } else {
            return null;
        }
    }

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
