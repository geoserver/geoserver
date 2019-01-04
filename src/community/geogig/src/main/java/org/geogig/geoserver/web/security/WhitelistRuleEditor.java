/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.security;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geogig.geoserver.config.WhitelistRule;

public class WhitelistRuleEditor extends Panel {

    private static final long serialVersionUID = 1508347689900059344L;

    private boolean isNew;

    public WhitelistRuleEditor(
            String id,
            IModel<WhitelistRule> model,
            final ModalWindow window,
            final WhitelistRulePanel table,
            boolean isNew) {
        super(id, model);
        this.isNew = isNew;
        Form<WhitelistRule> form = new Form<>("form", model);
        add(form);
        TextField<String> name = new TextField<>("name", new PropertyModel<>(model, "name"));
        name.setRequired(true);
        TextField<String> pattern =
                new TextField<>("pattern", new PropertyModel<>(model, "pattern"));
        pattern.setRequired(true);

        form.add(name);
        form.add(pattern);
        form.add(new CheckBox("requireSSL", new PropertyModel<>(model, "requireSSL")));

        final FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        form.add(feedbackPanel);

        form.add(
                new AjaxSubmitLink("submit", form) {

                    private static final long serialVersionUID = 1080309070367012502L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        target.add(feedbackPanel);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        boolean isNew = WhitelistRuleEditor.this.isNew;
                        if (isNew) {
                            WhitelistRule rule = (WhitelistRule) form.getModelObject();
                            table.add(rule);
                        }
                        table.save();
                        window.close(target);
                        target.add(table);
                    }
                });

        form.add(
                new AjaxLink<Void>("cancel") {
                    private static final long serialVersionUID = 4762717512666965125L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        window.close(target);
                        target.add(table);
                    }
                });
    }
}
