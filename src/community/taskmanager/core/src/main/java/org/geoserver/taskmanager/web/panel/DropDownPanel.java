/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.wicket.ParamResourceModel;

public class DropDownPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public DropDownPanel(
            String id,
            IModel<String> model,
            IModel<? extends List<? extends String>> choiceModel,
            boolean nullValid) {
        this(id, model, choiceModel, null, nullValid);
    }

    public DropDownPanel(
            String id,
            Model<String> model,
            Model<ArrayList<String>> choiceModel,
            ParamResourceModel paramResourceModel) {
        this(id, model, choiceModel, paramResourceModel, false);
    }

    public DropDownPanel(
            String id,
            IModel<String> model,
            IModel<? extends List<? extends String>> choiceModel,
            IModel<String> labelModel,
            boolean nullValid) {
        super(id, model);

        boolean custom = choiceModel.getObject().contains("");
        boolean useDropDown = !custom || choiceModel.getObject().contains(model.getObject());
        add(new Label("message", labelModel));
        add(
                new DropDownChoice<String>(
                                "dropdown",
                                useDropDown ? model : new Model<String>(""),
                                choiceModel)
                        .setNullValid(nullValid && !custom));
        add(new TextField<String>("custom", model).setVisible(!useDropDown));

        if (custom) {
            setOutputMarkupId(true);
            getDropDownChoice()
                    .add(
                            new OnChangeAjaxBehavior() {
                                private static final long serialVersionUID = 7823984472638368286L;

                                @Override
                                protected void onUpdate(AjaxRequestTarget target) {
                                    boolean useDropDown =
                                            !getDropDownChoice().getModelObject().equals("");
                                    if (useDropDown) {
                                        model.setObject(getDropDownChoice().getModelObject());
                                        getDropDownChoice().setModel(model);
                                    } else {
                                        getDropDownChoice().setModel(new Model<String>(""));
                                    }
                                    getTextField().setVisible(!useDropDown);
                                    target.add(DropDownPanel.this);
                                }
                            });
        }
    }

    @SuppressWarnings("unchecked")
    public DropDownChoice<String> getDropDownChoice() {
        return (DropDownChoice<String>) get("dropdown");
    }

    @SuppressWarnings("unchecked")
    public TextField<String> getTextField() {
        return (TextField<String>) get("custom");
    }
}
