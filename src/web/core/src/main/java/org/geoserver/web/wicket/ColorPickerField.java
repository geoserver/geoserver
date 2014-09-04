/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A text field linked to some javascript bits that make it a color picker.
 * @author Andrea Aime - OpenGeo
 *
 */
@SuppressWarnings("serial")
public class ColorPickerField extends TextField {
    
    private static final CompressedResourceReference JSCOLOR_JS = new CompressedResourceReference(
            ColorPickerField.class, "js/jscolor/jscolor.js");

    public ColorPickerField(String id) {
        super(id);
        init();
    }

    public ColorPickerField(String id, Class type) {
        super(id, type);
        init();
    }

    public ColorPickerField(String id, IModel model, Class type) {
        super(id, model, type);
        init();
    }

    
    public ColorPickerField(String id, IModel object) {
        super(id, object);
        init();
    }
    
    void init() {
        add(HeaderContributor.forJavaScript(JSCOLOR_JS));
        add(new AttributeAppender("class", new Model("color {required:false}"), ","));
    }
    
}
