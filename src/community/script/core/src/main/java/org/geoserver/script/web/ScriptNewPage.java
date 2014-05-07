/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.ScriptType;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.XMLNameValidator;
import org.geotools.util.logging.Logging;

import com.google.common.collect.Lists;

public class ScriptNewPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.script.web");

    Form form;

    public ScriptNewPage() {
        super();

        Script script = new Script();

        form = new Form("form", new CompoundPropertyModel(script)) {
            @Override
            protected void onSubmit() {
                save();
                doReturn(ScriptPage.class);
            }
        };
        add(form);

        // Name
        TextField name = new TextField("name", new PropertyModel(script, "name"));
        name.setRequired(true);
        name.add(new XMLNameValidator());
        form.add(name);

        // Type
        DropDownChoice<String> typeDropDownChoice = new DropDownChoice<String>("type",
                new PropertyModel(script, "type"), new LoadableDetachableModel<List<String>>() {
                    @Override
                    protected List<String> load() {
                        List<String> values = Lists.newArrayList();
                        for (ScriptType type : ScriptType.values()) {
                            values.add(type.getLabel());
                        }
                        return values;
                    }
                });
        typeDropDownChoice.setRequired(true);
        form.add(typeDropDownChoice);

        // Extension
        DropDownChoice<String> extensionDropDownChoice = new DropDownChoice<String>("extension",
                new PropertyModel(script, "extension"),
                new LoadableDetachableModel<List<String>>() {
                    @Override
                    protected List<String> load() {
                        List<String> extensions = Lists.newArrayList();
                        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptMgr");
                        for (ScriptPlugin plugin : scriptManager.getPlugins()) {
                            extensions.add(plugin.getExtension());
                        }
                        return extensions;
                    }
                });
        extensionDropDownChoice.setRequired(true);
        form.add(extensionDropDownChoice);

        // Content
        final TextArea content = new TextArea("contents", new PropertyModel(script, "contents"));
        content.setRequired(true);
        form.add(content);

        SubmitLink submitLink = new SubmitLink("submit", form);
        form.add(submitLink);
        form.setDefaultButton(submitLink);
        
        AjaxLink cancelLink = new AjaxLink("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn(ScriptPage.class);
            }
        };
        form.add(cancelLink);
    }
    
    private void save() {
        Script s = (Script) form.getModelObject();
        try {
            FileUtils.write(s.getFile(), s.getContents());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
