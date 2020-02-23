/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.metadata.data.dto.AttributeConfiguration;

public class AutoCompletePanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public AutoCompletePanel(
            String id,
            IModel<String> model,
            List<String> values,
            boolean forceValues,
            AttributeConfiguration configuration,
            IModel<List<String>> selectedValues) {

        super(id, model);

        AutoCompleteTextField<String> field =
                new AutoCompleteTextField<String>("autoComplete", model) {
                    private static final long serialVersionUID = 7742400754591550452L;

                    @Override
                    protected Iterator<String> getChoices(String input) {
                        Set<String> result = new TreeSet<String>();
                        for (String value : values) {
                            if (value.toLowerCase().startsWith(input.toLowerCase())) {
                                result.add(value);
                            }
                        }
                        if (result.isEmpty()) {
                            result.addAll(values);
                        }
                        if (selectedValues != null) {
                            result.removeIf(i -> selectedValues.getObject().contains(i));
                            if (!Strings.isEmpty(model.getObject())) {
                                result.add(model.getObject());
                            }
                        }
                        return result.iterator();
                    }
                };
        if (selectedValues != null) {
            field.add(
                    new AjaxFormComponentUpdatingBehavior("change") {
                        private static final long serialVersionUID = 1989673955080590525L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            target.add(
                                    AutoCompletePanel.this.findParent(
                                            RepeatableAttributesTablePanel.class));
                        }
                    });
        }

        if (forceValues) {
            field.add(
                    new IValidator<String>() {

                        private static final long serialVersionUID = -7843517457763685578L;

                        @Override
                        public void validate(IValidatable<String> validatable) {
                            if (!values.contains(validatable.getValue())) {
                                validatable.error(
                                        new ValidationError(
                                                new StringResourceModel(
                                                                "invalid", AutoCompletePanel.this)
                                                        .setParameters(
                                                                resolveLabelValue(configuration))
                                                        .getString()));
                            }
                        }
                    });
        }

        add(field);
    }

    /** Try to find the label from the resource bundle */
    private String resolveLabelValue(AttributeConfiguration attribute) {
        return getString(
                AttributeConfiguration.PREFIX + attribute.getKey(), null, attribute.getLabel());
    }
}
