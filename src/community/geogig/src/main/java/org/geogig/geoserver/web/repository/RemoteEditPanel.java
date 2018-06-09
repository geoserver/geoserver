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
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.PatternValidator;

public class RemoteEditPanel extends Panel {

    private static final long serialVersionUID = 1510021788966720096L;

    Form<RemoteInfo> form;

    TextField<String> name;

    TextField<String> url;

    TextField<String> user;

    PasswordTextField password;

    RemoteEditPanel(
            String id,
            IModel<RemoteInfo> model,
            final ModalWindow parentWindow,
            final RemotesListPanel table) {
        super(id, model);

        form = new Form<>("form", model);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        boolean isNew = true;
        for (RemoteInfo config : table.getRemotes()) {
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
                        for (RemoteInfo ri : table.getRemotes()) {
                            if (!ri.equals(model.getObject())) {
                                String newName = ri.getName();
                                if (newName != null
                                        && !newName.equals(previousName)
                                        && newName.equals(name)) {
                                    form.error(
                                            String.format(
                                                    "A remote named %s already exists", name));
                                }
                            }
                        }
                    }
                });

        url = new TextField<>("url", new PropertyModel<>(model, "URL"));
        url.setRequired(true);

        user = new TextField<>("user", new PropertyModel<>(model, "userName"));

        password = new PasswordTextField("password", new PropertyModel<>(model, "password"));
        password.setRequired(false);
        password.setResetPassword(false);

        form.add(name);
        form.add(url);
        form.add(user);
        form.add(password);

        form.add(
                new AjaxSubmitLink("submit", form) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        target.add(feedback);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        RemoteInfo newRemote = (RemoteInfo) form.getModelObject();
                        if (!isInTable) {
                            table.add(newRemote);
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
