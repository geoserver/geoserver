/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;

public class StyleNameInput extends Panel {
    private static final long serialVersionUID = 2132602896817040015L;
    String workspace = null;
    String name = "";

    public StyleNameInput(String id, final CssDemoPage demo) {
        super(id);

        final Form<?> form = new Form<Object>("layer.name.form");

        IValidator<String> nameValidator =
            new IValidator<String>() {
                private static final long serialVersionUID = 1558466250290018192L;

                @Override
                public void validate(IValidatable<String> text) {
                    final String value = text.getValue();
                    if (value.matches("\\s")) {
                        text.error(new ValidationError().setMessage(
                            "Spaces not allowed in style names"
                        ));
                        return;
                    }
                }
            };

         List<String> workspaces = new ArrayList<String>();
         workspaces.add(null);
         for (WorkspaceInfo info : demo.catalog().getWorkspaces()) {
             workspaces.add(info.getName());
         }
         IChoiceRenderer<String> workspaceRenderer = 
           new ChoiceRenderer<String>() {
            private static final long serialVersionUID = 2319702138663206028L;

            @Override
               public Object getDisplayValue(String value) {
                   return value == null ? "No workspace" : value;
               }
           };
         final WebMarkupContainer warningContainer = new WebMarkupContainer("warningContainer");
         warningContainer.setOutputMarkupId(true);
         form.add(warningContainer);
         final Label warning = new Label("workspaceWarning");
         warning.setOutputMarkupId(true);
         warning.setVisible(false);
         warningContainer.add(warning);
         final DropDownChoice<String> workspaceChooser = new DropDownChoice<String>(
                 "layer.workspace.field",
                 new PropertyModel<String>(this, "workspace"),
                 workspaces,
                 workspaceRenderer);
         workspaceChooser.add(new AjaxFormComponentUpdatingBehavior("change") {
            
            private static final long serialVersionUID = 1645220914767539957L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                LayerInfo layer = demo.getLayer();
                WorkspaceInfo layerWorkspace = layer.getResource().getStore().getWorkspace();
                String selected = workspaceChooser.getModelObject();
                if(selected == null || selected.equals(layerWorkspace.getName())) {
                    warning.setVisible(false);
                } else {
                    warning.setDefaultModel(new Model<String>("Warning, the layer " 
                            + layer.getName() + " is in workspace " + layerWorkspace.getName() 
                            + ",  cannot be used with a style in workspace " + selected));
                    warning.setVisible(true);
                }
                target.add(warningContainer);
            }
        });
         form.add(workspaceChooser);

         TextField<String> nameField =
             new TextField<String>(
                 "layer.name.field",
                 new PropertyModel<String>(this, "name")
             );
         nameField.add(nameValidator);

         AjaxButton submitButton =
             new AjaxButton("submit.button", form) {
                private static final long serialVersionUID = -2701440643314381309L;

                @Override
                 public void onSubmit(AjaxRequestTarget target, Form<?> f) {
                     if (demo.catalog().getStyleByName(workspace, name) != null) {
                         throw new RuntimeException(
                             "Trying to create style with same name as existing one!"
                         );
                     }
                     
                     demo.createCssTemplate(workspace, name);

                     PageParameters params = new PageParameters();
                     params.add("layer", demo.getLayer().prefixedName());
                     if (workspace == null) {
                         params.add("style", name);
                     } else {
                         params.add("style", workspace + ":" + name);
                     }
                     setResponsePage(CssDemoPage.class, params);
                 }
             };
         form.add(nameField);
         form.add(submitButton);
         add(form);
    }
}
