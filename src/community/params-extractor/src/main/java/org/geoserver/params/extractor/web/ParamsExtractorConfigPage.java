/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.PackageResourceReference;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.params.extractor.UrlTransform;
import org.geoserver.params.extractor.Utils;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

import java.net.URLDecoder;
import java.util.*;

public class ParamsExtractorConfigPage extends GeoServerSecuredPage {

    GeoServerTablePanel<RuleModel> rulesPanel;

    public ParamsExtractorConfigPage() {
        setHeaderPanel(headerPanel());
        add(rulesPanel = new GeoServerTablePanel<RuleModel>("rulesPanel", new RulesModel(), true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                                                        GeoServerDataProvider.Property<RuleModel> property) {
                if (property == RulesModel.BUTTONS) {
                    return new ButtonPanel(id, (RuleModel) itemModel.getObject());
                }
                return null;
            }
        });
        rulesPanel.setOutputMarkupId(true);
        Form<RuleTestModel> form = new Form<>("form", new CompoundPropertyModel<RuleTestModel>(new RuleTestModel()));
        add(form);
        TextArea<String> input = new TextArea<>("input");
        form.add(input);
        input.setDefaultModelObject("/geoserver/tiger/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&CQL_FILTER=CFCC%3D%27H11%27");
        final TextArea<String> output = new TextArea<>("output");
        output.setEnabled(false);
        form.add(output);
        form.add(new SubmitLink("test") {
            @Override
            public void onSubmit() {
                if (rulesPanel.getSelection().isEmpty()) {
                    output.setModelObject("NO RULES SELECTED !");
                    return;
                }
                String outputText;
                try {
                    RuleTestModel ruleTestModel = (RuleTestModel) getForm().getModelObject();
                    String[] urlParts = ruleTestModel.getInput().split("\\?");
                    String requestUri = urlParts[0];
                    String queryRequest = urlParts.length > 1 ? urlParts[1] : null;
                    UrlTransform urlTransform = new UrlTransform(requestUri, Utils.parseParameters(queryRequest));
                    for (RuleModel ruleModel : rulesPanel.getSelection()) {
                        ruleModel.toRule().apply(urlTransform);
                    }
                    if (urlTransform.haveChanged()) {
                        outputText = urlTransform.toString();
                    } else {
                        outputText = "NO RULES APPLIED !";
                    }
                } catch (Exception exception) {
                    outputText = "Exception: " + exception.getMessage();
                }
                output.setModelObject(outputText);
            }
        });
    }

    private Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        header.add(new AjaxLink<Object>("addNew") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new ParamsExtractorRulePage(null));
            }
        });
        header.add(new AjaxLink<Object>("removeSelected") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                List<String> rulesIds = new ArrayList<>();
                for (RuleModel ruleModel : rulesPanel.getSelection()) {
                    rulesIds.add(ruleModel.getId());
                }
                RulesModel.deleteRules(rulesIds.toArray(new String[rulesIds.size()]));
                target.addComponent(rulesPanel);
            }
        });
        return header;
    }

    private class ButtonPanel extends Panel {

        public ButtonPanel(String id, final RuleModel ruleModel) {
            super(id);
            this.setOutputMarkupId(true);
            ImageAjaxLink editLink = new ImageAjaxLink("edit", new PackageResourceReference(getClass(), "img/edit.png")) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    setResponsePage(new ParamsExtractorRulePage(ruleModel));
                }
            };
            editLink.getImage().add(new AttributeModifier("alt", new ParamResourceModel("ParamsExtractorConfigPage.edit", editLink)));
            editLink.setOutputMarkupId(true);
            add(editLink);
        }
    }
}
