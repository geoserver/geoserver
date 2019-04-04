/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.XMLNameValidator;

@SuppressWarnings("serial")
class NewWorkspacePanel extends Panel {

    String workspace;

    public NewWorkspacePanel(String id) {
        super(id);
        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        TextField wst = new TextField("workspace", new PropertyModel(this, "workspace"));
        wst.setRequired(true);

        wst.add(new WorkspaceDoesNotExistValidator());

        wst.add(new XMLNameValidator());

        this.add(wst);
    }

    static class WorkspaceDoesNotExistValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> iv) {
            String value = iv.getValue();
            if (GeoServerApplication.get().getCatalog().getWorkspaceByName(value) != null) {
                iv.error(
                        new ValidationError("NewWorkspacePanel.duplicateWorkspace")
                                .setVariable("workspace", value));
            }
        }
    }
}
