/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;
import org.geoserver.web.GeoServerSecuredPage;

/** The editing page for the {@link org.geoserver.proxybase.ext.ProxyBaseExtUrlMangler}. */
public class ProxyBaseExtensionRulePage extends GeoServerSecuredPage {
    public ProxyBaseExtensionRulePage(Optional<ProxyBaseExtensionRule> optionalRuleModel) {
        CompoundPropertyModel<ProxyBaseExtensionRule> simpleRuleModel =
                new CompoundPropertyModel<>(optionalRuleModel.orElse(new ProxyBaseExtensionRule()));
        Form<ProxyBaseExtensionRule> form = new Form<>("form");
        add(form);
        List<WrappedTab> tabs = new ArrayList<>();
        if (!optionalRuleModel.isPresent() || optionalRuleModel.get().getPosition() != null) {
            tabs.add(
                    new WrappedTab("Proxy Base Extension Rule", simpleRuleModel) {
                        @Override
                        public Panel getPanel(String panelId) {
                            return new SimpleRulePanel(panelId, simpleRuleModel);
                        }
                    });
        }
        AjaxTabbedPanel tabbedPanel = new AjaxTabbedPanel<>("tabs", tabs);
        form.add(tabbedPanel);
        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            WrappedTab selectedTab = tabs.get(tabbedPanel.getSelectedTab());
                            ProxyBaseExtensionRule ruleModel = selectedTab.getModel().getObject();
                            RulesDataProvider.saveOrUpdate(ruleModel);
                            doReturn(ProxyBaseExtensionConfigPage.class);
                        } catch (Exception exception) {
                            error(exception);
                        }
                    }
                });
        form.add(new BookmarkablePageLink<>("cancel", ProxyBaseExtensionConfigPage.class));
    }

    /** A tab that wraps a panel. */
    public abstract class WrappedTab extends AbstractTab {

        private final IModel<ProxyBaseExtensionRule> model;

        /**
         * Constructor.
         *
         * @param title the title
         * @param model the model
         */
        public WrappedTab(String title, IModel<ProxyBaseExtensionRule> model) {
            super(new Model<>(title));
            this.model = model;
        }

        /**
         * Returns the model.
         *
         * @return the model
         */
        public IModel<ProxyBaseExtensionRule> getModel() {
            return model;
        }
    }

    /** A simple rule panel. */
    public class SimpleRulePanel extends Panel {
        /**
         * Constructor.
         *
         * @param panelId the panel id
         * @param model the model
         */
        public SimpleRulePanel(String panelId, IModel<ProxyBaseExtensionRule> model) {
            super(panelId, model);
            add(new NumberTextField<Integer>("position").setMinimum(0).setRequired(true));
            add(new TextField<String>("matcher").setRequired(true));
            add(new TextField<String>("transformer").setRequired(true));
        }
    }
}
