/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.csp;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.security.csp.CSPConfiguration;
import org.geoserver.security.csp.CSPPolicy;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** Panel for {@link CSPPolicy} objects. */
public class CSPPolicyPanel extends Panel {

    private static final long serialVersionUID = -8329354368660703089L;

    private static final Property<CSPPolicy> ENABLED = new BeanProperty<>("enabled", "enabled");
    private static final Property<CSPPolicy> NAME = new BeanProperty<>("name", "name");
    private static final Property<CSPPolicy> DESCRIPTION = new BeanProperty<>("description", "description");
    private static final Property<CSPPolicy> REMOVE = new PropertyPlaceholder<>("remove");

    /** The properties for the policies table. */
    private static final List<Property<CSPPolicy>> PROPERTIES = List.of(ENABLED, NAME, DESCRIPTION, REMOVE);

    private CSPConfiguration config = null;

    private CSPPolicyTablePanel tablePanel = null;

    public CSPPolicyPanel(String id, CSPConfiguration config) {
        super(id);
        this.config = config;
        add(new AjaxLink<Void>("add") {
            private static final long serialVersionUID = 5518438243807007190L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                CSPPolicyPage page = new CSPPolicyPage(new CSPPolicy(), config);
                setResponsePage(page.setReturnPage(getPage()));
            }
        });
        this.tablePanel = new CSPPolicyTablePanel("table", config.getPolicies());
        add(this.tablePanel);
    }

    /**
     * Gets a fresh list of properties to avoid serialization issues.
     *
     * @return the properties
     */
    private static IModel<List<Property<CSPPolicy>>> getProperties() {
        return new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 6024865833524314857L;

            @Override
            protected List<Property<CSPPolicy>> load() {
                return PROPERTIES;
            }
        };
    }

    private class CSPPolicyTablePanel extends ReorderableTablePanel<CSPPolicy> {

        private static final long serialVersionUID = -3229289637490224342L;

        public CSPPolicyTablePanel(String id, List<CSPPolicy> rules) {
            super(id, CSPPolicy.class, rules, getProperties());
            setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());
            setFilterable(false);
            setPageable(false);
        }

        @Override
        protected Component getComponentForProperty(
                String id, IModel<CSPPolicy> itemModel, Property<CSPPolicy> property) {
            if (property == NAME) {
                return editLink(id, itemModel, property.getModel(itemModel));
            } else if (property == REMOVE) {
                return removeLink(id, itemModel.getObject());
            } else if (Boolean.TRUE.equals(property.getModel(itemModel).getObject())) {
                return new Icon(id, CatalogIconFactory.ENABLED_ICON);
            } else if (Boolean.FALSE.equals(property.getModel(itemModel).getObject())) {
                return new Label(id, "");
            }
            return new Label(id, property.getModel(itemModel));
        }

        private Component editLink(String id, IModel<CSPPolicy> model, IModel<?> label) {
            return new SimpleAjaxLink<>(id, model, label) {
                private static final long serialVersionUID = -7009235253455625060L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    CSPConfiguration config = CSPPolicyPanel.this.config;
                    CSPPolicyPage page = new CSPPolicyPage(model.getObject(), config);
                    setResponsePage(page.setReturnPage(getPage()));
                }
            };
        }

        private Component removeLink(String id, CSPPolicy policy) {
            ImageAjaxLink<Void> link =
                    new ImageAjaxLink<>(id, new PackageResourceReference(getClass(), "../img/icons/silk/delete.png")) {
                        private static final long serialVersionUID = 190400999968840349L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            CSPPolicyPanel.this.config.getPolicies().remove(policy);
                            target.add(CSPPolicyPanel.this.tablePanel);
                        }
                    };
            link.getImage().add(new AttributeModifier("alt", new ParamResourceModel("th.remove", CSPPolicyPanel.this)));
            return link;
        }
    }
}
