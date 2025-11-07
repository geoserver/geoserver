/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.simulator;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.authorization.AuthorizationService;

/**
 * Panel to present the {@link AccessInfo} authorization result of an {@link AuthorizationService#getAccessInfo()}
 * request
 */
@SuppressWarnings("serial")
class AccessInfoPanel extends Panel {

    private AccessInfoFiltersTabbedPanel filterTabs;

    public AccessInfoPanel(String id, IModel<AccessInfo> model) {
        super(id, new CompoundPropertyModel<>(model));
        add(grant());
        add(catalogMode());
        add(filterTabs = new AccessInfoFiltersTabbedPanel("accessInfoTabbedPanel", model));
    }

    private Component grant() {
        return new Label("grant", model().bind("grant"));
    }

    private Component catalogMode() {
        return new Label("catalogMode", model().bind("catalogMode"));
    }

    public @Override void onModelChanged() {
        filterTabs.modelChanged();
    }

    @SuppressWarnings("unchecked")
    private CompoundPropertyModel<AccessInfo> model() {
        return (CompoundPropertyModel<AccessInfo>) super.getDefaultModel();
    }
}
