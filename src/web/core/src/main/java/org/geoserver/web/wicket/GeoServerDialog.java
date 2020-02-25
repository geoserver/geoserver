/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.Arrays;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * An abstract OK/cancel dialog, subclasses will have to provide the actual contents and behavior
 * for OK/cancel
 */
@SuppressWarnings("serial")
public class GeoServerDialog extends Panel {

    ModalWindow window;
    Component userPanel;
    DialogDelegate delegate;

    public GeoServerDialog(String id) {
        super(id);
        add(window = new ModalWindow("dialog"));
    }

    /** Sets the window title */
    public void setTitle(IModel<String> title) {
        window.setTitle(title);
    }

    public String getHeightUnit() {
        return window.getHeightUnit();
    }

    public int getInitialHeight() {
        return window.getInitialHeight();
    }

    public int getInitialWidth() {
        return window.getInitialWidth();
    }

    public String getWidthUnit() {
        return window.getWidthUnit();
    }

    public void setHeightUnit(String heightUnit) {
        window.setHeightUnit(heightUnit);
    }

    public void setInitialHeight(int initialHeight) {
        window.setInitialHeight(initialHeight);
    }

    public void setInitialWidth(int initialWidth) {
        window.setInitialWidth(initialWidth);
    }

    public void setWidthUnit(String widthUnit) {
        window.setWidthUnit(widthUnit);
    }

    public int getMinimalHeight() {
        return window.getMinimalHeight();
    }

    public int getMinimalWidth() {
        return window.getMinimalWidth();
    }

    public void setMinimalHeight(int minimalHeight) {
        window.setMinimalHeight(minimalHeight);
    }

    public void setMinimalWidth(int minimalWidth) {
        window.setMinimalWidth(minimalWidth);
    }

    /**
     * Shows an OK/cancel dialog. The delegate will provide contents and behavior for the OK button
     * (and if needed, for the cancel one as well)
     */
    public void showOkCancel(AjaxRequestTarget target, final DialogDelegate delegate) {
        // wire up the contents
        userPanel = delegate.getContents("userPanel");
        window.setContent(new ContentsPage(userPanel));

        // make sure close == cancel behavior wise
        window.setCloseButtonCallback(
                new ModalWindow.CloseButtonCallback() {

                    public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                        return delegate.onCancel(target);
                    }
                });
        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        delegate.onClose(target);
                    }
                });

        // show the window
        this.delegate = delegate;
        window.show(target);
    }

    /**
     * Shows an information style dialog box.
     *
     * @param heading The heading of the information topic.
     * @param messages A list of models, displayed each as a separate paragraphs, containing the
     *     information dialog content.
     */
    public void showInfo(
            AjaxRequestTarget target,
            final IModel<String> heading,
            @SuppressWarnings("unchecked") final IModel<String>... messages) {
        window.setPageCreator(
                new ModalWindow.PageCreator() {
                    public Page createPage() {
                        return new InfoPage(heading, messages);
                    }
                });
        window.show(target);
    }

    /**
     * Forcibly closes the dialog.
     *
     * <p>Note that calling this method does not result in any {@link DialogDelegate} callbacks
     * being called.
     */
    public void close(AjaxRequestTarget target) {
        window.close(target);
        delegate = null;
        userPanel = null;
    }

    /** Submits the dialog. */
    public void submit(AjaxRequestTarget target) {
        submit(target, userPanel);
    }

    void submit(AjaxRequestTarget target, Component contents) {
        if (delegate.onSubmit(target, contents)) {
            close(target);
        }
    }

    /** Submit link that will forward to the {@link DialogDelegate} */
    AjaxSubmitLink sumbitLink(Component contents) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("submit") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        submit(target, (Component) this.getDefaultModelObject());
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        delegate.onError(target, form);
                    }
                };
        link.setDefaultModel(new Model<Component>(contents));
        return link;
    }

    /** Link that will forward to the {@link DialogDelegate} */
    Component cancelLink() {
        return new AjaxLink<Void>("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (delegate.onCancel(target)) {
                    window.close(target);
                    delegate = null;
                }
            }
        };
    }

    /**
     * This represents the contents of the dialog.
     *
     * <p>As of wicket 1.3.6 it still has to be a page, see
     * http://www.nabble.com/Nesting-ModalWindow-td19925848.html for details (ajax submit buttons
     * won't work with a panel)
     */
    protected class ContentsPage extends Panel {

        public ContentsPage(Component contents) {
            super("content");
            Form<?> form = new Form<>("form");
            add(form);
            form.add(contents);
            AjaxSubmitLink submit = sumbitLink(contents);
            form.add(submit);
            form.add(cancelLink());
            form.setDefaultButton(submit);
        }
    }

    protected class InfoPage extends WebPage {
        @SafeVarargs
        public InfoPage(IModel<String> title, IModel<String>... messages) {
            add(new Label("title", title));
            add(
                    new ListView<IModel<String>>("messages", Arrays.asList(messages)) {
                        @Override
                        protected void populateItem(ListItem<IModel<String>> item) {
                            item.add(
                                    new Label("message", item.getModelObject())
                                            .setEscapeModelStrings(false));
                        }
                    });
        }
    }

    /**
     * A {@link DialogDelegate} provides the bits needed to actually open a dialog:
     *
     * <ul>
     *   <li>a content pane, that will be hosted inside a {@link Form}
     *   <li>a behavior for the OK button
     *   <li>an eventual behavior for the Cancel button (the base implementation just returns true
     *       to make the window close)
     */
    public abstract static class DialogDelegate implements Serializable {

        /** Builds the contents for this dialog */
        protected abstract Component getContents(String id);

        /**
         * Called when the form inside the dialog breaks. By default adds all feedback panels to the
         * target
         */
        public void onError(final AjaxRequestTarget target, Form<?> form) {
            form.getPage()
                    .visitChildren(
                            IFeedback.class,
                            (component, visit) -> {
                                if (component.getOutputMarkupId()) {
                                    target.add(component);
                                }
                            });
        }

        /**
         * Called when the dialog is closed, allows the delegate to perform ajax updates on the page
         * underlying the dialog
         */
        public void onClose(AjaxRequestTarget target) {
            // by default do nothing
        }

        /**
         * Called when the dialog is submitted
         *
         * @return true if the dialog is to be closed, false otherwise
         */
        protected abstract boolean onSubmit(AjaxRequestTarget target, Component contents);

        /**
         * Called when the dialog is cancelled.
         *
         * @return true if the dialog is to be closed, false otherwise
         */
        protected boolean onCancel(AjaxRequestTarget target) {
            return true;
        }
    }

    public void setResizable(boolean resizable) {
        window.setResizable(resizable);
    }
}
