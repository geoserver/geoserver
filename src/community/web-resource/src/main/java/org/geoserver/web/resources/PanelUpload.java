/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Panel for uploading.
 *
 * @author Niels Charlier
 */
public class PanelUpload extends Panel {

    private static final long serialVersionUID = 6463557973347000931L;

    public PanelUpload(String id, String directory) {
        super(id);

        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new TextField<String>("directory", new Model<String>(directory)));
        add(new FileUploadField("file"));
    }

    public String getDirectory() {
        return get("directory").getDefaultModelObjectAsString();
    }

    public FileUpload getFileUpload() {
        return ((FileUploadField) get("file")).getFileUpload();
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("feedback");
    }
}
