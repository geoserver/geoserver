/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geotools.util.logging.Logging;

/**
 * A choice renderer assuming the display value of a particular string will be found in the
 * GeoServer i18n files, under the key <code>componentName.rawString</code>.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class EnumChoiceRenderer extends ChoiceRenderer<Enum<?>> {
    private static final long serialVersionUID = -8773437372842472840L;

    static final Logger LOGGER = Logging.getLogger(EnumChoiceRenderer.class);

    Component reference = null;

    public EnumChoiceRenderer(Component reference) {
        this.reference = reference;
    }

    @Override
    public Object getDisplayValue(Enum<?> object) {
        try {
            ParamResourceModel rm = new ParamResourceModel(object.name(), reference);
            return rm.getString();
        } catch (Exception e) {
            LOGGER.log(
                    Level.FINE,
                    "Failed to locate resource string " + object + " with context: " + reference,
                    e);
            return object;
        }
    }

    @Override
    public String getIdValue(Enum<?> object, int index) {
        return object.name();
    }
}
