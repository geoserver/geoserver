/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Optional;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo.EmailDisplayMode;
import org.geoserver.config.impl.UserDetailsDisplaySettingsInfoImpl;
import org.geoserver.web.GeoServerApplication;

/**
 * A customised {@link Label} component for displaying user e-mail addresses in the GeoServer Web UI.
 *
 * <p>The {@code EmailLabel} respects the current GeoServer user details display settings (as configured in
 * {@link UserDetailsDisplaySettingsInfo}) to determine how e-mail addresses are shown to users. The visualisation of
 * the e-mail is controlled by {@link EmailDisplayMode}, and can be one of:
 *
 * <ul>
 *   <li>{@code FULL} – the complete e-mail address is displayed
 *   <li>{@code MASKED} – the local part of the address is partially masked (e.g. {@code j***@example.com})
 *   <li>{@code DOMAIN_ONLY} – only the domain is shown (e.g. {@code example.com})
 *   <li>{@code HIDDEN} – a placeholder text (such as "(hidden)") is shown instead of the e-mail address
 * </ul>
 *
 * Optionally, when the web admin interface settings allows it, this component adds an AJAX click behaviour that
 * replaces the current partially visible e-mail with its full version without reloading the page.
 */
public class EmailLabel extends Label {

    private static boolean REVEALED = false;

    public EmailLabel(String id, final IModel<?> emailModel) {
        super(id, new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                String email = (String) emailModel.getObject();

                if (email == null || email.isEmpty()) {
                    return "";
                }

                if (REVEALED) {
                    /* next load won't reveal email */
                    REVEALED = false;
                    return email;
                }

                return switch (emailDisplayMode()) {
                    case HIDDEN -> hiddenEmailText();
                    case DOMAIN_ONLY -> email.substring(email.lastIndexOf("@") + 1);
                    case MASKED -> email.replaceAll("(?<=.).(?=[^@]*?@)", "*");
                    case FULL -> email;
                };
            }
        });

        setOutputMarkupId(true);

        if (emailDisplayMode() == EmailDisplayMode.HIDDEN) {
            add(new AttributeModifier("class", "italic"));
        }

        if (shouldRevealEmail()) {
            add(new AjaxEventBehavior("click") {
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    REVEALED = true;
                    getDefaultModel().detach();
                    target.add(EmailLabel.this);
                }
            });
        }
    }

    private static EmailDisplayMode emailDisplayMode() {
        UserDetailsDisplaySettingsInfo userDetailsDisplaySettingsInfo = Optional.ofNullable(
                        GeoServerApplication.get().getGeoServer().getGlobal().getUserDetailsDisplaySettings())
                .orElse(new UserDetailsDisplaySettingsInfoImpl());
        return userDetailsDisplaySettingsInfo.getEmailDisplayMode();
    }

    private static String hiddenEmailText() {
        return GeoServerApplication.get()
                .getResourceSettings()
                .getLocalizer()
                .getString("UserPanel.email.hidden", null, "(hidden)");
    }

    private static boolean shouldRevealEmail() {
        return revealEmailAtClick() && emailDisplayMode().allowsReveal();
    }

    private static boolean revealEmailAtClick() {
        UserDetailsDisplaySettingsInfo userDetailsDisplaySettingsInfo = Optional.ofNullable(
                        GeoServerApplication.get().getGeoServer().getGlobal().getUserDetailsDisplaySettings())
                .orElse(new UserDetailsDisplaySettingsInfoImpl());
        return userDetailsDisplaySettingsInfo.getRevealEmailAtClick();
    }
}
