/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;

public class ImportPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public ImportPanel(String id) {
        super(id);
        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new FileUploadField("upload").setRequired(true));
    }

    public FileUploadField getFileUpload() {
        return (FileUploadField) get("upload");
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("feedback");
    }
}
