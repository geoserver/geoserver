/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.util.ArrayList;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.taskmanager.util.TaskManagerBeans;

public class NewTaskPanel extends Panel {

    private static final long serialVersionUID = -1678565286034119572L;

    public NewTaskPanel(String id) {
        super(id);
        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new TextField<String>("name", new Model<String>()).setRequired(true));
        add(new DropDownChoice<String>("type", new Model<String>(), 
                new Model<ArrayList<String>>(new ArrayList<String>(
                        TaskManagerBeans.get().getTaskTypes().names())))
                .setRequired(true));
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
    
}
