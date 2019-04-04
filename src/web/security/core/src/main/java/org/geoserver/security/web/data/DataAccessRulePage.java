/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** A page listing data access rules, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class DataAccessRulePage extends AbstractSecurityPage {

    static final List<CatalogMode> CATALOG_MODES =
            Arrays.asList(CatalogMode.HIDE, CatalogMode.MIXED, CatalogMode.CHALLENGE);

    private GeoServerTablePanel<DataAccessRule> rules;

    private SelectionDataRuleRemovalLink removal;

    private RadioChoice catalogModeChoice;

    public DataAccessRulePage() {
        DataAccessRuleProvider provider = new DataAccessRuleProvider();
        add(
                rules =
                        new GeoServerTablePanel<DataAccessRule>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<DataAccessRule> itemModel,
                                    Property<DataAccessRule> property) {
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
                                removal.setEnabled(rules.getSelection().size() > 0);
                                target.add(removal);
                            }
                        });

        rules.setOutputMarkupId(true);

        setHeaderPanel(headerPanel());

        Form form =
                new Form(
                        "catalogModeForm",
                        new CompoundPropertyModel(
                                new CatalogModeModel(DataAccessRuleDAO.get().getMode())));
        add(form);
        form.add(
                new AjaxLink("catalogModeHelp") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel("catalogModeHelp.title", getPage(), null),
                                new StringResourceModel("catalogModeHelp.message", getPage(), null),
                                new StringResourceModel("catalogModeHelp.hide", getPage(), null),
                                new StringResourceModel("catalogModeHelp.mixed", getPage(), null),
                                new StringResourceModel(
                                        "catalogModeHelp.challenge", getPage(), null));
                    }
                });
        catalogModeChoice =
                new RadioChoice("catalogMode", CATALOG_MODES, new CatalogModeRenderer());
        catalogModeChoice.setSuffix(" ");
        form.add(catalogModeChoice);

        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            DataAccessRuleDAO dao = DataAccessRuleDAO.get();
                            CatalogMode newMode = dao.getByAlias(catalogModeChoice.getValue());
                            dao.setCatalogMode(newMode);
                            dao.storeRules();
                            doReturn();
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error occurred while saving user", e);
                            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
                        }
                    }
                });
        form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
    }

    Component editRuleLink(String id, IModel itemModel, Property<DataAccessRule> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(
                        new EditDataAccessRulePage((DataAccessRule) getDefaultModelObject()));
            }
        };
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(
                new BookmarkablePageLink<NewDataAccessRulePage>(
                        "addNew", NewDataAccessRulePage.class));

        // the removal button
        header.add(removal = new SelectionDataRuleRemovalLink("removeSelected", rules, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    class CatalogModeRenderer extends ChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return (String)
                    new ParamResourceModel(((CatalogMode) object).name(), getPage()).getObject();
        }

        public String getIdValue(Object object, int index) {
            return ((CatalogMode) object).name();
        }
    }
}
