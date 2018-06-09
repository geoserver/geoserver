/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * Helper class to test components that need a form around them to be tested (typically custom
 * panels with form components inside).
 *
 * <p>The panel will be placed into a form named "form", the panel itself will be named "content"
 */
public class FormTestPage extends WebPage {

    private static final long serialVersionUID = 4530843789748801989L;

    public static final String PANEL = "panel";
    public static final String FORM = "form";

    public FormTestPage(ComponentBuilder builder) {
        Form<?> form = new Form<Object>(FORM);
        form.add(builder.buildComponent(PANEL));
        add(form);
    }

    public FormTestPage(ComponentBuilder builder, IModel<Object> formModel) {
        Form<?> form = new Form<Object>(FORM, formModel);
        form.add(builder.buildComponent(PANEL));
        add(form);
    }
}
