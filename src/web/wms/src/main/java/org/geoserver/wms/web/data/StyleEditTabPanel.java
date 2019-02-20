/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Extension point for panels which appear in separate tabs on the style edit page.
 *
 * <p>Subclasses <b>must</b> override the {@link #StyleEditTabPanel(String, IModel)} constructor and
 * <b>not</b> change its signature.
 *
 * <p>Instances of this class are described in a spring context with a {@link StyleEditTabPanelInfo}
 * bean.
 */
public class StyleEditTabPanel extends Panel {

    private static final long serialVersionUID = 8044055895040826418L;

    protected AbstractStylePage stylePage;

    /**
     * @param id The id given to the panel.
     * @param parent parent the page that contains this editor.
     */
    public StyleEditTabPanel(String id, AbstractStylePage parent) {
        super(id);

        this.stylePage = parent;
    }

    protected AbstractStylePage getStylePage() {
        return stylePage;
    }

    /**
     * Called by {@link AbstractStylePage} when the style form is submitted.
     *
     * <p>
     */
    protected void onStyleFormSubmit() {
        // do nothing by default
    }

    protected void configurationChanged() {
        // do nothing by default
    }

    public StyleEditTabPanel setInputEnabled(final boolean enabled) {
        visitChildren(
                (component, visit) -> {
                    component.setEnabled(enabled);
                });
        return this;
    }
}
