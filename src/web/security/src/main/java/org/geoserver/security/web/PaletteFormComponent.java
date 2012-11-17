/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.impl.GeoServerUserGroup;

public class PaletteFormComponent<T> extends FormComponentPanel {

    /**
     * the palette
     */
    protected Palette<T> palette;

    /**
     * list of behaviors to add, staged before the palette recorder component is created
     */
    List<IBehavior> toAdd = new ArrayList();

    public PaletteFormComponent(String id, IModel<List<T>> model, IModel<Collection<T>> choicesModel, 
            ChoiceRenderer<T> renderer) {
        super(id, new Model());

        add(palette = new Palette<T>("palette", model, choicesModel, renderer, 10, false) {
            @Override
            protected Recorder<T> newRecorderComponent() {
                Recorder<T> rec = super.newRecorderComponent();

                //add any behaviors that need to be added
                rec.add(toAdd.toArray(new IBehavior[toAdd.size()]));
                toAdd.clear();
                return rec;
            }
        });
        palette.setOutputMarkupId(true);
    }

    @Override
    public Component add(IBehavior... behaviors) {
        if (palette.getRecorderComponent() == null) {
            //stage for them for later
            toAdd.addAll(Arrays.asList(behaviors));
        }
        else {
            //add them now
            palette.getRecorderComponent().add(behaviors);
        }
        return this;
    }

    public Palette<T> getPalette() {
        return palette;
    }

    public IModel<List<T>> getPaletteModel() {
        return (IModel<List<T>>) palette.getDefaultModel();
    }

    @Override
    public void updateModel() {
        super.updateModel();
        palette.getRecorderComponent().updateModel();
    }
}
