/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.util.Arrays;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.geoserver.featurestemplating.configuration.SupportedFormat;

/** A Dropdown with output formats supported by features templating. */
class OutputFormatsDropDown extends DropDownChoice<SupportedFormat> {

    OutputFormatsDropDown(String id, IModel<SupportedFormat> model) {
        super(id);
        this.setChoices(Arrays.asList(SupportedFormat.values()));
        this.setModel(model);
        this.setChoiceRenderer(getFormatChoiceRenderer());
    }

    OutputFormatsDropDown(String id, IModel<SupportedFormat> model, String templateExtension) {
        super(id);
        this.setChoices(SupportedFormat.getByExtension(templateExtension));
        this.setModel(model);
        this.setChoiceRenderer(getFormatChoiceRenderer());
    }

    private EnumChoiceRenderer<SupportedFormat> getFormatChoiceRenderer() {
        return new EnumChoiceRenderer<SupportedFormat>() {
            @Override
            public String getDisplayValue(SupportedFormat object) {
                return object.getFormat();
            }
        };
    }
}
