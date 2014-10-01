/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.columns;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.web.TJSBasePage;
import gmx.iderc.geoserver.tjs.web.dataset.DatasetEditPage;
import gmx.iderc.geoserver.tjs.web.dataset.DatasetPage;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * A page listing the resources contained in a store, and whose links will bring
 * the user to a new resource configuration page
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class ColumnEditPage extends TJSBasePage {

    Form form;
    DatasetInfo datasetInfo;
    PageParameters parameters;

    public ColumnEditPage(PageParameters parameters) {
        String dataStoreId = parameters.getString("dataStoreId");
        String datasetName = parameters.getString("datasetName");
        String columnName = parameters.getString("columnName");
        this.parameters = parameters;

        datasetInfo = getTJSCatalog().getDataset(dataStoreId, datasetName);
        ColumnInfo columnInfo = datasetInfo.getColumn(columnName);

        final IModel model = new CompoundPropertyModel(columnInfo);
        form = new Form("form", model) {

            @Override
            protected void onSubmit() {
                TJSCatalog catalog = getTJSCatalog();
//                DatasetInfo dsi = (DatasetInfo) form.getModelObject();
                catalog.save(datasetInfo);
                setResponsePage(DatasetEditPage.class, ColumnEditPage.this.parameters);
            }
        };
        add(form);

        TextField<String> nameTextField = new TextField<String>("name");
        nameTextField.setEnabled(false);
        form.add(nameTextField.setRequired(true));

        TextField<String> typeTextField = new TextField<String>("type");
        typeTextField.setEnabled(false);
        form.add(typeTextField.setRequired(true));

        TextField<String> titleTextField = new TextField<String>("title");
        form.add(titleTextField.setRequired(true));

        TextField<String> descriptionTextField = new TextField<String>("abstractValue");
        form.add(descriptionTextField.setRequired(false));

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

        TextField<String> valueOUMField = new TextField<String>("valueUOM");
        form.add(valueOUMField.setRequired(false));

        DropDownChoice<String> purposeValues = new DropDownChoice<String>(
                                                                                 "purpose",
                                                                                 Arrays.asList(new String[]{
                                                                                                                   "SpatialComponentIdentifier",
                                                                                                                   "SpatialComponentProportion",
                                                                                                                   "SpatialComponentPercentage",
                                                                                                                   "TemporalIdentifier",
                                                                                                                   "TemporalValue",
                                                                                                                   "VerticalIdentifier",
                                                                                                                   "VerticalValue",
                                                                                                                   "OtherSpatialIdentifier",
                                                                                                                   "NonSpatialIdentifer",
                                                                                                                   "Attribute"
                                                                                 })
        );
        form.add(purposeValues.setRequired(true));

        CheckBox enabledChk = new CheckBox("enabled", new PropertyModel(this, "enabled"));
        form.add(enabledChk);

        SubmitLink submitLink = new SubmitLink("save", form);
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
}
