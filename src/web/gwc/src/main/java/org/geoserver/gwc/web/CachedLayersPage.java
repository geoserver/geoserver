/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static org.geoserver.gwc.web.CachedLayerProvider.ENABLED;
import static org.geoserver.gwc.web.CachedLayerProvider.NAME;
import static org.geoserver.gwc.web.CachedLayerProvider.QUOTA_LIMIT;
import static org.geoserver.gwc.web.CachedLayerProvider.QUOTA_USAGE;
import static org.geoserver.gwc.web.CachedLayerProvider.TYPE;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.gwc.web.CachedLayerInfo.TYPE;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geowebcache.diskquota.storage.Quota;

public class CachedLayersPage extends GeoServerSecuredPage {
    private CachedLayerProvider provider = new CachedLayerProvider();

    private GeoServerTablePanel<CachedLayerInfo> table;

    private GeoServerDialog dialog;

    private CachedLayerSelectionRemovalLink removal;

    public CachedLayersPage() {
        final GWCIconFactory icons = GWCIconFactory.get();
        table = new GeoServerTablePanel<CachedLayerInfo>("table", provider, true) {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<CachedLayerInfo> property) {

                if (property == TYPE) {
                    Fragment f = new Fragment(id, "iconFragment", CachedLayersPage.this);
                    CachedLayerInfo layerInfo = (CachedLayerInfo) itemModel.getObject();
                    TYPE type = layerInfo.getType();
                    ResourceReference layerIcon = icons.getSpecificLayerIcon(type);
                    f.add(new Image("layerIcon", layerIcon));
                    return f;
                } else if (property == NAME) {
                    return cachedLayerLink(id, itemModel);
                } else if (property == QUOTA_LIMIT) {
                    IModel<Quota> quotaLimitModel = property.getModel(itemModel);
                    return quotaLink(id, quotaLimitModel);
                } else if (property == QUOTA_USAGE) {
                    IModel<Quota> quotaUsageModel = property.getModel(itemModel);
                    return quotaLink(id, quotaUsageModel);
                } else if (property == ENABLED) {
                    CachedLayerInfo layerInfo = (CachedLayerInfo) itemModel.getObject();
                    boolean enabled = layerInfo.isEnabled();
                    ResourceReference icon;
                    if (enabled) {
                        icon = icons.getEnabledIcon();
                    } else if (layerInfo.getConfigErrorMessage() != null) {
                        icon = icons.getErrorIcon();
                    } else {
                        icon = icons.getDisabledIcon();
                    }
                    Fragment f = new Fragment(id, "iconFragment", CachedLayersPage.this);
                    f.add(new Image("layerIcon", icon));
                    return f;
                }
                throw new IllegalArgumentException("Don't know a property named "
                        + property.getName());
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

    private Component quotaLink(String id, IModel<Quota> quotaModel) {
        Quota quota = quotaModel.getObject();
        String formattedQuota;
        if (null == quota) {
            formattedQuota = new ResourceModel("CachedLayersPage.quotaLimitNotSet").getObject();
        } else {
            formattedQuota = quota.toNiceString();
        }
        return new Label(id, formattedQuota);
    }

    @SuppressWarnings("unchecked")
    private Component cachedLayerLink(String id, IModel<CachedLayerInfo> itemModel) {
        IModel<String> nameModel = NAME.getModel(itemModel);
        String layerName = nameModel.getObject();
        // return new SimpleBookmarkableLink(id, CachedLayerEditPage.class, nameModel, "name",
        // layerName);
        Label link = new Label(id, layerName);
        String configErrorMessage = itemModel.getObject().getConfigErrorMessage();
        if (configErrorMessage != null) {
            link.add(new AttributeModifier("style", true, new Model<String>(
                    "text-decoration: line-through; font-style: italic;")));
            link.add(new AttributeModifier("title", true, new Model<String>(configErrorMessage)));
        }
        return link;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", NewLayerPage.class));

        // the removal button
        header.add(removal = new CachedLayerSelectionRemovalLink("removeSelected", table, dialog));
        // removal.setOutputMarkupId(true);
        // removal.setEnabled(false);

        return header;
    }

    private static class CachedLayerSelectionRemovalLink extends AjaxLink<CachedLayerInfo> {

        private static final long serialVersionUID = 1L;

        public CachedLayerSelectionRemovalLink(String string,
                GeoServerTablePanel<CachedLayerInfo> table, GeoServerDialog dialog) {
            super(string);
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            // TODO Auto-generated method stub

        }
    }

}
