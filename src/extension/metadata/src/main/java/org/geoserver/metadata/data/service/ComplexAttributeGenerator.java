/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.Serializable;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataMap;

public interface ComplexAttributeGenerator extends Serializable {

    String getType();

    void generate(
            AttributeConfiguration attributeConfiguration,
            ComplexMetadataMap metadata,
            LayerInfo layerInfo,
            Object data);

    default boolean supports(ComplexMetadataMap metadata, LayerInfo layerInfo) {
        return true;
    }

    default Component getDialogContent(String id, LayerInfo layerInfo) {
        return new Label(
                id,
                new StringResourceModel("RepeatableComplexAttributesTablePanel.confirmGenerate"));
    }

    default int getDialogContentHeight() {
        return 100;
    }
}
