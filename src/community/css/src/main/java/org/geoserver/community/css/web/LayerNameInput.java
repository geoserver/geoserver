/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

public class LayerNameInput extends Panel {
    String name = "";

    public LayerNameInput(String id, final CssDemoPage demo) {
        super(id);

        Form form = new Form("layer.name.form");

        IValidator<String> nameValidator =
            new IValidator<String>() {
                @Override
                public void validate(IValidatable<String> text) {
                    String value = text.getValue();
                    if (value.matches("\\s")) {
                        text.error(new ValidationError().setMessage(
                            "Spaces not allowed in style names"
                        ));
                    } else {
                        text.error(new ValidationError().setMessage(
                            "That name is already taken!"
                        ));
                    }
                }
            };

         TextField nameField =
             new TextField(
                 "layer.name.field",
                 new PropertyModel<String>(this, "name")
             );
         nameField.add(nameValidator);

         AjaxButton submitButton =
             new AjaxButton("submit.button", form) {
                 @Override
                 public void onSubmit(AjaxRequestTarget target, Form f) {
                     if (demo.catalog().getStyleByName(name) != null)
                         throw new RuntimeException(
                             "Trying to create style with same name as existing one!"
                         );
                     
                     demo.createCssTemplate(name);

                     PageParameters params = new PageParameters();
                     params.put("layer", demo.getLayer().getPrefixedName());
                     params.put("style", name);
                     setResponsePage(CssDemoPage.class, params);
                 }
             };
         form.add(nameField);
         form.add(submitButton);
         add(form);
    }
}
