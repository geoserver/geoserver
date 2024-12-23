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
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.security.csp.CSPPolicy;
import org.geoserver.security.csp.CSPRule;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** Panel for {@link CSPRule} objects. */
public class CSPRulePanel extends Panel {

    private static final long serialVersionUID = 6368251831224251873L;

    private static final Property<CSPRule> ENABLED = new BeanProperty<>("enabled", "enabled");
    private static final Property<CSPRule> NAME = new BeanProperty<>("name", "name");
    private static final Property<CSPRule> DESCRIPTION = new BeanProperty<>("description", "description");
    private static final Property<CSPRule> FILTER = new BeanProperty<>("filter", "filter");
    private static final Property<CSPRule> DIRECTIVES = new BeanProperty<>("directives", "directives");
    private static final Property<CSPRule> REMOVE = new PropertyPlaceholder<>("remove");

    /** The properties for the rules table. */
    private static final List<Property<CSPRule>> PROPERTIES =
            List.of(ENABLED, NAME, DESCRIPTION, FILTER, DIRECTIVES, REMOVE);

    private CSPPolicy policy = null;

    private CSPRuleTablePanel tablePanel = null;

    public CSPRulePanel(String id, CSPPolicy policy) {
        super(id);
        this.policy = policy;
        add(new AjaxLink<Void>("add") {
            private static final long serialVersionUID = 7028097965705654491L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                CSPRulePage page = new CSPRulePage(new CSPRule(), policy);
                setResponsePage(page.setReturnPage(getPage()));
            }
        });
        this.tablePanel = new CSPRuleTablePanel("table", policy.getRules());
        add(this.tablePanel);
    }

    /**
     * Gets a fresh list of properties to avoid serialization issues.
     *
     * @return the properties
     */
    private static IModel<List<Property<CSPRule>>> getProperties() {
        return new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1880449575748893130L;

            @Override
            protected List<Property<CSPRule>> load() {
                return PROPERTIES;
            }
        };
    }

    private class CSPRuleTablePanel extends ReorderableTablePanel<CSPRule> {

        private static final long serialVersionUID = -4762272059375502701L;

        public CSPRuleTablePanel(String id, List<CSPRule> rules) {
            super(id, CSPRule.class, rules, getProperties());
            setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());
            setFilterable(false);
            setPageable(false);
        }

        @Override
        protected Component getComponentForProperty(String id, IModel<CSPRule> itemModel, Property<CSPRule> property) {
            if (property == NAME) {
                return editLink(id, itemModel, property.getModel(itemModel));
            } else if (property == DESCRIPTION) {
                return new Icon(
                                id,
                                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/information.png"),
                                Model.of((String) property.getModel(itemModel).getObject()))
                        .setOutputMarkupId(true);
            } else if (property == REMOVE) {
                return removeLink(id, itemModel.getObject());
            } else if (Boolean.TRUE.equals(property.getModel(itemModel).getObject())) {
                return new Icon(id, CatalogIconFactory.ENABLED_ICON);
            } else if (Boolean.FALSE.equals(property.getModel(itemModel).getObject())) {
                return new Label(id, "");
            }
            return new Label(id, property.getModel(itemModel));
        }

        private Component editLink(String id, IModel<CSPRule> model, IModel<?> label) {
            return new SimpleAjaxLink<>(id, model, label) {
                private static final long serialVersionUID = 1567366293977781250L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    CSPPolicy policy = CSPRulePanel.this.policy;
                    CSPRulePage page = new CSPRulePage(model.getObject(), policy);
                    setResponsePage(page.setReturnPage(getPage()));
                }
            };
        }

        private Component removeLink(String id, CSPRule rule) {
            ImageAjaxLink<Void> link =
                    new ImageAjaxLink<>(id, new PackageResourceReference(getClass(), "../img/icons/silk/delete.png")) {
                        private static final long serialVersionUID = -3140594684451087223L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            CSPRulePanel.this.policy.getRules().remove(rule);
                            target.add(CSPRulePanel.this.tablePanel);
                        }
                    };
            link.getImage().add(new AttributeModifier("alt", new ParamResourceModel("th.remove", CSPRulePanel.this)));
            return link;
        }
    }
}
