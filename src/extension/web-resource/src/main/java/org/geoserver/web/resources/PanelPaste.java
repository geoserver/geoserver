/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Panel for pasting.
 *
 * @author Niels Charlier
 */
public class PanelPaste extends Panel {

    private static final long serialVersionUID = 6463557973347000931L;

    public PanelPaste(String id, String source, String directory, boolean isCopy) {
        super(id);

        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new TextField<String>("source", new Model<String>(source)).setOutputMarkupId(true));
        add(new WebMarkupContainer("labelMove").setVisible(!isCopy));
        add(new WebMarkupContainer("labelCopy").setVisible(isCopy));
        add(new TextField<String>("directory", new Model<String>(directory)));
    }

    public String getDirectory() {
        return get("directory").getDefaultModelObjectAsString();
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("feedback");
    }

    @SuppressWarnings("unchecked")
    public TextField<String> getSourceField() {
        return ((TextField<String>) get("source"));
    }
}
