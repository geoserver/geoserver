/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleAjaxLink;

@SuppressWarnings("serial")
public class UserTablePanel extends GeoServerTablePanel<GeoServerUser> {

    String ugServiceName;

    public UserTablePanel(
            String id, String ugServiceName, GeoServerDataProvider<GeoServerUser> dataProvider) {
        super(id, dataProvider);
        this.ugServiceName = ugServiceName;
    }

    public UserTablePanel(
            String id,
            String ugServiceName,
            GeoServerDataProvider<GeoServerUser> dataProvider,
            boolean selectable) {
        super(id, dataProvider, selectable);
        this.ugServiceName = ugServiceName;
        setItemReuseStrategy(new DefaultItemReuseStrategy());
    }

    @Override
    protected Component getComponentForProperty(
            String id, IModel<GeoServerUser> itemModel, Property<GeoServerUser> property) {

        if (property == UserListProvider.USERNAME) {
            return editUserLink(id, itemModel, property);
        } else if (property == UserListProvider.ENABLED) {
            if ((Boolean) property.getModel(itemModel).getObject())
                return new Icon(id, CatalogIconFactory.ENABLED_ICON);
            else return new Label(id, "");
        } else if (property == UserListProvider.HASATTRIBUTES) {
            if ((Boolean) property.getModel(itemModel).getObject())
                return new Icon(id, CatalogIconFactory.ENABLED_ICON);
            else return new Label(id, "");
        }
        throw new RuntimeException("Uknown property " + property);
    }

    protected Component editUserLink(
            String id, IModel itemModel, Property<GeoServerUser> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(
                        new EditUserPage(ugServiceName, (GeoServerUser) getDefaultModelObject())
                                .setReturnPage(getPage()));
            }
        };
    }
}
