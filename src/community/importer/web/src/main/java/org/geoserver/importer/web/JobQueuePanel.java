package org.geoserver.importer.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;

public class JobQueuePanel extends Panel {

    public JobQueuePanel(String id) {
        super(id);

        final JobQueueTable table = new JobQueueTable("table");
        add(table);

        final AjaxLink refreshLink = new AjaxLink("refresh") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.addComponent(table);
            }
        };
        add(refreshLink);
    }

}
