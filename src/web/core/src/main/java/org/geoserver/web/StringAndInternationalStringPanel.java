/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * A reusable component for values that can be provided both as a TextField and as an i18n editable
 * table.
 */
public class StringAndInternationalStringPanel extends Panel {

    /**
     * @param id the component id.
     * @param model the model of the form object.
     * @param labelKey the key of the label to be used for the string and i18n string field.
     * @param stringProperty the property of the string value.
     * @param internationalProperty the property of the i18n value.
     * @param labelProvider the WebMarkupContainer being the context for labels in properties files.
     */
    public StringAndInternationalStringPanel(
            String id,
            IModel<?> model,
            String labelKey,
            String stringProperty,
            String internationalProperty,
            WebMarkupContainer labelProvider) {
        super(id, model);
        WebMarkupContainer container = new WebMarkupContainer("labelContainer");
        add(container);
        container.add(new Label("stringLabel", new StringResourceModel(labelKey, labelProvider)));
        TextField<String> title =
                new TextField<>("stringField", new PropertyModel<>(model, stringProperty));
        add(title);

        InternationalStringPanel<TextField<String>> internationalStringField =
                new InternationalStringPanel<TextField<String>>(
                        "internationalField",
                        new PropertyModel<>(model, internationalProperty),
                        title,
                        container) {
                    @Override
                    protected TextField<String> getTextComponent(String id, IModel<String> model) {
                        return new TextField<>(id, model);
                    }
                };

        add(internationalStringField);
    }
}
