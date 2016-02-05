/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.params.extractor.Utils;
import org.geoserver.web.GeoServerSecuredPage;

import java.util.ArrayList;
import java.util.List;

public class ParamsExtractorRulePage extends GeoServerSecuredPage {

    public ParamsExtractorRulePage(RuleModel optionalRuleModel) {
        final CompoundPropertyModel<RuleModel> simpleRuleModel = new CompoundPropertyModel<>(Utils.withDefault(optionalRuleModel, new RuleModel()));
        final CompoundPropertyModel<RuleModel> complexRuleModel = new CompoundPropertyModel<>(Utils.withDefault(optionalRuleModel, new RuleModel()));
        Form<RuleModel> form = new Form<>("form");
        add(form);
        final List<ITab> tabs = new ArrayList<>();
        if (optionalRuleModel == null || optionalRuleModel.getPosition() != null) {
            tabs.add(new WrappedTab("Basic Rule", simpleRuleModel) {
                public Panel getPanel(String panelId) {
                    return new SimpleRulePanel(panelId, simpleRuleModel);
                }
            });
        }
        if (optionalRuleModel == null || optionalRuleModel.getMatch() != null) {
            tabs.add(new WrappedTab("Advanced Rule", complexRuleModel) {
                public Panel getPanel(String panelId) {
                    return new ComplexRulePanel(panelId, complexRuleModel);
                }
            });
        }
        final AjaxTabbedPanel tabbedPanel = new AjaxTabbedPanel("tabs", tabs);
        form.add(tabbedPanel);
        form.add(new SubmitLink("save") {
            @Override
            public void onSubmit() {
                try {
                    WrappedTab selectedTab = (WrappedTab) tabs.get(tabbedPanel.getSelectedTab());
                    RuleModel ruleModel = selectedTab.getModel().getObject();
                    RulesModel.saveOrUpdate(ruleModel);
                    doReturn(ParamsExtractorConfigPage.class);
                } catch (Exception exception) {
                    error(exception);
                }
            }
        });
        form.add(new BookmarkablePageLink("cancel", ParamsExtractorConfigPage.class));
    }

    public abstract class WrappedTab extends AbstractTab {

        private final IModel<RuleModel> model;

        public WrappedTab(String title, IModel<RuleModel> model) {
            super(new Model<>(title));
            this.model = model;
        }

        public IModel<RuleModel> getModel() {
            return model;
        }
    }

    public class SimpleRulePanel extends Panel {

        public SimpleRulePanel(String panelId, IModel<RuleModel> model) {
            super(panelId, model);
            add(new TextField<Integer>("position").setRequired(true).setType(Integer.class));
            add(new TextField<String>("parameter").setRequired(true));
            add(new TextField<String>("transform").setRequired(true));
        }
    }

    public class ComplexRulePanel extends Panel {

        public ComplexRulePanel(String panelId, IModel<RuleModel> model) {
            super(panelId, model);
            add(new TextField<String>("match").setRequired(true));
            add(new TextField<String>("activation"));
            add(new TextField<String>("parameter").setRequired(true));
            add(new TextField<String>("transform").setRequired(true));
            add(new TextField<Integer>("remove").setType(Integer.class));
            add(new TextField<String>("combine"));
        }
    }
}
