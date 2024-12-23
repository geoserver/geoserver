/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.io.IClusterable;

/**
 * Temprary replacement for the pre-Wicket 9 GSModalWindow. Should be rewritten using Wicket9+ modal window facilities
 */
public class GSModalWindow extends Panel {

    private static final long serialVersionUID = 4093464097152933949L;

    private static final String TITLE_ID = "title";

    private final ModalDialog delegate;

    private final ContentsPanel panel;

    private CloseButtonCallback closeButtonCallback = null;
    private WindowClosedCallback windowClosedCallback = null;

    private int initialHeight = 400;
    private int initialWidth = 600;
    private boolean unloadConfirmation = true;

    public GSModalWindow(String id) {
        super(id);
        this.delegate = new ModalDialog("modal");
        this.delegate.add(new DefaultTheme());
        this.delegate.trapFocus();
        add(this.delegate);
        this.panel = new ContentsPanel(ModalDialog.CONTENT_ID);
        this.panel.add(new WebMarkupContainer(TITLE_ID));
        this.panel.add(new WebMarkupContainer(ModalDialog.CONTENT_ID));
        this.panel.add(new AjaxLink<>("close") {

            private static final long serialVersionUID = 8414211581955106952L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (closeButtonCallback == null || closeButtonCallback.onCloseButtonClicked(target)) {
                    close(target);
                }
            }
        });
        this.delegate.setContent(this.panel);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new PackageResourceReference(getClass(), "modal/modal.css")));
        response.render(
                CssHeaderItem.forReference(new PackageResourceReference(getClass(), "modal/GSModalWindow.css")));
        response.render(
                JavaScriptHeaderItem.forReference(new PackageResourceReference(getClass(), "modal/GSModalWindow.js")));
    }

    public void close(AjaxRequestTarget target) {
        this.delegate.close(target);
        if (this.unloadConfirmation) {
            target.prependJavaScript("\n  $(window).off('beforeunload', GSModalWindow.onbeforeunload);");
        }
        if (this.windowClosedCallback != null) {
            this.windowClosedCallback.onClose(target);
        }
    }

    public String getContentId() {
        return ModalDialog.CONTENT_ID;
    }

    @SuppressWarnings("unchecked")
    public IModel<String> getTitle() {
        return (IModel<String>) this.panel.get(TITLE_ID).getDefaultModel();
    }

    public boolean isShown() {
        return this.delegate.isOpen();
    }

    public void setContent(Component component) {
        this.panel.replace(component);
    }

    public void setCloseButtonCallback(CloseButtonCallback closeButtonCallback) {
        this.closeButtonCallback = closeButtonCallback;
    }

    public void setInitialHeight(int initialHeight) {
        this.initialHeight = initialHeight;
    }

    public void setInitialWidth(int initialWidth) {
        this.initialWidth = initialWidth;
    }

    public void setTitle(String title) {
        this.panel.replace(new Label(TITLE_ID, title));
    }

    public void setTitle(IModel<String> title) {
        this.panel.replace(new Label(TITLE_ID, title));
    }

    public void setWindowClosedCallback(WindowClosedCallback windowClosedCallback) {
        this.windowClosedCallback = windowClosedCallback;
    }

    public void show(AjaxRequestTarget target) {
        this.delegate.open(target);
        StringBuilder script = new StringBuilder();
        script.append("\n  $('.w_content_container').css('height', '");
        script.append(this.initialHeight);
        script.append("px');");
        script.append("\n  $('.wicket-modal').css('width', '");
        script.append(this.initialWidth);
        script.append("px');");
        script.append("\n  GSModalWindow.center();");
        if (this.unloadConfirmation) {
            script.append("\n  $(window).on('beforeunload', GSModalWindow.onbeforeunload);");
        }
        target.appendJavaScript(script.toString());
    }

    // only used in metadata extension and taskmanager community module
    public void showUnloadConfirmation(boolean unloadConfirmation) {
        this.unloadConfirmation = unloadConfirmation;
    }

    public interface CloseButtonCallback extends IClusterable {

        boolean onCloseButtonClicked(AjaxRequestTarget target);
    }

    public interface WindowClosedCallback extends IClusterable {

        void onClose(AjaxRequestTarget target);
    }

    private static final class ContentsPanel extends Panel {

        private static final long serialVersionUID = -8770328867678258989L;

        public ContentsPanel(String id) {
            super(id);
        }
    }
}
