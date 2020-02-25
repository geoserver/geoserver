/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.ScriptType;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geotools.util.logging.Logging;

public class ScriptNewPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.script.web");

    Form form;

    public ScriptNewPage() {
        super();

        Script script = new Script();

        form =
                new Form("form", new CompoundPropertyModel(script)) {
                    @Override
                    protected void onSubmit() {
                        save();
                        doReturn(ScriptPage.class);
                    }
                };
        add(form);

        // Get List of script extensions from installed plugins
        final List<String> extensions = getExtensions();

        // Content
        String mode = extensions.size() > 0 ? getModeFromExtension(extensions.get(0)) : "py";
        final CodeMirrorEditor content =
                new CodeMirrorEditor("contents", mode, new PropertyModel(script, "contents"));
        content.setRequired(true);
        form.add(content);

        // Name
        TextField name = new TextField("name", new PropertyModel(script, "name"));
        name.setRequired(true);
        form.add(name);

        // Type
        DropDownChoice<String> typeDropDownChoice =
                new DropDownChoice<String>(
                        "type",
                        new PropertyModel(script, "type"),
                        new LoadableDetachableModel<List<String>>() {
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
        final DropDownChoice<String> extensionDropDownChoice =
                new DropDownChoice<String>(
                        "extension",
                        new PropertyModel(script, "extension"),
                        new LoadableDetachableModel<List<String>>() {
                            @Override
                            protected List<String> load() {
                                return extensions;
                            }
                        });
        extensionDropDownChoice.setRequired(true);
        extensionDropDownChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        int i = Integer.parseInt(extensionDropDownChoice.getValue());
                        String ext = extensions.get(i);
                        String mode = getModeFromExtension(ext);
                        content.setMode(mode);
                    }
                });
        form.add(extensionDropDownChoice);

        SubmitLink submitLink = new SubmitLink("submit", form);
        form.add(submitLink);
        form.setDefaultButton(submitLink);

        AjaxLink cancelLink =
                new AjaxLink("cancel") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn(ScriptPage.class);
                    }
                };
        form.add(cancelLink);
    }

    private List<String> getExtensions() {
        List<String> extensions = Lists.newArrayList();
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptManager");
        for (ScriptPlugin plugin : scriptManager.getPlugins()) {
            extensions.add(plugin.getExtension());
        }
        return extensions;
    }

    private String getModeFromExtension(String ext) {
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptManager");
        String mode = scriptManager.lookupEditorModeByExtension(ext);
        return mode;
    }

    private void save() {
        Script s = (Script) form.getModelObject();
        try (OutputStream out = s.getResource().out()) {
            IOUtils.write(s.getContents(), out);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
