/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.dggs;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.dggs.DGGSFactoryFinder;
import org.geotools.dggs.gstore.DGGSGeometryStoreFactory;

/**
 * Provides the form components for the shapefile datastore
 *
 * @author Andrea Aime - GeoSolution
 */
public class DGGSGeometryStoreEditPanel extends StoreEditPanel {

    public DGGSGeometryStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, ?>> paramsModel =
                new PropertyModel<>(model, "connectionParameters");

        IModel<Serializable> valueModel =
                new MapModel<>(paramsModel, DGGSGeometryStoreFactory.DGGS_FACTORY_ID.key);
        IModel<String> labelModel = new ParamResourceModel("DGGSFactoryId", this);
        DropDownChoiceParamPanel parameterPanel =
                new DropDownChoiceParamPanel(
                        "factoryId", valueModel, labelModel, getDGGSFactoryIds(), true);
        add(parameterPanel);
    }

    private List<String> getDGGSFactoryIds() {
        return DGGSFactoryFinder.getFactoryIdentifiers().collect(Collectors.toList());
    }
}
