/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.dataset;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import gmx.iderc.geoserver.tjs.web.TJSBasePage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.web.wicket.XMLNameValidator;
import org.geotools.util.NullProgressListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * A page listing the resources contained in a store, and whose links will bring
 * the user to a new resource configuration page
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class DatasetNewPage extends TJSBasePage {

    String dataStoreId;
    Form form;

    public DatasetNewPage(String dataStoreId, String dataSetName) {

        this.dataStoreId = dataStoreId;
        DatasetInfo newDatasetInfo = getTJSCatalog().getFactory().newDataSetInfo();
        DataStoreInfo dataStoreInfo = getTJSCatalog().getDataStore(dataStoreId);
        newDatasetInfo.setDataStore(dataStoreInfo);
        newDatasetInfo.setDatasetName(dataSetName);
        newDatasetInfo.setEnabled(true);

        final IModel model = new CompoundPropertyModel(newDatasetInfo);
        form = new Form("form", model) {

            @Override
            protected void onSubmit() {
                TJSCatalog catalog = getTJSCatalog();
                DatasetInfo dsi = (DatasetInfo) form.getModelObject();
                catalog.add(dsi);
                catalog.save();
                setResponsePage(DatasetPage.class);
            }
        };
        add(form);

        TextField<String> nameTextField = new TextField<String>("name");
        nameTextField.setRequired(true);
        nameTextField.add(new XMLNameValidator());
        form.add(nameTextField.setRequired(true));

        TJSDataStore store = dataStoreInfo.getTJSDataStore(new NullProgressListener());
        TJSDatasource tJSDatasource = store.getDatasource(dataSetName, dataStoreInfo.getConnectionParameters());
        String[] fields = tJSDatasource.getFields();
        List<String> fieldList = Arrays.asList(fields);

        DropDownChoice<String> geoKeyfield = new DropDownChoice<String>("geoKeyField", fieldList);
        geoKeyfield.setRequired(true);//la llave geografica es obligatoria!!!, Alvaro Javier Fuentes Suarez
        form.add(geoKeyfield);

        DropDownChoice<String> framework = new DropDownChoice<String>("framework", new FrameworkListModel(), new FrameworkListRenderer());
        //el framework es obligatorio!!!!, Alvaro Javier Fuentes Suarez
        form.add(framework.setRequired(true));

        TextField<String> descriptionTextField = new TextField<String>("description");
        form.add(descriptionTextField.setRequired(false));

        TextField<String> organizationTextField2 = new TextField<String>("organization");
        form.add(organizationTextField2.setRequired(false));

        DateTextField referenceDateTextField = new DateTextField("referenceDate", "dd-MM-yyyy");
        referenceDateTextField.setConvertEmptyInputStringToNull(true);
        form.add(referenceDateTextField.setRequired(false));

        TextField<String> versionTextField = new TextField<String>("version");
        form.add(versionTextField.setRequired(false));

        TextField<String> documentationTextField = new TextField<String>("documentation");
        documentationTextField.add(new StringValidator() {

            @Override
            protected void onValidate(IValidatable<String> paramIValidatable) {
                try {
                    URI uri = new URI(paramIValidatable.getValue());
                } catch (URISyntaxException ex) {
                    error(paramIValidatable, "badUriError");
                }
            }
        });
        form.add(documentationTextField.setRequired(false));

        CheckBox enabledChk = new CheckBox("enabled", new PropertyModel(model, "enabled"));
        form.add(enabledChk);

        SubmitLink submitLink = new SubmitLink("submit", form);
        form.add(submitLink);
        form.setDefaultButton(submitLink);

        AjaxLink cancelLink = new AjaxLink("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(DatasetPage.class);
            }
        };
        form.add(cancelLink);
    }

    class FrameworkListModel extends LoadableDetachableModel {

        @Override
        protected Object load() {
            TJSCatalog catalog = TJSExtension.getTJSCatalog();
            List<FrameworkInfo> frameworks = new ArrayList<FrameworkInfo>(catalog.getFrameworks());
            Collections.sort(frameworks, new FrameworkComparator());
            return frameworks;
        }

        class FrameworkComparator implements Comparator<FrameworkInfo> {

            public int compare(FrameworkInfo w1, FrameworkInfo w2) {
                return w1.getName().compareToIgnoreCase(w2.getName());
            }
        }
    }

    class FrameworkListRenderer implements IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return ((FrameworkInfo) object).getName();
        }

        public String getIdValue(Object object, int index) {
            return ((FrameworkInfo) object).getId();
        }
    }
}
