/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class RepeatableAttributeDataProvider<T extends Serializable>
        extends GeoServerDataProvider<ComplexMetadataAttribute<T>> implements IModel<List<T>> {

    private static final long serialVersionUID = -255037580716257623L;

    public static String KEY_VALUE = "value";

    public static String KEY_REMOVE_ROW = "remove";

    public static String KEY_UPDOWN_ROW = "updown";

    private final Property<ComplexMetadataAttribute<T>> VALUE =
            new BeanProperty<ComplexMetadataAttribute<T>>(KEY_VALUE, "value");

    private final Property<ComplexMetadataAttribute<T>> REMOVE_ROW =
            new BeanProperty<ComplexMetadataAttribute<T>>(KEY_REMOVE_ROW, "");

    private final Property<ComplexMetadataAttribute<T>> UPDOWN_ROW =
            new BeanProperty<ComplexMetadataAttribute<T>>(KEY_UPDOWN_ROW, "");

    private final AttributeConfiguration attributeConfiguration;

    private IModel<ComplexMetadataMap> metadataModel;

    private List<ComplexMetadataAttribute<T>> items = new ArrayList<>();

    public RepeatableAttributeDataProvider(
            Class<T> clazz,
            AttributeConfiguration attributeConfiguration,
            IModel<ComplexMetadataMap> metadataModel) {

        this.metadataModel = metadataModel;

        this.attributeConfiguration = attributeConfiguration;

        items = new ArrayList<ComplexMetadataAttribute<T>>();
        for (int i = 0; i < metadataModel.getObject().size(attributeConfiguration.getKey()); i++) {
            Class<T> itemClass = EditorFactory.getInstance().getItemClass(attributeConfiguration);
            items.add(metadataModel.getObject().get(itemClass, attributeConfiguration.getKey(), i));
        }
    }

    @Override
    protected List<Property<ComplexMetadataAttribute<T>>> getProperties() {
        return Arrays.asList(VALUE, UPDOWN_ROW, REMOVE_ROW);
    }

    @Override
    protected List<ComplexMetadataAttribute<T>> getItems() {
        return items;
    }

    public void addField() {
        Class<T> itemClass = EditorFactory.getInstance().getItemClass(attributeConfiguration);
        ComplexMetadataAttribute<T> item =
                metadataModel
                        .getObject()
                        .get(itemClass, attributeConfiguration.getKey(), items.size());
        item.init();
        items.add(item);
    }

    public void removeField(ComplexMetadataAttribute<T> attribute) {
        int index = items.indexOf(attribute);
        // remove from model
        metadataModel.getObject().delete(attributeConfiguration.getKey(), index);
        // remove from view
        items.remove(index);
    }

    public AttributeConfiguration getConfiguration() {
        return attributeConfiguration;
    }

    @Override
    public List<T> getObject() {
        List<T> list = new ArrayList<>();
        for (ComplexMetadataAttribute<T> item : items) {
            list.add(item.getValue());
        }
        return list;
    }

    @Override
    public void setObject(List<T> object) {
        throw new UnsupportedOperationException();
    }
}
