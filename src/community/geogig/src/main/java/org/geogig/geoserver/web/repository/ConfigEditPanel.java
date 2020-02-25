/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.PatternValidator;

public class ConfigEditPanel extends Panel {

    private static final long serialVersionUID = -1015911960516043997L;

    Form<ConfigEntry> form;

    TextField<String> name;

    TextField<String> value;

    ConfigEditPanel(
            String id,
            IModel<ConfigEntry> model,
            final ModalWindow parentWindow,
            final ConfigListPanel table) {
        super(id, model);

        form = new Form<>("form", model);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        boolean isNew = true;
        for (ConfigEntry config : table.getConfigs()) {
            if (config.equals(model.getObject())) {
                isNew = false;
                break;
            }
        }
        final boolean isInTable = !isNew;
        name = new TextField<>("name", new PropertyModel<>(model, "name"));
        name.setRequired(true);
        name.add(new PatternValidator("[^\\s]+"));
        name.add(
                new IValidator<String>() {
                    private static final long serialVersionUID = 2927770353770055054L;

                    final String previousName = isInTable ? form.getModelObject().getName() : null;

                    @Override
                    public void validate(IValidatable<String> validatable) {
                        String name = validatable.getValue();
                        if (ConfigEntry.isRestricted(name)) {
                            form.error(
                                    String.format(
                                            "Modifying %s through this interface can have unintended consequences and is not allowed.",
                                            name));
                        } else {
                            for (ConfigEntry config : table.getConfigs()) {
                                if (!config.equals(model.getObject())) {
                                    String newName = config.getName();
                                    if (newName != null
                                            && !newName.equals(previousName)
                                            && newName.equals(name)) {
                                        form.error(
                                                String.format(
                                                        "A config entry called %s already exists",
                                                        name));
                                    }
                                }
                            }
                        }
                    }
                });

        value = new TextField<>("value", new PropertyModel<>(model, "value"));
        value.setRequired(true);

        form.add(name);
        form.add(value);

        form.add(
                new AjaxSubmitLink("submit", form) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        target.add(feedback);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ConfigEntry newConfig = (ConfigEntry) form.getModelObject();
                        if (!isInTable) {
                            table.add(newConfig);
                        }
                        parentWindow.close(target);
                        target.add(table);
                    }
                });

        form.add(
                new AjaxLink<Void>("cancel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        parentWindow.close(target);
                        target.add(table);
                    }
                });
    }
}
