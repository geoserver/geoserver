package org.geoserver.web.security.catalog;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.data.CatalogModeModel;
import org.geoserver.web.wicket.ParamResourceModel;

public class CatalogModePage extends GeoServerSecuredPage {

    List<CatalogMode> CATALOG_MODES = Arrays.asList(CatalogMode.HIDE, CatalogMode.MIXED,
            CatalogMode.CHALLENGE);

    Form formCatalogMode;

    RadioChoice catalogMode;

    public CatalogModePage() {
        setDefaultModel(new CompoundPropertyModel(new CatalogModeModel(DataAccessRuleDAO.get().getMode())));

        formCatalogMode = new Form("catalogModeForm");
        add(formCatalogMode);
        catalogMode = new RadioChoice("catalogMode", CATALOG_MODES, new CatalogModeRenderer());
        catalogMode.setSuffix(" ");
        formCatalogMode.add(catalogMode);
        
        formCatalogMode.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
        formCatalogMode.add(saveLink());
    }

    SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                onFormSubmit();
            }

            private void onFormSubmit() {
                try {
                    DataAccessRuleDAO dao = DataAccessRuleDAO.get();
                    CatalogMode newMode = dao.getByAlias(catalogMode.getValue());
                    dao.setCatalogMode(newMode);
                    dao.storeRules();
                    setResponsePage(CatalogModePage.class);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error occurred while saving user", e);
                    error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
                }

            }
        };
    }

    class CatalogModeRenderer implements IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return (String) new ParamResourceModel(((CatalogMode) object).name(), getPage())
                    .getObject();
        }

        public String getIdValue(Object object, int index) {
            return ((CatalogMode) object).name();
        }
    }

}
