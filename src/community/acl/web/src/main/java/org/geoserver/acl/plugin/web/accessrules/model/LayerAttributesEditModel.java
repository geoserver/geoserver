/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.acl.domain.rules.LayerDetails.LayerType;

/** */
@SuppressWarnings("serial")
public class LayerAttributesEditModel implements Serializable {

    private final @Getter IModel<Boolean> setAttributesModel;
    private final @NonNull @Getter IModel<List<MutableLayerAttribute>> model;

    private LayerAttributeDataProvider dataProvider;
    private LayerDetailsEditModel details;

    public LayerAttributesEditModel(LayerDetailsEditModel details, IModel<List<MutableLayerAttribute>> model) {
        this.details = details;
        this.model = model;
        dataProvider = new LayerAttributeDataProvider(model);

        boolean hasAttributes = !model.getObject().isEmpty();
        setAttributesModel = Model.of(hasAttributes);
    }

    public LayerAttributeDataProvider getDataProvider() {
        return dataProvider;
    }

    public boolean isShowPanel() {
        LayerType layerType = details.getLayerType();
        return layerType == LayerType.VECTOR;
    }

    public boolean isShowTable() {
        boolean setAttributes = setAttributesModel.getObject().booleanValue();
        return isShowPanel() && setAttributes;
    }

    public boolean layerDetailsHasAttributesSet() {
        return !model.getObject().isEmpty();
    }

    public void computeAttributes() {
        // TODO Auto-generated method stub

    }
}
