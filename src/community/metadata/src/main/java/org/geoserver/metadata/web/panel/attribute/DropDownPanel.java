/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.geoserver.metadata.data.dto.AttributeConfiguration;

public class DropDownPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public DropDownPanel(
            String id,
            String attributeKey,
            IModel<String> model,
            List<String> values,
            IModel<List<String>> selectedValues) {

        super(id, model);

        if (selectedValues == null) { // not part of repeatable
            add(createDropDown(attributeKey, model, values));
        } else { // part of repeatable
            add(createDropDown(attributeKey, model, values, selectedValues));
        }
    }

    private DropDownChoice<String> createDropDown(
            String attributeKey, IModel<String> model, List<String> values) {
        DropDownChoice<String> choice =
                new DropDownChoice<String>("dropdown", model, values, createRenderer(attributeKey));
        choice.setNullValid(true);
        return choice;
    }

    private DropDownChoice<String> createDropDown(
            String attributeKey,
            IModel<String> model,
            List<String> values,
            IModel<List<String>> selectedValues) {
        DropDownChoice<String> choice =
                new DropDownChoice<String>(
                        "dropdown",
                        model,
                        new IModel<List<String>>() {
                            private static final long serialVersionUID = -2410089772309709492L;

                            @Override
                            public List<String> getObject() {
                                Set<String> currentList = new TreeSet<>();
                                currentList.addAll(values);
                                currentList.removeIf(i -> selectedValues.getObject().contains(i));
                                if (!Strings.isEmpty(model.getObject())) {
                                    currentList.add(model.getObject());
                                }
                                return new ArrayList<String>(currentList);
                            }

                            @Override
                            public void setObject(List<String> object) {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public void detach() {}
                        },
                        createRenderer(attributeKey));
        choice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 1989673955080590525L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(
                                DropDownPanel.this.findParent(
                                        RepeatableAttributesTablePanel.class));
                    }
                });
        choice.setNullValid(true);
        return choice;
    }

    private IChoiceRenderer<String> createRenderer(String attributeKey) {
        return new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(String object) {
                return getString(
                        AttributeConfiguration.PREFIX + attributeKey + "." + object, null, object);
            }

            @Override
            public String getIdValue(String object, int index) {
                return object;
            }

            @Override
            public String getObject(String id, IModel<? extends List<? extends String>> choices) {
                return Strings.isEmpty(id) ? null : id;
            }
        };
    }
}
