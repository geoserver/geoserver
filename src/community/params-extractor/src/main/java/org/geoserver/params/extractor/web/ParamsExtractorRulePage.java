/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerSecuredPage;

public class ParamsExtractorRulePage extends GeoServerSecuredPage {

    public ParamsExtractorRulePage(Optional<RuleModel> optionalRuleModel) {
        CompoundPropertyModel<RuleModel> simpleRuleModel =
                new CompoundPropertyModel<>(optionalRuleModel.orElse(new RuleModel()));
        CompoundPropertyModel<RuleModel> complexRuleModel =
                new CompoundPropertyModel<>(optionalRuleModel.orElse(new RuleModel()));
        CompoundPropertyModel<RuleModel> echoParameterModel =
                new CompoundPropertyModel<>(optionalRuleModel.orElse(new RuleModel(true)));
        Form<RuleModel> form = new Form<>("form");
        add(form);
        List<WrappedTab> tabs = new ArrayList<>();
        if (!optionalRuleModel.isPresent() || optionalRuleModel.get().isEchoOnly()) {
            tabs.add(
                    new WrappedTab("Echo Parameter", echoParameterModel) {
                        public Panel getPanel(String panelId) {
                            return new EchoParameterPanel(panelId, echoParameterModel);
                        }
                    });
        }
        if (!optionalRuleModel.isPresent() || optionalRuleModel.get().getPosition() != null) {
            tabs.add(
                    new WrappedTab("Basic Rule", simpleRuleModel) {
                        public Panel getPanel(String panelId) {
                            return new SimpleRulePanel(panelId, simpleRuleModel);
                        }
                    });
        }
        if (!optionalRuleModel.isPresent() || optionalRuleModel.get().getMatch() != null) {
            tabs.add(
                    new WrappedTab("Advanced Rule", complexRuleModel) {
                        public Panel getPanel(String panelId) {
                            return new ComplexRulePanel(panelId, complexRuleModel);
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
            add(new NumberTextField<Integer>("position").setMinimum(1).setRequired(true));
            add(new TextField<String>("parameter").setRequired(true));
            add(new TextField<String>("transform").setRequired(true));
            add(new CheckBox("echo"));
        }
    }

    public class ComplexRulePanel extends Panel {

        public ComplexRulePanel(String panelId, IModel<RuleModel> model) {
            super(panelId, model);
            add(new TextField<String>("match").setRequired(true));
            add(new TextField<String>("activation"));
            add(new TextField<String>("parameter").setRequired(true));
            add(new TextField<String>("transform").setRequired(true));
            add(new NumberTextField<Integer>("remove").setMinimum(1));
            add(new TextField<String>("combine"));
            add(new CheckBox("repeat"));
            add(new CheckBox("echo"));
        }
    }

    public class EchoParameterPanel extends Panel {

        public EchoParameterPanel(String panelId, IModel<RuleModel> model) {
            super(panelId, model);
            add(new TextField<String>("parameter").setRequired(true));
        }
    }
}
