/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.Serial;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.platform.resource.Resource;

/**
 * Panel for editing.
 *
 * @author Niels Charlier
 */
public class PanelEdit extends Panel {

    @Serial
    private static final long serialVersionUID = -31594049414032328L;

    public PanelEdit(String id, Resource resource, boolean isNew, String contents) {
        super(id);
        if (!resource.isInternal()) {
            // double check resource browser cannot be used to edit
            // files outside of resource store
            throw new IllegalStateException("Path location not supported by Resource Browser");
        }
        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new TextField<>("resource", new Model<>(resource.toString())) {
            @Serial
            private static final long serialVersionUID = 1019950718780805835L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                if (!isNew) {
                    tag.put("readonly", "readonly");
                }
            }
        });
        add(new WebMarkupContainer("createDirectory").setVisible(isNew));
        add(new TextArea<>("contents", new Model<>(contents)));
    }

    public String getContents() {
        return (String) get("contents").getDefaultModelObject();
    }

    public String getResource() {
        return (String) get("resource").getDefaultModelObject();
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("feedback");
    }
}
