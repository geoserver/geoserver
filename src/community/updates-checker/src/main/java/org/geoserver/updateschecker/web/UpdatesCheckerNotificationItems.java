/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.updateschecker.UpdatesCheckerNotification;
import org.geoserver.updateschecker.UpdatesCheckerSeverity;

/**
 * What the banner and the Status section share: one list item per notification, and the script and stylesheet that the
 * items need. Styled with GeoServer's own {@code feedbackPanelERROR}, {@code feedbackPanelWARNING} and
 * {@code feedbackPanelINFO} classes, which already follow the light and dark themes.
 */
final class UpdatesCheckerNotificationItems {

    private static final PackageResourceReference SCRIPT =
            new PackageResourceReference(UpdatesCheckerNotificationItems.class, "UpdatesCheckerDismissals.js");

    private static final PackageResourceReference STYLES =
            new PackageResourceReference(UpdatesCheckerNotificationItems.class, "UpdatesCheckerBanner.css");

    private UpdatesCheckerNotificationItems() {}

    /** Adds the script that hides dismissed notifications and the stylesheet. */
    static void renderAssets(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(STYLES));
        response.render(JavaScriptHeaderItem.forReference(SCRIPT));
    }

    /**
     * Fills one list item: the data the script reads, the severity label, the message, the details link and the action
     * links. The values the script needs go in as {@code data-} attributes.
     *
     * @param owner the panel that holds the list, for the language and the action labels
     */
    static void populate(ListItem<UpdatesCheckerNotification> item, Component owner) {
        UpdatesCheckerNotification n = item.getModelObject();
        item.add(AttributeModifier.append("class", cssClass(n.getSeverity())));
        item.add(AttributeModifier.replace("data-type", n.getType().name()));
        item.add(AttributeModifier.replace("data-severity", n.getSeverity().name()));
        item.add(AttributeModifier.replace("data-discriminator", n.getDiscriminator()));
        item.add(AttributeModifier.replace("data-target-version", n.getTargetVersion()));
        item.add(AttributeModifier.replace(
                "data-dismissible", String.valueOf(!n.getActions().isEmpty())));
        item.add(AttributeModifier.replace(
                "data-session-only", String.valueOf(n.getType().isSessionOnly())));
        item.add(new Label("label", n.getSeverity().label(owner.getLocale())));
        item.add(new Label("message", n.getMessageHtml(owner.getLocale())).setEscapeModelStrings(false));

        // the separator goes with the link
        WebMarkupContainer details = new WebMarkupContainer("details");
        details.setVisible(n.getLink() != null);
        item.add(details);
        ExternalLink link = new ExternalLink("link", n.getLink() == null ? "#" : n.getLink());
        link.add(AttributeModifier.replace("target", "_blank"));
        link.add(AttributeModifier.replace("rel", "noopener noreferrer"));
        details.add(link);

        WebMarkupContainer actions = new WebMarkupContainer("actions");
        actions.setVisible(!n.getActions().isEmpty());
        item.add(actions);
        actions.add(new ListView<>("action", n.getActions()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<UpdatesCheckerNotification.Action> actionItem) {
                UpdatesCheckerNotification.Action action = actionItem.getModelObject();
                WebMarkupContainer actionLink = new WebMarkupContainer("actionLink");
                actionLink.add(AttributeModifier.replace("data-action", action.getId()));
                actionItem.add(actionLink);
                String text = owner.getString(action.getMessageKey());
                if (n.getTargetVersion() != null) {
                    text = text.replace("{0}", n.getTargetVersion());
                }
                actionLink.add(new Label("actionText", text));
            }
        });
    }

    private static String cssClass(UpdatesCheckerSeverity severity) {
        switch (severity) {
            case CRITICAL:
            case HIGH:
                return "feedbackPanelERROR";
            case WARN:
                return "feedbackPanelWARNING";
            default:
                return "feedbackPanelINFO";
        }
    }
}
