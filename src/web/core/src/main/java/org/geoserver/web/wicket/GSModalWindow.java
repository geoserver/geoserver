/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Temprary replacement for the pre-Wicket 9 GSModalWindow. Should be rewritten using Wicket9+ modal
 * window facilities
 */
@SuppressWarnings("deprecation")
public class GSModalWindow extends Panel {
    private ModalWindow delegate;

    public GSModalWindow(String id) {
        super(id);
        delegate =
                new ModalWindow("modal") {
                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        // to avoid local style being blocked by CSP
                        tag.remove("style");
                        tag.put("class", "hidden");
                    }
                };
        add(delegate);
    }

    public void setInitialHeight(int initialHeight) {
        delegate.setInitialHeight(initialHeight);
    }

    public void setInitialWidth(int intialWidth) {
        delegate.setInitialWidth(intialWidth);
    }

    public void setContent(Component component) {
        delegate.setContent(component);
    }

    public String getContentId() {
        return delegate.getContentId();
    }

    public IModel<String> getTitle() {
        return delegate.getTitle();
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
    }

    public void setTitle(IModel<String> title) {
        delegate.setTitle(title);
    }

    public void show(AjaxRequestTarget target) {
        delegate.show(target);
    }

    @Override
    protected void onRender() {
        this.internalRenderComponent();
    }

    public void close(AjaxRequestTarget target) {
        delegate.close(target);
    }

    public void setCookieName(String cookieName) {
        delegate.setCookieName(cookieName);
    }

    // This is only used in Metadata extension and taskmanager community module, and might work
    // better on GeoServerDialog.
    public void showUnloadConfirmation(boolean unloadConfirmation) {
        delegate.showUnloadConfirmation(unloadConfirmation);
    }

    public interface PageCreator extends ModalWindow.PageCreator {}

    public interface WindowClosedCallback extends ModalWindow.WindowClosedCallback {}

    public interface CloseButtonCallback extends ModalWindow.CloseButtonCallback {}

    public void setPageCreator(PageCreator pageCreator) {
        delegate.setPageCreator(pageCreator);
    }

    public boolean isShown() {
        return delegate.isShown();
    }

    public void setCloseButtonCallback(GSModalWindow.CloseButtonCallback closeButtonCallback) {
        delegate.setCloseButtonCallback(closeButtonCallback);
    }

    public void setWindowClosedCallback(GSModalWindow.WindowClosedCallback windowClosedCallback) {
        delegate.setWindowClosedCallback(windowClosedCallback);
    }

    // Mostly GeoServerDialog from here down.
    String getHeightUnit() {
        return delegate.getHeightUnit();
    }

    int getInitialHeight() {
        return delegate.getInitialHeight();
    }

    int getInitialWidth() {
        return delegate.getInitialWidth();
    }

    String getWidthUnit() {
        return delegate.getWidthUnit();
    }

    void setHeightUnit(String heightUnit) {
        delegate.setHeightUnit(heightUnit);
    }

    void setWidthUnit(String widthUnit) {
        delegate.setWidthUnit(widthUnit);
    }

    int getMinimalHeight() {
        return delegate.getMinimalHeight();
    }

    int getMinimalWidth() {
        return delegate.getMinimalWidth();
    }

    void setMinimalWidth(int minimalWidth) {
        delegate.setMinimalWidth(minimalWidth);
    }

    void setMinimalHeight(int minimalHeight) {
        delegate.setMinimalHeight(minimalHeight);
    }

    void setResizable(boolean resizable) {
        delegate.setResizable(resizable);
    }
}
