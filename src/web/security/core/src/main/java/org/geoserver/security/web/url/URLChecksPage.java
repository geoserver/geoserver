/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.url;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.urlchecks.AbstractURLCheck;
import org.geoserver.security.urlchecks.GeoServerURLChecker;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geotools.data.ows.URLChecker;
import org.geotools.data.ows.URLCheckers;
import org.geotools.util.logging.Logging;

/** Page for configuring URL checks */
public class URLChecksPage extends GeoServerSecuredPage {

    static final Logger LOGGER = Logging.getLogger(URLChecksPage.class);

    GeoServerDialog dialog;

    AjaxLink<Void> removal;
    AjaxCheckBox service;
    Label statusLabel;
    GeoServerTablePanel<AbstractURLCheck> table;

    /** serialVersionUID */
    private static final long serialVersionUID = 5963434654817570467L;

    public URLChecksPage() throws Exception {
        URLCheckDAO dao = getUrlCheckDAO();
        URLCheckProvider provider = new URLCheckProvider(dao.getChecks());
        table =
                new GeoServerTablePanel<>("table", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<AbstractURLCheck> itemModel,
                            Property<AbstractURLCheck> property) {
                        // TODO Auto-generated method stub
                        if (property == URLCheckProvider.NAME) {
                            return urlEntryPageLink(id, itemModel);
                        }
                        if (property == URLCheckProvider.DESCRIPTION) {
                            return new Label(id, itemModel.getObject().getDescription());
                        }
                        if (property == URLCheckProvider.ENABLED) {
                            if (itemModel.getObject().isEnabled())
                                return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                            else return new Label(id, "");
                        }
                        if (property == URLCheckProvider.CONFIGURATION) {
                            return new Label(id, itemModel.getObject().getConfiguration());
                        }
                        return null;
                    }
                };
        table.setPageable(false);
        table.setSortable(false);
        table.setFilterable(false);
        table.setOutputMarkupId(true);
        add(table);

        // checks testing
        Form form = new Form<>("testForm");
        add(form);
        final TextArea<String> testInput = new TextArea<>("testInput", new Model<>());
        testInput.setOutputMarkupId(true);
        form.add(testInput);
        form.add(
                new AjaxSubmitLink("testURL") {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form form) {
                        try {
                            testInput.processInput();

                            URLChecker check = getMatchingRule();

                            if (check != null) {
                                String msg = getMessage("testSuccess", check.getName());
                                info(msg);
                            } else {
                                String msg = getMessage("testFail");
                                error(msg);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Test failed", e);
                            error("Test failed: " + e.getMessage());
                        }
                        addFeedbackPanels(target);
                    }

                    private URLChecker getMatchingRule() throws IOException {
                        String test = testInput.getInput();
                        String normalize = URLCheckers.normalize(test);

                        List<URLChecker> checks = new ArrayList<>();
                        checks.addAll(getUrlCheckDAO().getChecks());
                        checks.addAll(
                                URLCheckers.getEnabledURLCheckers().stream()
                                        .filter(item -> !(item instanceof GeoServerURLChecker))
                                        .collect(Collectors.toList()));

                        for (URLChecker check : checks) {
                            if (check.isEnabled()) {
                                if (check.confirm(normalize)) return check;
                            }
                        }

                        return null;
                    }
                });

        // the add button
        add(new BookmarkablePageLink<>("addNew", RegexCheckPage.class));

        // the removal button
        add(removal = removeSelectedLink("removeSelected"));
        removal.setOutputMarkupId(true);
        removal.setEnabled(true);

        // checkbox to toggle the URL checks
        add(service = getEnabledCheckbox("checksEnabled", new ChecksEnabledModel()));
        service.setOutputMarkupId(true);
        service.setEnabled(true);

        add(statusLabel = new Label("statusLabel", getServiceStatusMessage(dao.isEnabled())));
        statusLabel.setOutputMarkupId(true);

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(360);
        dialog.setInitialHeight(180);
    }

    Component urlEntryPageLink(String id, final IModel<AbstractURLCheck> itemModel) {
        if (itemModel.getObject() instanceof RegexURLCheck) {
            IModel<?> nameModel = URLCheckProvider.NAME.getModel(itemModel);
            return new SimpleBookmarkableLink(
                    id, RegexCheckPage.class, nameModel, "name", (String) nameModel.getObject());
        }

        throw new IllegalArgumentException(
                "Unknown URL check type: " + itemModel.getObject().getClass().getName());
    }

    private AjaxLink<Void> removeSelectedLink(String id) {
        return new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.setTitle(new ParamResourceModel("confirmDeleteTitle", URLChecksPage.this));
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
                                                        URLChecksPage.this,
                                                        table.getSelection().size()));
                                confirmLabel.setEscapeModelStrings(
                                        false); // allow some html inside, like
                                // <b></b>, etc
                                return confirmLabel;
                            }

                            @Override
                            protected boolean onSubmit(
                                    final AjaxRequestTarget target, final Component contents) {
                                URLCheckDAO dao = getUrlCheckDAO();

                                if (table.getSelection().isEmpty()) {
                                    info("Nothing Selected");
                                    return false;
                                }

                                try {
                                    List<AbstractURLCheck> selection = table.getSelection();
                                    List<AbstractURLCheck> checks = dao.getChecks();
                                    checks.removeAll(selection);
                                    dao.saveChecks(checks);
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

    private URLCheckDAO getUrlCheckDAO() {
        return getGeoServerApplication().getBeanOfType(URLCheckDAO.class);
    }

    private AjaxCheckBox getEnabledCheckbox(String id, IModel<Boolean> model) {
        return new AjaxCheckBox(id, model) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                toggleState(target);
            }
        };
    }

    private void toggleState(AjaxRequestTarget target) {
        boolean enabled;
        try {
            enabled = getUrlCheckDAO().isEnabled();
        } catch (Exception e) {
            error(e.getMessage());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return;
        }
        // choose message as per state of service
        String title = enabled ? "confirmDisableTitle" : "confirmEnableTitle";
        String message = enabled ? "confirmDisableMessage" : "confirmEnableMessage";

        dialog.setTitle(new ParamResourceModel(title, this));
        dialog.setDefaultModel(getDefaultModel());

        dialog.showOkCancel(
                target,
                new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Component getContents(final String id) {

                        Label confirmLabel =
                                new Label(id, new ParamResourceModel(message, URLChecksPage.this));
                        confirmLabel.setEscapeModelStrings(false);

                        return confirmLabel;
                    }

                    @Override
                    protected boolean onSubmit(
                            final AjaxRequestTarget target, final Component contents) {
                        // toggle state and save
                        try {
                            getUrlCheckDAO().setEnabled(!enabled);
                            // update status message
                            statusLabel.setDefaultModelObject(
                                    getServiceStatusMessage(getUrlCheckDAO().isEnabled()));
                            target.add(statusLabel);
                        } catch (Exception e) {
                            error(e.getMessage());
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }

                        return true;
                    }
                });
    }

    private String getServiceStatusMessage(boolean isEnabled) {
        if (isEnabled) return getMessage("checksEnabled");
        else return getMessage("checksDisabled");
    }

    private String getMessage(String key, Object... params) {
        return new ParamResourceModel(key, this, params).getString();
    }

    private class ChecksEnabledModel extends LoadableDetachableModel<Boolean> {
        @Override
        protected Boolean load() {
            try {
                return getUrlCheckDAO().isEnabled();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
