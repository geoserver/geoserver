/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.mosaic;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.DefaultDataStoreEditPanel;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.TextAreaParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.data.DataStoreFinder;

public class VectorMosaicDataStoreEditPanel extends DefaultDataStoreEditPanel {
    private static final String SPI_PARAM_NAME = "preferredDataStoreSPI";
    private static final String COMMON_PARAMETERS_PARAM_NAME = "commonParameters";
    private static final String DELEGATE_STORE_PARAM_NAME = "delegateStoreName";
    private static final String VECTOR_MOSAIC_DATASTORE_SPI = "org.geotools.vectormosaic.VectorMosaicStoreFactory";
    private static final String VECTOR_MOSAIC_DATASTORE_TYPE = "Vector Mosaic Data Store";
    /**
     * Creates a new parameters panel with a list of input fields matching the {@link Param}s for the Vector Mosaic
     * factory that's the model of the provided {@code Form}.
     *
     * @param componentId the id for this component instance
     * @param storeEditForm the form being build by the calling class, whose model is the {@link DataStoreInfo} being
     *     edited
     */
    public VectorMosaicDataStoreEditPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);
    }

    @Override
    protected Panel getInputComponent(
            final String componentId,
            final IModel<Map<String, Serializable>> paramsModel,
            final ParamInfo paramMetadata) {

        if (paramMetadata.getName().equals(SPI_PARAM_NAME)) {
            Iterator<DataStoreFactorySpi> dataStoreFactorySpiIterator = DataStoreFinder.getAvailableDataStores();
            List<String> dataStoreFactorySpiList = new ArrayList<>();
            while (dataStoreFactorySpiIterator.hasNext()) {
                DataStoreFactorySpi dataStoreFactorySpi = dataStoreFactorySpiIterator.next();
                if (!dataStoreFactorySpi.getClass().getName().equals(VECTOR_MOSAIC_DATASTORE_SPI)) {
                    dataStoreFactorySpiList.add(dataStoreFactorySpi.getClass().getName());
                }
            }
            dataStoreFactorySpiList.sort(Collator.getInstance());
            IModel<Serializable> mapModel = new MapModel<>(paramsModel, SPI_PARAM_NAME);
            ResourceModel labelModel = new ResourceModel(componentId, SPI_PARAM_NAME);
            return new DropDownChoiceParamPanel(componentId, mapModel, labelModel, dataStoreFactorySpiList, false);
        } else if (paramMetadata.getName().equals(DELEGATE_STORE_PARAM_NAME)) {
            List<String> storeNames = GeoServerApplication.get().getCatalog().getStores(DataStoreInfo.class).stream()
                    .filter(dataStoreInfo -> !dataStoreInfo.getType().equals(VECTOR_MOSAIC_DATASTORE_TYPE))
                    .map(DataStoreInfo::getName)
                    .collect(Collectors.toList());
            storeNames.sort(Collator.getInstance());
            IModel<Serializable> mapModel = new MapModel<>(paramsModel, DELEGATE_STORE_PARAM_NAME);
            ResourceModel labelModel = new ResourceModel(componentId, DELEGATE_STORE_PARAM_NAME);
            return new DropDownChoiceParamPanel(componentId, mapModel, labelModel, storeNames, true);
        } else if (paramMetadata.getName().equals(COMMON_PARAMETERS_PARAM_NAME)) {
            // multiline textbox for properties-like content
            IModel<String> mapModel = new MapModel<>(paramsModel, COMMON_PARAMETERS_PARAM_NAME);
            ResourceModel labelModel = new ResourceModel(componentId, COMMON_PARAMETERS_PARAM_NAME);
            return new TextAreaParamPanel(componentId, mapModel, labelModel, false);
        } else {
            return super.getInputComponent(componentId, paramsModel, paramMetadata);
        }
    }
}
