/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Simple editor for enumerations
 *
 * @author Andrea Aime - GeoSolutions
 */
public class EnumPanel extends Panel {

    public EnumPanel(String id, Class<Enum> enumeration, IModel<Enum> model) {
        super(id, model);
        final List<Enum> enums = Arrays.asList(enumeration.getEnumConstants());
        DropDownChoice<Enum> choice = new DropDownChoice<Enum>("enum", model, enums);
        add(choice);
    }
}
