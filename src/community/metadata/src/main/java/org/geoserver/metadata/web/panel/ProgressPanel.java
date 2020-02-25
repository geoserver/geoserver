/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata.web.panel;

import java.io.Serializable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.CloseButtonCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

public class ProgressPanel extends Panel {

    public interface EventHandler extends Serializable {
        public void onFinished(AjaxRequestTarget target);

        public void onCanceled(AjaxRequestTarget target);
    }

    private static final long serialVersionUID = -258488244844400514L;

    private ModalWindow window;

    private boolean cancelMe = false;

    public ProgressPanel(String id) {
        this(id, null);
    }

    public ProgressPanel(String id, IModel<String> title) {
        super(id);

        add(window = new ModalWindow("dialog"));

        window.setInitialHeight(35);
        window.setTitle(title);
        window.setInitialWidth(424);
        window.showUnloadConfirmation(false);
    }

    public void setTitle(IModel<String> title) {
        window.setTitle(title);
    }

    public void start(AjaxRequestTarget target, IModel<Float> model, EventHandler handler) {
        ProgressBar progressBar =
                new ProgressBar(
                        "content",
                        new ProgressionModel() {
                            private static final long serialVersionUID = 5716227987463146386L;

                            protected Progression getProgression() {
                                return new Progression(
                                        cancelMe ? 100 : Math.round(100 * model.getObject()));
                            }
                        }) {
                    private static final long serialVersionUID = 6384204231727968702L;

                    protected void onFinished(AjaxRequestTarget target) {
                        window.close(target);
                        if (cancelMe) {
                            handler.onCanceled(target);
                        } else {
                            handler.onFinished(target);
                        }
                    }
                };

        window.setContent(progressBar);
        window.setCloseButtonCallback(
                new CloseButtonCallback() {
                    private static final long serialVersionUID = 5570427983448661370L;

                    @Override
                    public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                        cancelMe = true;
                        return false;
                    }
                });
        window.show(target);
        progressBar.start(target);
    }

    protected class ProgressPage extends WebPage {
        private static final long serialVersionUID = -6560263676965574430L;

        public ProgressPage(ProgressBar progressBar) {
            WebMarkupContainer panel = new WebMarkupContainer("panel");
            add(panel);
            panel.add(progressBar);
            panel.setOutputMarkupId(true);
        }
    }
}
