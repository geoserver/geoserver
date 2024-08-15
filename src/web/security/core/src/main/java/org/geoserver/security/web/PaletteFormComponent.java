/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class PaletteFormComponent<T extends Serializable> extends FormComponentPanel<T> {

    /** the palette */
    protected Palette<T> palette;

    /** list of behaviors to add, staged before the palette recorder component is created */
    List<Behavior> toAdd = new ArrayList<>();

    public PaletteFormComponent(
            String id,
            IModel<List<T>> model,
            IModel<List<T>> choicesModel,
            ChoiceRenderer<T> renderer) {
        super(id, new Model<>());

        add(
                palette =
                        new Palette<T>("palette", model, choicesModel, renderer, 10, false) {
                            @Override
                            protected Recorder<T> newRecorderComponent() {
                                Recorder<T> rec = super.newRecorderComponent();

                                // add any behaviors that need to be added
                                rec.add(toAdd.toArray(new Behavior[toAdd.size()]));
                                toAdd.clear();
                                return rec;
                            }

                            /** Override otherwise the header is not i18n'ized */
                            @Override
                            public Component newSelectedHeader(final String componentId) {

                                return new Label(
                                        componentId,
                                        new ResourceModel(getSelectedHeaderPropertyKey()));
                            }

                            /** Override otherwise the header is not i18n'ized */
                            @Override
                            public Component newAvailableHeader(final String componentId) {
                                return new Label(
                                        componentId,
                                        new ResourceModel(getAvailableHeaderPropertyKey()));
                            }
                        });
        palette.add(new DefaultTheme());
        palette.setOutputMarkupId(true);
    }

    /**
     * @return the default key, subclasses may override, if "Selected" is not illustrative enough
     */
    protected String getSelectedHeaderPropertyKey() {
        return "PaletteFormComponent.selectedHeader";
    }

    /**
     * @return the default key, subclasses may override, if "Available" is not illustrative enough
     */
    protected String getAvailableHeaderPropertyKey() {
        return "PaletteFormComponent.availableHeader";
    }

    @Override
    public Component add(Behavior... behaviors) {
        if (palette.getRecorderComponent() == null) {
            // stage for them for later
            toAdd.addAll(Arrays.asList(behaviors));
        } else {
            // add them now
            palette.getRecorderComponent().add(behaviors);
        }
        return this;
    }

    public Palette<T> getPalette() {
        return palette;
    }

    public IModel<Collection<T>> getPaletteModel() {
        return palette.getModel();
    }

    @Override
    public void updateModel() {
        super.updateModel();
        palette.getRecorderComponent().updateModel();
    }
}
