/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.proxybase.ext.ProxyBaseExtUrlMangler;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

/** The configuration page for the {@link ProxyBaseExtUrlMangler}. */
public class ProxyBaseExtensionConfigPage extends GeoServerSecuredPage {
    GeoServerTablePanel<ProxyBaseExtensionRule> rulesPanel;

    /** Default constructor. */
    public ProxyBaseExtensionConfigPage() {
        setHeaderPanel(headerPanel());
        add(
                rulesPanel =
                        new GeoServerTablePanel<ProxyBaseExtensionRule>(
                                "rulesPanel", new RulesDataProvider(), true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<ProxyBaseExtensionRule> itemModel,
                                    GeoServerDataProvider.Property<ProxyBaseExtensionRule>
                                            property) {
                                if (property == RulesDataProvider.EDIT_BUTTON) {
                                    return createEditLink(id, itemModel.getObject());
                                }
                                if (property == RulesDataProvider.ACTIVATE_BUTTON) {
                                    // wrapped in panel because checkbox is not working with the
                                    // GeoServerTablePanel component markup
                                    return new ActivateButtonPanel(id, itemModel.getObject());
                                }
                                return null;
                            }
                        });
        rulesPanel.setOutputMarkupId(true);
        rulesPanel.setSortable(false);
        rulesPanel.setPageable(false);
        RuleTestModel ruleTestModel = new RuleTestModel();
        Form<RuleTestModel> form = new Form<>("form", new CompoundPropertyModel<>(ruleTestModel));
        add(form);
        TextArea<String> headersArea =
                new TextArea<>("headers", new PropertyModel<>(ruleTestModel, "headers"));
        form.add(headersArea);
        TextArea<String> input =
                new TextArea<>("input", new PropertyModel<>(ruleTestModel, "input"));
        form.add(input);
        input.setDefaultModelObject("http://localhost:8080/geoserver/wfs");
        TextArea<String> output =
                new TextArea<>("output", new PropertyModel<>(ruleTestModel, "output"));
        output.setEnabled(false);
        form.add(output);
        form.add(
                new SubmitLink("test") {
                    @Override
                    public void onSubmit() {
                        String outputText;
                        try {
                            RuleTestModel ruleTestModel =
                                    (RuleTestModel) getForm().getModelObject();
                            Iterator<ProxyBaseExtensionRule> iterator =
                                    rulesPanel
                                            .getDataProvider()
                                            .iterator(0, rulesPanel.getDataProvider().size());
                            List<ProxyBaseExtensionRule> rules = new ArrayList<>();
                            while (iterator.hasNext()) {
                                rules.add(iterator.next());
                            }
                            ProxyBaseExtUrlMangler mangler = new ProxyBaseExtUrlMangler(rules);

                            outputText =
                                    mangler.transformURL(
                                            ruleTestModel.getInput(), ruleTestModel.getHeaders());

                        } catch (Exception exception) {
                            outputText = "Exception: " + exception.getMessage();
                        }
                        output.setModelObject(outputText);
                    }
                });
    }

    private Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        header.add(
                new AjaxLink<Object>("addNew") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(new ProxyBaseExtensionRulePage(Optional.empty()));
                    }
                });
        header.add(
                new AjaxLink<Object>("removeSelected") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        RulesDataProvider.delete(
                                rulesPanel.getSelection().stream()
                                        .map(ProxyBaseExtensionRule::getId)
                                        .toArray(String[]::new));
                        target.add(rulesPanel);
                    }
                });
        return header;
    }

    Component createEditLink(String id, final ProxyBaseExtensionRule ruleModel) {
        ImageAjaxLink<Object> editLink =
                new ImageAjaxLink<Object>(
                        id, new PackageResourceReference(getClass(), "img/edit.png")) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        setResponsePage(new ProxyBaseExtensionRulePage(Optional.of(ruleModel)));
                    }
                };
        editLink.getImage()
                .add(
                        new AttributeModifier(
                                "alt",
                                new ParamResourceModel(
                                        "ProxyBaseExtensionConfigPage.edit", editLink)));
        editLink.setOutputMarkupId(true);
        return editLink;
    }

    private class ActivateButtonPanel extends Panel {

        public ActivateButtonPanel(String id, final ProxyBaseExtensionRule ruleModel) {
            super(id);
            this.setOutputMarkupId(true);
            CheckBox activateButton =
                    new CheckBox("activated", new PropertyModel<>(ruleModel, "activated")) {

                        @Override
                        public void onSelectionChanged() {
                            super.onSelectionChanged();
                            RulesDataProvider.saveOrUpdate(ruleModel);
                        }

                        @Override
                        protected boolean wantOnSelectionChangedNotifications() {
                            return true;
                        }
                    };
            add(activateButton);
        }
    }
}
