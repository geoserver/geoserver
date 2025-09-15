/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.Serial;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Panel for renaming.
 *
 * @author Niels Charlier
 */
public class PanelRename extends Panel {

    @Serial
    private static final long serialVersionUID = 6463557973347000931L;

    public PanelRename(String id, String name) {
        super(id);

        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new TextField<>("name", new Model<>(name)));
    }

    public String getName() {
        return get("name").getDefaultModelObjectAsString();
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("feedback");
    }
}
