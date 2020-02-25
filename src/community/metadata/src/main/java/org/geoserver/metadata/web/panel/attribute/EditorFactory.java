/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataAttributeModel;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;

/**
 * Factory to generate a component based on the configuration.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class EditorFactory {

    private static final EditorFactory instance = new EditorFactory();

    // private constructor to avoid client applications to use constructor
    private EditorFactory() {}

    public static EditorFactory getInstance() {
        return instance;
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration,
            String id,
            ComplexMetadataMap metadataMap,
            ResourceInfo rInfo) {
        IModel<T> model =
                new ComplexMetadataAttributeModel<T>(
                        metadataMap.get(getItemClass(configuration), configuration.getKey()));
        return create(
                configuration, id, model, metadataMap.subMap(configuration.getKey()), null, rInfo);
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration,
            String id,
            ComplexMetadataAttribute<T> metadataAttribute,
            ResourceInfo rInfo) {
        return create(configuration, id, metadataAttribute, null, rInfo);
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration,
            String id,
            ComplexMetadataAttribute<T> metadataAttribute,
            IModel<List<String>> selection,
            ResourceInfo rInfo) {
        IModel<T> model = new ComplexMetadataAttributeModel<T>(metadataAttribute);
        return create(
                configuration,
                id,
                model,
                new ComplexMetadataMapImpl(new HashMap<String, Serializable>()),
                selection,
                rInfo);
    }

    @SuppressWarnings("unchecked")
    private Component create(
            AttributeConfiguration configuration,
            String id,
            IModel<?> model,
            ComplexMetadataMap submap,
            IModel<List<String>> selection,
            ResourceInfo rInfo) {

        switch (configuration.getFieldType()) {
            case TEXT:
                return new TextFieldPanel(id, (IModel<String>) model);
            case NUMBER:
                return new NumberFieldPanel(id, (IModel<Integer>) model);
            case BOOLEAN:
                return new CheckBoxPanel(id, (IModel<Boolean>) model);
            case DROPDOWN:
                return new DropDownPanel(
                        id,
                        configuration.getKey(),
                        (IModel<String>) model,
                        configuration.getValues(),
                        selection);
            case TEXT_AREA:
                return new TextAreaPanel(id, (IModel<String>) model);
            case DATE:
                return new DateTimeFieldPanel(id, (IModel<Date>) model, false);
            case DATETIME:
                return new DateTimeFieldPanel(id, (IModel<Date>) model, true);
            case UUID:
                return new UUIDFieldPanel(id, (IModel<String>) model);
            case SUGGESTBOX:
                return new AutoCompletePanel(
                        id,
                        (IModel<String>) model,
                        configuration.getValues(),
                        false,
                        configuration,
                        selection);
            case REQUIREBOX:
                return new AutoCompletePanel(
                        id,
                        (IModel<String>) model,
                        configuration.getValues(),
                        true,
                        configuration,
                        selection);
            case COMPLEX:
                return new AttributesTablePanel(
                        id,
                        new AttributeDataProvider(configuration.getTypename(), rInfo),
                        new Model<ComplexMetadataMap>(submap),
                        null,
                        rInfo);
            default:
                break;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> Class<T> getItemClass(
            AttributeConfiguration attributeConfiguration) {
        switch (attributeConfiguration.getFieldType()) {
            case NUMBER:
                return (Class<T>) Integer.class;
            case DATE:
            case DATETIME:
                return (Class<T>) Date.class;
            case BOOLEAN:
                return (Class<T>) Boolean.class;
            default:
                break;
        }
        return (Class<T>) String.class;
    }
}
