/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.control;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.urlchecker.GeoserverURLChecker;
import org.geoserver.security.urlchecker.GeoserverURLConfigService;
import org.geoserver.security.urlchecker.URLEntry;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geotools.util.logging.Logging;

/** @author ImranR */
public class ControlPage extends GeoServerSecuredPage {

    static final Logger LOGGER = Logging.getLogger(ControlPage.class.getCanonicalName());

    public static final String enabledText = "Service is enabled";
    public static final String disabledText = "Service is disabled";

    GeoServerDialog dialog;

    AjaxLink<Void> removal;
    AjaxLink<Void> service;
    Label statusLabel;
    GeoServerTablePanel<URLEntry> table;

    /** serialVersionUID */
    private static final long serialVersionUID = 5963434654817570467L;

    public ControlPage() {
        GeoserverURLConfigService geoserverURLConfigServiceBean =
                GeoServerExtensions.bean(GeoserverURLConfigService.class);
        GeoserverURLChecker urlChecker = geoserverURLConfigServiceBean.getGeoserverURLChecker();
        URLEntryProvider provider = new URLEntryProvider(urlChecker.getRegexList());
        table =
                new GeoServerTablePanel<URLEntry>("table", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<URLEntry> itemModel, Property<URLEntry> property) {
                        // TODO Auto-generated method stub
                        if (property == URLEntryProvider.NAME) {
                            return urlEntryPageLink(id, itemModel);
                        }
                        if (property == URLEntryProvider.DESCRIPTION) {
                            return new Label(id, itemModel.getObject().getDescription());
                        }
                        if (property == URLEntryProvider.ENABLE) {
                            if (itemModel.getObject().isEnable())
                                return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                            else return new Label(id, "");
                        }
                        if (property == URLEntryProvider.REGEX_EXPRESSION) {
                            return new Label(id, itemModel.getObject().getRegexExpression());
                        }
                        return new Label(id, "");
                    }
                };

        table.setOutputMarkupId(true);
        add(table);
        setHeaderPanel(headerPanel(urlChecker));
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(360);
        dialog.setInitialHeight(180);
    }

    Component urlEntryPageLink(String id, final IModel<URLEntry> itemModel) {

        IModel<?> nameModel = URLEntryProvider.NAME.getModel(itemModel);
        return new SimpleBookmarkableLink(
                id, URLEntryPage.class, nameModel, "name", (String) nameModel.getObject());
    }

    protected Component headerPanel(GeoserverURLChecker urlChecker) {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<URLEntryPage>("addNew", URLEntryPage.class));

        // the removal button

        header.add(removal = removeSelectedLink("removeSelected"));
        removal.setOutputMarkupId(true);
        removal.setEnabled(true);

        header.add(service = getEnableLink("service"));
        service.setOutputMarkupId(true);
        service.setEnabled(true);

        header.add(
                statusLabel =
                        new Label("statusLabel", getServiceStatusMessage(urlChecker.isEnabled())));
        statusLabel.setOutputMarkupId(true);
        // check for full admin, we don't allow workspace admins to add new workspaces
        header.setEnabled(isAuthenticatedAsAdmin());
        return header;
    }

    private AjaxLink<Void> removeSelectedLink(String id) {
        return new AjaxLink<Void>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.setTitle(new ParamResourceModel("confirmDeleteTitle", ControlPage.this));
                dialog.setDefaultModel(getDefaultModel());

                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected Component getContents(final String id) {

                                Label confirmLabel =
                                        new Label(
                                                id,
                                                new ParamResourceModel(
                                                        "confirmDeleteMessage",
                                                        ControlPage.this,
                                                        table.getSelection().size()));
                                confirmLabel.setEscapeModelStrings(
                                        false); // allow some html inside, like
                                // <b></b>, etc
                                return confirmLabel;
                            }

                            @Override
                            protected boolean onSubmit(
                                    final AjaxRequestTarget target, final Component contents) {
                                GeoserverURLConfigService geoserverURLConfigServiceBean =
                                        GeoServerExtensions.bean(GeoserverURLConfigService.class);

                                if (table.getSelection().isEmpty()) {
                                    info("Nothing Selected");
                                    return false;
                                }

                                try {
                                    geoserverURLConfigServiceBean.removeAndsave(
                                            table.getSelection());
                                } catch (Exception e) {
                                    error("An Error while deleting URL entries");
                                    LOGGER.log(
                                            Level.SEVERE, "An Error while deleting URL entries", e);
                                }
                                return true;
                            }

                            @Override
                            public void onClose(final AjaxRequestTarget target) {
                                table.clearSelection();
                                target.add(table);
                                setResponsePage(getPage());
                            }
                        });
            }
        };
    }

    private AjaxLink<Void> getEnableLink(String id) {
        return new AjaxLink<Void>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean currentServiceStatus;
                try {
                    GeoserverURLConfigService geoserverURLConfigServiceBean =
                            GeoServerExtensions.bean(GeoserverURLConfigService.class);
                    currentServiceStatus =
                            geoserverURLConfigServiceBean.getGeoserverURLChecker().isEnabled();

                } catch (Exception e) {
                    error(e.getMessage());
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    return;
                }
                // choose message as per state of service
                String title =
                        (currentServiceStatus) ? "confirmDisableTitle" : "confirmEnableTitle";
                String message =
                        (currentServiceStatus) ? "confirmDisableMessage" : "confirmEnableMessage";

                dialog.setTitle(new ParamResourceModel(title, ControlPage.this));
                dialog.setDefaultModel(getDefaultModel());

                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected Component getContents(final String id) {

                                Label confirmLabel =
                                        new Label(
                                                id,
                                                new ParamResourceModel(message, ControlPage.this));
                                confirmLabel.setEscapeModelStrings(false);

                                return confirmLabel;
                            }

                            @Override
                            protected boolean onSubmit(
                                    final AjaxRequestTarget target, final Component contents) {
                                // toggle state and save
                                GeoserverURLConfigService geoserverURLConfigServiceBean =
                                        GeoServerExtensions.bean(GeoserverURLConfigService.class);
                                GeoserverURLChecker copy;
                                try {
                                    copy =
                                            geoserverURLConfigServiceBean
                                                    .getGeoserverURLCheckerCopy();
                                    // toggle state
                                    copy.setEnabled(!copy.isEnabled());
                                    geoserverURLConfigServiceBean.save(copy);
                                    // update status message
                                    statusLabel.setDefaultModelObject(
                                            getServiceStatusMessage(copy.isEnabled()));
                                    target.add(statusLabel);

                                } catch (Exception e) {
                                    error(e.getMessage());
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }

                                return true;
                            }
                        });
            }
        };
    }

    private String getServiceStatusMessage(boolean isEnabled) {
        if (isEnabled) return enabledText;
        else return disabledText;
    }
}
