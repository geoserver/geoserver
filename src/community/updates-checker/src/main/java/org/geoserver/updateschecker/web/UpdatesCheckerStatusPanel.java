/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.UpdatesCheckerNotification;
import org.geoserver.updateschecker.UpdatesCheckerRunningVersion;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.geoserver.updateschecker.UpdatesCheckerStatus;

/**
 * The settings page's Status section: what is running, when the feed was last checked, and every active notification
 * with the same actions as the banner; the ones dismissed in this browser are hidden. Filled from the cache when the
 * page loads and refreshed in place after the first-view fetch and after Check Now, which sits in this panel.
 */
public class UpdatesCheckerStatusPanel extends Panel {

    private final Model<UpdatesCheckerStatus> status;

    private final WebMarkupContainer root;

    public UpdatesCheckerStatusPanel(String id, UpdatesCheckerStatus initial) {
        super(id);
        setOutputMarkupId(true);
        status = Model.of(initial);

        root = new WebMarkupContainer("root");
        root.setOutputMarkupId(true);
        add(root);

        root.add(new Label("error", new StatusText(UpdatesCheckerStatus::getCheckFailedMessage)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                UpdatesCheckerStatus s = status.getObject();
                setVisible(s.isFetchFailed() && s.getRunningKind() != UpdatesCheckerRunningVersion.Kind.UNKNOWN);
            }
        });

        root.add(new Label("feedLine", new StatusText(UpdatesCheckerStatus::getFeedLine)));

        root.add(new AjaxLink<Void>("checkNow") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onInitialize() {
                super.onInitialize();
                setOutputMarkupId(true);
                add(AttributeModifier.replace("data-checking", getString("UpdatesCheckerPage.checking")));
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                // the button reads "Checking..." and stays disabled for at least 2 seconds, even when the answer is
                // quicker
                String id = getMarkupId();
                attributes
                        .getAjaxCallListeners()
                        .add(new AjaxCallListener()
                                .onBefore("return GsUpdatesChecker.checkNowBegin('" + id + "');")
                                .onComplete("GsUpdatesChecker.checkNowEnd('" + id + "');"));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                UpdatesCheckerStatus result = GeoServerExtensions.bean(UpdatesCheckerService.class)
                        .check(UpdatesCheckerService.Trigger.MANUAL);
                // this admin has now had a fetch, so later views in the session need not fetch again
                UpdatesCheckerSessionState.markChecked();
                show(target, result);
            }
        });

        if (!UpdatesCheckerSessionState.isChecked()) {
            add(new UpdatesCheckerFirstViewBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onStatus(AjaxRequestTarget target, UpdatesCheckerStatus result) {
                    show(target, result);
                }
            });
        }

        Label header = new Label("header", new StatusText(UpdatesCheckerStatus::getHeaderHtml));
        // the values in it are escaped by the status; only the emphasis markup is raw
        header.setEscapeModelStrings(false);
        root.add(header);

        final IModel<List<UpdatesCheckerNotification>> notifications = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<UpdatesCheckerNotification> load() {
                return status.getObject().getNotifications();
            }
        };
        // always rendered, because the script shows it when every notification has been dismissed
        WebMarkupContainer none = new WebMarkupContainer("none");
        none.add(AttributeModifier.replace("hidden", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return notifications.getObject().isEmpty() ? null : "hidden";
            }
        }));
        root.add(none);

        // the script fills in the count and shows the link only when this browser has something to clear; the label is
        // looked up when the page renders, because getString() in a constructor runs before the panel is on a page
        WebMarkupContainer clear = new WebMarkupContainer("clear");
        clear.add(AttributeModifier.replace("data-template", new ResourceModel("UpdatesChecker.clear")));
        root.add(clear);
        WebMarkupContainer list = new WebMarkupContainer("notificationList") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!notifications.getObject().isEmpty());
            }
        };
        root.add(list);
        list.add(new ListView<>("notification", notifications) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<UpdatesCheckerNotification> item) {
                UpdatesCheckerNotificationItems.populate(item, UpdatesCheckerStatusPanel.this);
            }
        });
    }

    /** A line of text taken from the shown status, in the language of the page. */
    private class StatusText extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        private final SerializableTextSource source;

        StatusText(SerializableTextSource source) {
            this.source = source;
        }

        @Override
        protected String load() {
            return source.text(status.getObject(), getLocale());
        }
    }

    private interface SerializableTextSource extends Serializable {
        String text(UpdatesCheckerStatus status, Locale locale);
    }

    /** Replaces the shown status and redraws the section. */
    void show(AjaxRequestTarget target, UpdatesCheckerStatus newStatus) {
        status.setObject(newStatus);
        target.add(this);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        UpdatesCheckerNotificationItems.renderAssets(response);
        response.render(OnDomReadyHeaderItem.forScript(
                "GsUpdatesChecker.initStatus(document.getElementById('" + root.getMarkupId() + "'));"));
    }
}
