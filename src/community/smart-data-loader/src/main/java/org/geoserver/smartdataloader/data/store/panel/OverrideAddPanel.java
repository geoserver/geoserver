package org.geoserver.smartdataloader.data.store.panel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class OverrideAddPanel extends Panel {

    private final SmartOverridesModel smartOverridesModel;
    private IModel<String> keyModel = Model.of("");
    private IModel<String> expressionModel = Model.of("");
    ;
    private SmartOverridesRefreshingView smartOverridesRefreshingView;
    private final TextField<String> input1;
    private final TextField<String> input2;

    public OverrideAddPanel(
            String id, SmartOverridesModel overridesModel, SmartOverridesRefreshingView smartOverridesRefreshingView) {
        super(id);
        this.smartOverridesModel = overridesModel;
        this.smartOverridesRefreshingView = smartOverridesRefreshingView;
        Form<Void> form = new Form<>("addOverrideForm");
        input1 = new TextField<>("addOverrideKey", keyModel);
        input2 = new TextField<>("addOverrideExpression", expressionModel);
        FeedbackPanel feedback = new FeedbackPanel("addOverrideFeedback");
        feedback.setOutputMarkupId(true);

        AjaxButton submitButton = new AjaxButton("addOverrideSubmitButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);
                addOverride();
                String result = "Data Added: " + keyModel.getObject() + " | " + expressionModel.getObject();
                info(result);

                keyModel.setObject("");
                expressionModel.setObject("");

                target.add(feedback);
                target.add(form);
                target.add(smartOverridesRefreshingView.getParent());
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(feedback);
            }
        };
        // Enable AJAX updates
        form.setOutputMarkupId(true);
        input1.setOutputMarkupId(true);
        input2.setOutputMarkupId(true);
        submitButton.setOutputMarkupId(true);
        // Add components to form
        form.add(input1, input2, submitButton);
        add(form, feedback);
    }

    private void addOverride() {
        String key = StringUtils.trim(input1.getValue());
        String expression = StringUtils.trim(input2.getValue());
        if (StringUtils.isBlank(key) || StringUtils.isBlank(expression)) {
            throw new IllegalArgumentException("Key and expression must be provided");
        }
        smartOverridesModel.add(new SmartOverrideEntry(key, expression));
    }
}
