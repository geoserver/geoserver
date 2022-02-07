/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.ogcapi.AttributeType;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.QueryablesBuilder;
import org.geotools.filter.visitor.ExpressionTypeVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

public class STACQueryablesBuilder {

    public static final String GEOMETRY_SCHEMA_REF =
            "https://schemas.stacspec.org/v1.0.0/item-spec/json-schema/item.json#/geometry";
    public static final String ID_SCHEMA_REF =
            "https://schemas.stacspec.org/v1.0.0/item-spec/json-schema/item.json#/id";
    public static final String COLLECTION_SCHEMA_REF =
            "https://schemas.stacspec.org/v1.0.0/item-spec/json-schema/item.json#/collection";
    public static final String DATETIME_SCHEMA_REF =
            "https://schemas.stacspec.org/v1.0.0/item-spec/json-schema/datetime.json#/properties/datetime";
    private static Set<String> SKIP_PROPERTIES = new HashSet<>();

    static {
        // replaced by "datetime"
        SKIP_PROPERTIES.add("start_datetime");
        SKIP_PROPERTIES.add("end_datetime");
        SKIP_PROPERTIES.add("datetime");
        // replaced by "geometry"
        SKIP_PROPERTIES.add("footprint");
        // replaced by "id"
        SKIP_PROPERTIES.add("eoIdentifier");
        // replaced by "collection"
        SKIP_PROPERTIES.add("parentIdentifier");
    }

    private final TemplateBuilder template;
    private final Queryables queryables;
    private final FeatureType itemsSchema;

    public STACQueryablesBuilder(String id, TemplateBuilder template, FeatureType itemsSchema) {
        this.queryables = new QueryablesBuilder(id).build();
        this.queryables.setProperties(new LinkedHashMap<>());
        this.template = template;
        this.itemsSchema = itemsSchema;
    }

    Queryables getQueryables() throws IOException {
        AbstractTemplateBuilder features = lookupBuilder(template, "features");
        if (features != null) {
            AbstractTemplateBuilder properties = lookupBuilder(features, "properties");
            if (properties != null) visitTemplateBuilder(null, properties, true);
        }
        // force in the extra properties not found under properties
        Map<String, Schema> properties = this.queryables.getProperties();
        properties.put("id", getSchema("ID", ID_SCHEMA_REF));
        properties.put("collection", getSchema("Collection", COLLECTION_SCHEMA_REF));
        properties.put("geometry", getSchema("Geometry", GEOMETRY_SCHEMA_REF));
        properties.put("datetime", getSchema("Datetime", DATETIME_SCHEMA_REF));

        return this.queryables;
    }

    private Schema<?> getSchema(String description, String ref) {
        Schema schema = new Schema();
        schema.set$ref(ref);
        schema.setDescription(description);
        return schema;
    }

    private AbstractTemplateBuilder lookupBuilder(TemplateBuilder parent, String key) {
        for (TemplateBuilder child : parent.getChildren()) {
            if (child instanceof AbstractTemplateBuilder) {
                AbstractTemplateBuilder atb = (AbstractTemplateBuilder) child;
                if (key.equals(atb.getKey(null))) {
                    return atb;
                } else if (atb instanceof CompositeBuilder && atb.getKey(null) == null) {
                    return lookupBuilder(atb, key);
                }
            }
        }
        return null;
    }

    private void visitTemplateBuilder(String parentPath, TemplateBuilder atb, boolean skipPath) {
        // no queryables out of static builders for the moment, we migth want
        // to revisit once we consider eventual filters
        if (atb instanceof StaticBuilder || !(atb instanceof AbstractTemplateBuilder)) return;

        // check the key, if we get a null it means the key is dynamic and it's not possible
        // to do anything with this JSON sub-tree
        String key = ((AbstractTemplateBuilder) atb).getKey(null);
        if (key == null) return;

        String path = getPath(parentPath, key, skipPath);
        if (atb instanceof DynamicValueBuilder) {
            DynamicValueBuilder db = (DynamicValueBuilder) atb;
            if (SKIP_PROPERTIES.contains(key)) return;
            queryables.getProperties().put(path, getSchema(db));
        } else {
            for (TemplateBuilder child : atb.getChildren()) {
                visitTemplateBuilder(path, child, false);
            }
        }
    }

    private String getPath(String parentPath, String key, boolean skipPath) {
        if (skipPath) return null;
        return parentPath == null ? key : parentPath + "." + key;
    }

    private Schema getSchema(DynamicValueBuilder db) {
        Class<?> binding = null;
        if (db.getXpath() != null) {
            Object result = db.getXpath().evaluate(itemsSchema);
            if (result instanceof PropertyDescriptor) {
                PropertyDescriptor pd = (PropertyDescriptor) result;
                binding = pd.getType().getBinding();
            }
        } else if (db.getCql() != null) {
            ExpressionTypeVisitor visitor = new ExpressionTypeVisitor(itemsSchema);
            binding = (Class<?>) db.getCql().accept(visitor, null);
        }
        // did we get a binding?
        if (binding != null) {
            return QueryablesBuilder.getSchema(binding);
        }
        // fall back for property not found or undetermined type
        return getGenericSchema();
    }

    private Schema getGenericSchema() {
        Schema schema = new Schema();
        schema.setType(AttributeType.STRING.getType());
        return schema;
    }
}
