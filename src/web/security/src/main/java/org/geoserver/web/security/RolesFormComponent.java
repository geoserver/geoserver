/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.security.impl.GeoserverUserDao;
import org.geoserver.web.wicket.SimpleChoiceRenderer;

/**
 * A form component that can be used to edit user/rule role lists
 */
@SuppressWarnings("serial")
public class RolesFormComponent extends FormComponentPanel {
    List<String> roles;

    List<String> choices;

    TextField newRoleField;

    Palette rolePalette;

    public RolesFormComponent(String id, IModel choiceModel, Form form, boolean hasAny) {
        super(id, choiceModel);
        roles = GeoserverUserDao.get().getRoles();
        if (hasAny) {
            roles.add(roles.size(), "*");
        }
        choices = new ArrayList<String>((List<String>) choiceModel.getObject());

        rolePalette = rolesPalette(new PropertyModel(this, "choices"));
        rolePalette.setOutputMarkupId(true);
        add(rolePalette);
        add(newRoleField = new TextField("newRole", new Model()));
        newRoleField.setOutputMarkupId(true);
        add(addRoleButton(form));

    }

    /**
     * Builds a palette that forces at least one role to be chosen
     * 
     * @param userModel
     * @return
     */
    Palette rolesPalette(final IModel choiceModel) {
        return new Palette("roles", choiceModel, new Model((Serializable) roles),
                new SimpleChoiceRenderer(), 10, false) {

            // trick to force the palette to have at least one selected elements
            // tried with a nicer validator but it's not used at all, the required thing
            // instead is working (don't know why...)
            protected Recorder newRecorderComponent() {
                Recorder rec = super.newRecorderComponent();
                rec.setRequired(true);
                return rec;
            }
        };
    }

    private AjaxButton addRoleButton(Form form) {
        // have this work without triggering the form validation. This also means
        // we need to grab the raw value out of the role field
        AjaxButton button = new AjaxButton("addRole", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String newRole = newRoleField.getRawInput();

                // update the palette
                if (!"*".equals(newRole)) {
                    // cumbersome trick to force the palette to store the selection in chioces
                    // then we update the chioces and force the component to repaint...
                    rolePalette.getRecorderComponent().validate();
                    rolePalette.getRecorderComponent().updateModel();
                    roles.add(newRole);
                    choices.add(newRole);
                    rolePalette.modelChanged();
                }

                // clear the role field
                newRoleField.clearInput();
                target.addComponent(rolePalette);
                target.addComponent(newRoleField);
            }
        };
        button.setDefaultFormProcessing(false);
        return button;
    }

    @Override
    protected void convertInput() {
        // if we have "*" in the list, only "*" will do
        for (String role : choices) {
            if("*".equals(role)) {
                choices = new ArrayList<String>();
                choices.add("*");
            }
        }
        setConvertedInput(choices);
    }

	public Palette getRolePalette() {
		return rolePalette;
	}
}
