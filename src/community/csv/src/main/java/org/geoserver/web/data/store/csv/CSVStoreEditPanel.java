/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.csv;

import static org.geotools.data.csv.CSVDataStoreFactory.STRATEGYP;
import static org.geotools.data.csv.CSVDataStoreFactory.URL_PARAM;

import java.io.Serializable;
import java.util.Map;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;

/**
 * Provides the form components for the shapefile datastore
 *
 * @author Andrea Aime - GeoSolution
 */
@SuppressWarnings("serial")
public class CSVStoreEditPanel extends /*StoreEditPanel*/ DefaultDataStoreEditPanel{

    public CSVStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, Serializable>> paramsModel = new PropertyModel<>(model, "connectionParameters");
        final IModel<Map<String, Object>> paramsModel2 = new PropertyModel<>(model, "connectionParameters");

        Panel file = buildFileParamPanel(paramsModel2);
        add(file);
        ParamInfo paramMetadata = new ParamInfo(STRATEGYP);
        add(getInputComponent("strategy", paramsModel, paramMetadata));
    }

    protected Panel buildFileParamPanel(final IModel<Map<String, Object>> paramsModel) {
        FileParamPanel file = new FileParamPanel(
                "url", new MapModel<>(paramsModel, URL_PARAM.key), new ParamResourceModel("csv", this), true);
        file.setFileFilter(new Model<>(new ExtensionFileFilter(".csv")));
        file.getFormComponent().add(new FileExistsValidator());
        return file;
    }
}
