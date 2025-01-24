/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

/**
 * A text field linked to some javascript bits that make it a color picker.
 *
 * @author Andrea Aime - OpenGeo
 */
public class ColorPickerField extends TextField<String> {

    private static final long serialVersionUID = -5126346882014020980L;

    public ColorPickerField(String id) {
        this(id, null);
    }

    public ColorPickerField(String id, IModel<String> model) {
        super(id, model, String.class);
        add(AttributeModifier.replace("type", "color"));
    }
}
