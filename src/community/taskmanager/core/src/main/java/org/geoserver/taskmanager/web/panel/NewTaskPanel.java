/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.util.ArrayList;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.TaskManagerBeans;

public class NewTaskPanel extends Panel {

    private static final long serialVersionUID = -1678565286034119572L;

    public NewTaskPanel(String id, Configuration config) {
        super(id);
        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new TextField<String>("name", new Model<String>()).setRequired(true));
        add(
                new DropDownChoice<String>(
                                "type",
                                new Model<String>(),
                                new Model<ArrayList<String>>(
                                        new ArrayList<String>(
                                                TaskManagerBeans.get().getTaskTypes().names())))
                        .setRequired(true)
                        .setOutputMarkupId(true));
        add(
                new DropDownChoice<String>(
                                "copy",
                                new Model<String>(),
                                new Model<ArrayList<String>>(
                                        new ArrayList<String>(config.getTasks().keySet())))
                        .setOutputMarkupId(true));

        getCopyField()
                .add(
                        new OnChangeAjaxBehavior() {
                            private static final long serialVersionUID = -5575115165929413404L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                if (getCopyField().getConvertedInput() != null) {
                                    getTypeField()
                                            .getModel()
                                            .setObject(
                                                    config.getTasks()
                                                            .get(getCopyField().getConvertedInput())
                                                            .getType());
                                    target.add(getTypeField());
                                }
                            }
                        });

        getTypeField()
                .add(
                        new OnChangeAjaxBehavior() {
                            private static final long serialVersionUID = -1427899086435643578L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                getCopyField().getModel().setObject(null);
                                target.add(getCopyField());
                            }
                        });
    }

    @SuppressWarnings("unchecked")
    public DropDownChoice<String> getTypeField() {
        return ((DropDownChoice<String>) get("type"));
    }

    @SuppressWarnings("unchecked")
    public TextField<String> getNameField() {
        return ((TextField<String>) get("name"));
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("feedback");
    }

    @SuppressWarnings("unchecked")
    public DropDownChoice<String> getCopyField() {
        return ((DropDownChoice<String>) get("copy"));
    }
}
