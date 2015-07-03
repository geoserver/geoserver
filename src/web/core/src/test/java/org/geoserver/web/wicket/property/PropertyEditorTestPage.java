/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.property;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;

public class PropertyEditorTestPage extends WebPage {

    public PropertyEditorTestPage(Foo foo) {
        Form form = new Form("form", new CompoundPropertyModel(foo));
        form.add(new PropertyEditorFormComponent("props"));
        form.add(new SubmitLink("save"));
        add(form);
    }
}
