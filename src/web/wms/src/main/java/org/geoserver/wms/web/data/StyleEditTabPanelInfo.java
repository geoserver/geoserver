/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.ComponentInfo;

/**
 * Information about panels plugged into additional tabs on style edit page.
 *
 * <p>Style edit tabs have a self declared order which describes where they end up on the style edit
 * page. Lower order panels are weighted toward the left hand side, higher order panels are weighted
 * toward the right hand side.
 */
public class StyleEditTabPanelInfo extends ComponentInfo<StyleEditTabPanel> {

    private static final long serialVersionUID = 4849692244366766812L;

    /** order of the panel with respect to other panels. */
    int order = -1;

    boolean enabledOnNew = true;

    /** Should this tab be enabled when creating a new style */
    public boolean isEnabledOnNew() {
        return enabledOnNew;
    }

    /** Returns the order of the panel. */
    public int getOrder() {
        return order;
    }

    /** Sets the order of the panel. */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * It may be that a tab contribution to the {@link AbstractStylePage} need to work on a
     * different model object that the page's layer and resource models (for example, because it
     * edits and saves related information not directly attached to the style); if such is the case,
     * this method shall return the model to be passed to the {@link StyleEditTabPanel} constructor.
     *
     * <p>This default implementation just returns {@code null} and assumes the {@link
     * StyleEditTabPanel} described by this tab panel info works against the {@link
     * AbstractStylePage} StyleInfo model. Subclasses may override as appropriate.
     *
     * @return {@code null} if no need for a custom model for the tab, the model to use otherwise
     * @see StyleEditTabPanel#save()
     */
    public IModel<?> createOwnModel(IModel<? extends StyleInfo> model, boolean isNew) {
        return null;
    }
}
