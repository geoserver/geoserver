/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxChannel;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.Notification;
import org.geoserver.updateschecker.RunningVersion;
import org.geoserver.updateschecker.Status;
import org.geoserver.updateschecker.UpdatesCheckerService;

/**
 * The Status section of the settings page: what is running, when the feed was checked, Check Now, and every
 * notification this browser has not dismissed. Redrawn after the first-view check and after Check Now.
 */
public class StatusPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private final Model<Status> status;

    private final WebMarkupContainer root = new WebMarkupContainer("root");

    public StatusPanel(String id, Status initial) {
        super(id);
        setOutputMarkupId(true);
        status = Model.of(initial);
        root.setOutputMarkupId(true);
        add(root);

        root.add(new Label("error", () -> status.getObject().getCheckFailedMessage(getLocale())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                Status s = status.getObject();
                setVisible(s.isFetchFailed() && s.getRunningKind() != RunningVersion.Kind.UNKNOWN);
            }
        });
        // the values in the header are escaped by Status; only its emphasis is markup
        root.add(new Label("header", () -> status.getObject().getHeaderHtml(getLocale())).setEscapeModelStrings(false));
        root.add(new Label("feedLine", () -> status.getObject().getFeedLine(getLocale())));
        root.add(checkNow());
        root.add(new Label("suppressed", () -> status.getObject().getSuppressedMessage(getLocale())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!status.getObject().getSuppressed().isEmpty());
            }
        });

        IModel<List<Notification>> notifications = () -> status.getObject().getNotifications();
        // always rendered, because notifications.js shows it once every notification is dismissed
        WebMarkupContainer none = new WebMarkupContainer("none");
        none.add(AttributeModifier.replace(
                "hidden", () -> notifications.getObject().isEmpty() ? null : "hidden"));
        root.add(none);
        root.add(new ListView<>("notification", notifications) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Notification> item) {
                NotificationLine.populate(item);
            }
        });
        // notifications.js fills in the count and shows the link when there is something to clear
        WebMarkupContainer clear = new WebMarkupContainer("clear");
        clear.add(AttributeModifier.replace("data-template", new ResourceModel("UpdatesChecker.clear")));
        root.add(clear);

        if (!FirstViewBehavior.isChecked()) {
            add(new FirstViewBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onStatus(AjaxRequestTarget target, Status result) {
                    show(target, result);
                }
            });
        }
    }

    private AjaxLink<Void> checkNow() {
        AjaxLink<Void> link = new AjaxLink<>("checkNow") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                String id = getMarkupId();
                attributes.setChannel(new AjaxChannel("updates-checker", AjaxChannel.Type.ACTIVE));
                attributes
                        .getAjaxCallListeners()
                        .add(new AjaxCallListener()
                                .onBefore("GsUpdatesChecker.checkNow('" + id + "', true);")
                                .onComplete("GsUpdatesChecker.checkNow('" + id + "', false);"));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                Status result = GeoServerExtensions.bean(UpdatesCheckerService.class)
                        .check(UpdatesCheckerService.Trigger.MANUAL);
                FirstViewBehavior.markChecked();
                show(target, result);
            }
        };
        link.setOutputMarkupId(true);
        return link;
    }

    private void show(AjaxRequestTarget target, Status newStatus) {
        status.setObject(newStatus);
        target.add(this);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        NotificationLine.renderAssets(response);
        response.render(OnDomReadyHeaderItem.forScript(
                "GsUpdatesChecker.init(document.getElementById('" + root.getMarkupId() + "'));"));
    }
}
