/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.Serializable;
import java.util.Optional;

/**
 * Bean used to register module installation in applicationContext.xml.
 * <p>
 * Bean completly defined by applicationContext.xml - no dynamic content.
 * 
 * <pre>
 * &lt!-- code example needed --&gt;>
 * </pre>
 * 
 * @author Morgan Thompson - Boundless
 */
public class ModuleStatusImpl implements ModuleStatus, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -5759469520194940051L;

    private String module;

    private String name;

    private String component;

    private String version;

    private String documentation;

    private String message;

    private boolean isEnabled;

    private boolean isAvailable;

    public ModuleStatusImpl() {
    }

    public ModuleStatusImpl(ModuleStatus status) {
        this.module = status.getModule();
        this.name = status.getName();
        this.component = status.getComponent().orElse(null);
        this.version = status.getVersion().orElse(null);
        this.documentation = status.getDocumentation().orElse(null);
        this.message = status.getMessage().orElse(null);
        this.isEnabled = status.isEnabled();
        this.isAvailable = status.isAvailable();
    }

    public ModuleStatusImpl(String module, String name) {
        this.module = module;
        this.name = name;
        this.isAvailable = true;
        this.isEnabled = true;
    }

    /**
     * @return the machine readable name
     */
    public String getModule() {
        return module;
    }

    /**
     * @param module the module name to set
     */
    public void setModule(String module) {
        this.module = module;
    }

    public Optional<String> getComponent() {
        return Optional.ofNullable(component);
    }

    /**
     * @param component the component to set
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the version
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean isAvailable() {
        return this.isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String toString() {
        return "ModuleStatusImpl [module=" + module + ", component=" + component + ", version="
                + version + "]";
    }
}
