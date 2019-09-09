/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class RepeatableComplexAttributeDataProvider
        extends GeoServerDataProvider<ComplexMetadataMap> {

    private static final long serialVersionUID = -255037580716257623L;

    public static String KEY_VALUE = "value";

    public static String KEY_REMOVE_ROW = "remove";

    public static String KEY_UPDOWN_ROW = "updown";

    public static final Property<ComplexMetadataMap> VALUE =
            new BeanProperty<ComplexMetadataMap>(KEY_VALUE, "value");

    private final Property<ComplexMetadataMap> REMOVE_ROW =
            new GeoServerDataProvider.BeanProperty<ComplexMetadataMap>(KEY_REMOVE_ROW, "");

    private final Property<ComplexMetadataMap> UPDOWN_ROW =
            new BeanProperty<ComplexMetadataMap>(KEY_UPDOWN_ROW, "");

    private IModel<ComplexMetadataMap> metadataModel;

    private AttributeConfiguration attributeConfiguration;

    private List<ComplexMetadataMap> items = new ArrayList<>();

    public RepeatableComplexAttributeDataProvider(
            AttributeConfiguration attributeConfiguration,
            IModel<ComplexMetadataMap> metadataModel) {
        this.metadataModel = metadataModel;
        this.attributeConfiguration = attributeConfiguration;

        reset();
    }

    public void reset() {
        items = new ArrayList<ComplexMetadataMap>();
        for (int i = 0; i < metadataModel.getObject().size(attributeConfiguration.getKey()); i++) {
            items.add(metadataModel.getObject().subMap(attributeConfiguration.getKey(), i));
        }
    }

    @Override
    protected List<Property<ComplexMetadataMap>> getProperties() {
        return Arrays.asList(VALUE, UPDOWN_ROW, REMOVE_ROW);
    }

    @Override
    protected List<ComplexMetadataMap> getItems() {
        return items;
    }

    public void addField() {
        ComplexMetadataMap item =
                metadataModel.getObject().subMap(attributeConfiguration.getKey(), items.size());
        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        service.init(item, attributeConfiguration.getTypename());
        items.add(item);
    }

    public void removeField(ComplexMetadataMap attribute) {
        int index = items.indexOf(attribute);
        // remove from model
        metadataModel.getObject().delete(attributeConfiguration.getKey(), index);
        // remove from view
        items.remove(index);
    }

    public AttributeConfiguration getConfiguration() {
        return attributeConfiguration;
    }
}
