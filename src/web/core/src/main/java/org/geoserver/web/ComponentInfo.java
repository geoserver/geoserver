/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import org.apache.wicket.Component;

/**
 * Information about a component being plugged into a user interface.
 *
 * <p>Subclasses of this class are used to implement user interface "extension points". For an example see
 * {@link MenuPageInfo}.
 *
 * @author Andrea Aime, The Open Planning Project
 * @author Justin Deoliveira, The Open Planning Project
 * @param <C>
 */
@SuppressWarnings("serial")
public abstract class ComponentInfo<C extends Component> implements Serializable {

    /** the id of the component */
    String id;
    /** the title of the component */
    String title;
    /** The description of the component */
    String description;
    /** the class of the component */
    Class<C> componentClass;
    /** Controls access to the component */
    ComponentAuthorizer authorizer = ComponentAuthorizer.ALLOW;

    /**
     * Comma-separated list of page parameter names to carry forward in navigation links (e.g. {@code "workspace"},
     * {@code "workspace,layer"}, {@code "workspace,group"}). The special value {@code "all"} includes every available
     * context parameter ({@code workspace}, {@code layer}, {@code name}, {@code group}).
     *
     * <p>Defaults to {@code null} (no context parameters forwarded). Must be explicitly set by extension-point beans
     * that want context-aware navigation.
     */
    String contextParams = null;

    public String getContextParams() {
        return contextParams;
    }

    public void setContextParams(String contextParams) {
        this.contextParams = contextParams;
    }

    /**
     * Returns {@code true} if the given page parameter name should be forwarded in navigation links, as determined by
     * the {@link #contextParams} setting.
     */
    public boolean includesContextParam(String paramName) {
        if (contextParams == null || contextParams.isBlank()) return false;
        String trimmed = contextParams.trim();
        if ("all".equalsIgnoreCase(trimmed)) return true;
        for (String p : trimmed.split(",")) {
            if (paramName.equalsIgnoreCase(p.trim())) return true;
        }
        return false;
    }

    /** The id of the component. */
    public String getId() {
        return id;
    }
    /** Sets the id of the component. */
    public void setId(String id) {
        this.id = id;
    }
    /**
     * The i18n key for the title of the component.
     *
     * <p>The exact way this title is used depends one the component. For instance if the component is a page, the title
     * could be the used for a link to the page. If the component is a panel in a tabbed panel, the title might be the
     * label on the tab.
     */
    public String getTitleKey() {
        return title;
    }
    /** The i18n key for the title of the component. */
    public void setTitleKey(String title) {
        this.title = title;
    }

    /**
     * The i18n key for the description of the component.
     *
     * <p>This description is often used as a tooltip, or some contextual help.
     */
    public String getDescriptionKey() {
        return description;
    }

    /** Sets the description of the component. */
    public void setDescriptionKey(String description) {
        this.description = description;
    }

    /** The implementation class of the component. */
    public Class<C> getComponentClass() {
        return componentClass;
    }

    /** Sets the implementation class of the component. */
    public void setComponentClass(Class<C> componentClass) {
        this.componentClass = componentClass;
    }

    /** The authorizer that controls access to the component. */
    public ComponentAuthorizer getAuthorizer() {
        return authorizer;
    }

    /** Sets the authorizer that controls access to the component. */
    public void setAuthorizer(ComponentAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append("id='").append(id).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", componentClass=").append(componentClass);
        sb.append(", authorizer=").append(authorizer);
        sb.append('}');
        return sb.toString();
    }
}
