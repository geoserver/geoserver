/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package main.java.org.geoserver.template.editor.web;

import java.util.Optional;
import org.geoserver.platform.ModuleStatus;

public class TemplateEditorModuleStatus implements ModuleStatus {

    @Override
    public String getModule() {
        String id = "gs-template-editor";
        return id;
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.of("template-editor (community extension)");
    }

    @Override
    public String getName() {
        String name = "Geoserver FreeMarker template editor";
        return name;
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.of("1.0.0");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.of(
                "https://docs.geoserver.org/latest/en/user/community/template-editor/index.html");
    }
}
