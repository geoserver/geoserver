/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.browser.DirectoryInput;

/** A page listing data access rules, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class DataSecurityPage extends AbstractSecurityPage {

    // ArrayList as it needs to be serializable
    static final ArrayList<CatalogMode> CATALOG_MODES =
            new ArrayList<>(Arrays.asList(CatalogMode.HIDE, CatalogMode.MIXED, CatalogMode.CHALLENGE));

    private GeoServerTablePanel<DataAccessRule> rules;

    private SelectionDataRuleRemovalLink removal;

    private RadioChoice<CatalogMode> catalogModeChoice;

    public DataSecurityPage() {
        DataAccessRuleProvider provider = new DataAccessRuleProvider();
        add(
                rules = new GeoServerTablePanel<>("table", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<DataAccessRule> itemModel, Property<DataAccessRule> property) {
                        if (property == DataAccessRuleProvider.RULEKEY) {
                            return editRuleLink(id, itemModel, property);
                        }
                        if (property == DataAccessRuleProvider.ROLES) {
                            return new Label(id, property.getModel(itemModel));
                        }
                        throw new RuntimeException("Uknown property " + property);
                    }

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        removal.setEnabled(!rules.getSelection().isEmpty());
                        target.add(removal);
                    }
                });

        rules.setOutputMarkupId(true);

        setHeaderPanel(headerPanel());

        Form form = new Form<>("otherSettingsForm");
        add(form);
        form.add(new HelpLink("catalogModeHelp").setDialog(dialog));

        DataAccessRuleDAO dataAccessRuleDAO = DataAccessRuleDAO.get();
        catalogModeChoice = new RadioChoice<>(
                "catalogMode",
                new Model<>(dataAccessRuleDAO.getMode()),
                new Model<>(CATALOG_MODES),
                new CatalogModeRenderer());
        catalogModeChoice.add(new FormComponentUpdatingBehavior() {});
        catalogModeChoice.setSuffix(" ");
        form.add(catalogModeChoice);

        // Filesystem sandbox configuration, available only if the system administrator did
        // not set it via a system property
        WebMarkupContainer sandboxContainer = new WebMarkupContainer("sandboxContainer");
        form.add(sandboxContainer);
        DefaultFileAccessManager fam = GeoServerExtensions.bean(
                DefaultFileAccessManager.class, getGeoServerApplication().getApplicationContext());
        sandboxContainer.setVisible(!fam.isSystemSanboxEnabled());
        Model<String> sandboxModel = new Model<>(dataAccessRuleDAO.getFilesystemSandbox());
        DirectoryInput sandboxInput = new DirectoryInput(
                "sandbox",
                sandboxModel,
                new ParamResourceModel("sandbox", this),
                false,
                new DirectoryExistsValidator());
        sandboxInput.setPrefixPaths(false);
        sandboxContainer.add(sandboxInput);

        form.add(new SubmitLink("save") {
            @Override
            public void onSubmit() {
                try {
                    // not serializable, so we cannot use the variable in the outer class
                    DataAccessRuleDAO dao = DataAccessRuleDAO.get();
                    CatalogMode newMode = dao.getByAlias(catalogModeChoice.getValue());
                    dao.setCatalogMode(newMode);
                    dao.setFilesystemSandbox(sandboxModel.getObject());
                    dao.storeRules();
                    doReturn();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error occurred while saving user", e);
                    error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
                }
            }
        });
        form.add(new BookmarkablePageLink<>("cancel", GeoServerHomePage.class));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // Content-Security-Policy: inline styles must be nonce=...
        String css = " #catalogMode {\n"
                + "         display:block;\n"
                + "         padding-top: 0.5em;\n"
                + "       }\n"
                + "       #catalogMode input {\n"
                + "          display: block;\n"
                + "          float: left;\n"
                + "          clear:left;\n"
                + "          padding-top:0.5em;\n"
                + "          margin-bottom: 0.5em;\n"
                + "       }\n"
                + "       #catalogMode label {\n"
                + "          clear:right;\n"
                + "          margin-bottom: 0.5em;\n"
                + "       }";
        response.render(CssHeaderItem.forCSS(css, "org-geoserver-security-web-data-DataSecurityPage-1"));
    }

    Component editRuleLink(String id, IModel<DataAccessRule> itemModel, Property<DataAccessRule> property) {
        return new SimpleAjaxLink<>(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(new EditDataAccessRulePage(getModelObject()));
            }
        };
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<>("addNew", NewDataAccessRulePage.class));

        // the removal button
        header.add(removal = new SelectionDataRuleRemovalLink("removeSelected", rules, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    class CatalogModeRenderer extends ChoiceRenderer<CatalogMode> {

        @Override
        public Object getDisplayValue(CatalogMode object) {
            return new ParamResourceModel(object.name(), getPage()).getObject();
        }

        @Override
        public String getIdValue(CatalogMode object, int index) {
            return object.name();
        }
    }

    private static class DirectoryExistsValidator implements IValidator<String> {
        private static final long serialVersionUID = 1L;

        @Override
        public void validate(IValidatable<String> validatable) {
            String path = validatable.getValue();
            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (!file.exists() || !file.isDirectory()) {
                    ValidationError error = new ValidationError(this);
                    error.addKey("DataSecurityPage.sanboxNotFoundError");
                    validatable.error(error);
                }
            }
        }
    }
}
