/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * A text field linked to some javascript bits that make it a color picker.
 *
 * @author Andrea Aime - OpenGeo
 */
public class ColorPickerField extends TextField<String> {

    private static final long serialVersionUID = -5126346882014020980L;
    private static final PackageResourceReference JSCOLOR_JS =
            new PackageResourceReference(ColorPickerField.class, "js/jscolor/jscolor.js");

    public ColorPickerField(String id) {
        super(id, String.class);
        init();
    }

    public ColorPickerField(String id, IModel<String> model) {
        super(id, model, String.class);
        init();
    }

    void init() {
        add(
                new Behavior() {
                    private static final long serialVersionUID = 4269437302317170665L;

                    @Override
                    public void renderHead(Component component, IHeaderResponse response) {
                        super.renderHead(component, response);
                        response.render(JavaScriptHeaderItem.forReference(JSCOLOR_JS));
                    }
                });
        add(new AttributeAppender("class", new Model<String>("color {required:false}"), ","));
    }
}
