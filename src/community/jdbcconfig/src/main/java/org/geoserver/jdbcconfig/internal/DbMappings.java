/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static com.google.common.base.Preconditions.*;
import static org.geoserver.jdbcconfig.internal.DbUtils.*;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.ows.util.ClassProperties;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class DbMappings {

    private static final Logger LOGGER = Logging.getLogger(DbMappings.class);

    private final Dialect dialect;

    private BiMap<Integer, Class<?>> types;

    private BiMap<Class<?>, Integer> typeIds;

    /**
     * Per type oid property types. Keys are {@link #getTypeId(Class) type ids}, values are a map of
     * property name to property type for that type of object.
     */
    private Map<Integer, Map<String, PropertyType>> propertyTypes;

    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Serializable>> INDEXABLE_TYPES =
            ImmutableSet.of( //
                    String.class, //
                    Boolean.class, //
                    Number.class, //
                    BigInteger.class, //
                    BigDecimal.class, //
                    Byte.class, //
                    Short.class, //
                    Integer.class, //
                    Long.class, //
                    Float.class, //
                    Double.class //
                    );

    public DbMappings(Dialect dialect) {
        this.dialect = dialect;
    }

    public Integer getTypeId(Class<?> type) {
        Integer typeId = typeIds.get(type);
        return typeId;
    }

    public Class<?> getType(Integer typeId) {
        return types.get(typeId);
    }

    /** @param template */
    public void initDb(final NamedParameterJdbcOperations template) {

        LOGGER.fine("Initializing Catalog and Config database");

        ClassMappings[] classMsappings = ClassMappings.values();

        {
            BiMap<Integer, Class<?>> existingTypes = loadTypes(template);
            for (ClassMappings cm : classMsappings) {
                Class<? extends Info> clazz = cm.getInterface();
                if (!existingTypes.containsValue(clazz)) {
                    createType(clazz, template);
                }
            }
            this.types = loadTypes(template);
            this.typeIds = this.types.inverse();
        }

        this.propertyTypes = loadPropertyTypes(template);

        // create all direct property types for which we don't need a special mapping entry on
        // nested_properties.properties. Need to do this before adding nested properties for
        // relationships to be found
        for (ClassMappings cm : classMsappings) {
            Class<? extends Info> clazz = cm.getInterface();
            addDirectPropertyTypes(clazz, template);
        }

        // create all nested and/or collection properties, both self and related to other objects,
        // as defined nested_properties.properties
        final Multimap<Class<?>, PropertyTypeDef> nestedPropertyTypeDefs =
                loadNestedPropertyTypeDefs();

        for (ClassMappings cm : classMsappings) {
            Class<? extends Info> clazz = cm.getInterface();
            Collection<PropertyTypeDef> nestedPropDefs = nestedPropertyTypeDefs.get(clazz);
            if (!nestedPropDefs.isEmpty()) {
                addNestedPropertyTypes(template, nestedPropDefs);
            }
        }

        this.propertyTypes = ImmutableMap.copyOf(this.propertyTypes);
    }

    private static class PropertyTypeDef {
        final Class<?> propertyOf;

        final String propertyName;

        @Nullable final Class<?> targetPropertyOf;

        @Nullable final String targetPropertyName;

        @Nullable final boolean isCollection;

        @Nullable final Boolean isText;

        public PropertyTypeDef(
                Class<?> propertyOf,
                String propertyName,
                @Nullable Class<?> targetPropertyOf,
                @Nullable String targetPropertyName,
                boolean isCollection,
                Boolean isText) {
            this.propertyOf = propertyOf;
            this.propertyName = propertyName;
            this.targetPropertyOf = targetPropertyOf;
            this.targetPropertyName = targetPropertyName;
            this.isCollection = isCollection;
            this.isText = isText;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    /** */
    private Multimap<Class<?>, PropertyTypeDef> loadNestedPropertyTypeDefs() {

        Properties properties = loadTypeDefsFromResource();

        Multimap<Class<?>, PropertyTypeDef> byTypePropDefs = ArrayListMultimap.create();

        for (String classPropName : properties.stringPropertyNames()) {
            final String propertyName;
            final Class<?> objectType;
            final boolean collectionProperty;
            final Class<?> targetObjectType;
            final String targetPropertyName;
            final Boolean textProperty;

            {
                int classNameSeparatorIndex = classPropName.indexOf('.');
                String simpleClassName = classPropName.substring(0, classNameSeparatorIndex);
                objectType = toClass(simpleClassName);
                propertyName = classPropName.substring(1 + classNameSeparatorIndex);

                final String propertySpec = properties.getProperty(classPropName);
                String[] propTarget = propertySpec.split(":");

                String targetClassPropName = propTarget.length > 0 ? propTarget[0] : null;
                if (targetClassPropName.trim().length() == 0) {
                    targetObjectType = null;
                    targetPropertyName = null;
                } else {
                    classNameSeparatorIndex = targetClassPropName.indexOf('.');
                    simpleClassName = targetClassPropName.substring(0, classNameSeparatorIndex);
                    targetObjectType = toClass(simpleClassName);
                    targetPropertyName = targetClassPropName.substring(1 + classNameSeparatorIndex);
                }
                String colType = propTarget.length > 1 ? propTarget[1] : null;
                String textType =
                        propTarget.length > 1
                                ? (propTarget.length > 2 ? propTarget[2] : propTarget[1])
                                : null;

                collectionProperty =
                        "list".equalsIgnoreCase(colType) || "set".equalsIgnoreCase(colType);
                if ("text".equalsIgnoreCase(textType)) {
                    textProperty = Boolean.TRUE;
                } else {
                    textProperty = null;
                }
            }

            PropertyTypeDef ptd =
                    new PropertyTypeDef(
                            objectType,
                            propertyName,
                            targetObjectType,
                            targetPropertyName,
                            collectionProperty,
                            textProperty);

            byTypePropDefs.put(objectType, ptd);
        }

        return byTypePropDefs;
    }

    private Properties loadTypeDefsFromResource() {
        Properties properties = new Properties();
        try {
            final String resourceName = "nested_properties.properties";
            URL resource = Resources.getResource(getClass(), resourceName);
            InputStream in = resource.openStream();
            try {
                properties.load(in);
            } finally {
                Closeables.close(in, true);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
        return properties;
    }

    /** @param simpleClassName */
    private Class<?> toClass(String simpleClassName) {
        for (Class<?> c : this.typeIds.keySet()) {
            if (simpleClassName.equalsIgnoreCase(c.getSimpleName())) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown type: '" + simpleClassName + "'");
    }

    private Map<Integer, Map<String, PropertyType>> loadPropertyTypes(
            NamedParameterJdbcOperations template) {
        final String query =
                "select oid, target_property, type_id, name, collection, text from property_type";
        RowMapper<PropertyType> rowMapper =
                new RowMapper<PropertyType>() {
                    @Override
                    public PropertyType mapRow(ResultSet rs, int rowNum) throws SQLException {
                        Integer oid = rs.getInt(1);
                        // cannot use getInteger and we might get BigDecimal or Integer
                        Number targetPropertyOid = (Number) rs.getObject(2);
                        Integer objectTypeOid = rs.getInt(3);
                        String propertyName = rs.getString(4);
                        Boolean collectionProperty = rs.getBoolean(5);
                        Boolean textProperty = rs.getBoolean(6);

                        if (targetPropertyOid != null) {
                            targetPropertyOid = targetPropertyOid.intValue();
                        }
                        PropertyType pt =
                                new PropertyType(
                                        oid,
                                        (Integer) targetPropertyOid,
                                        objectTypeOid,
                                        propertyName,
                                        collectionProperty,
                                        textProperty);

                        return pt;
                    }
                };

        final List<PropertyType> propertyTypes;
        {
            final Map<String, ?> params = Collections.emptyMap();
            propertyTypes = template.query(query, params, rowMapper);
        }

        Map<Integer, Map<String, PropertyType>> perTypeProps = Maps.newHashMap();
        for (PropertyType pt : propertyTypes) {
            Integer objectType = pt.getObjectTypeOid();
            Map<String, PropertyType> typeProperties = perTypeProps.get(objectType);
            if (typeProperties == null) {
                typeProperties = Maps.newHashMap();
                perTypeProps.put(objectType, typeProperties);
            }
            typeProperties.put(pt.getPropertyName(), pt);
        }
        return perTypeProps;
    }

    private BiMap<Integer, Class<?>> loadTypes(NamedParameterJdbcOperations template) {
        String sql = "select oid, typename from type";
        SqlRowSet rowSet = template.queryForRowSet(sql, params("", ""));
        BiMap<Integer, Class<?>> types = HashBiMap.create();
        if (rowSet.first()) {
            do {
                Number oid = (Number) rowSet.getObject(1);
                String typeName = rowSet.getString(2);
                Class<?> clazz;
                try {
                    clazz = Class.forName(typeName);
                } catch (ClassNotFoundException e) {
                    Throwables.throwIfUnchecked(e);
                    throw new RuntimeException(e);
                }
                types.put(oid.intValue(), clazz);
            } while (rowSet.next());
        }
        return types;
    }

    private void createType(Class<? extends Info> clazz, NamedParameterJdbcOperations template) {

        final String typeName = clazz.getName();
        String sql =
                String.format(
                        "insert into type (typename, oid) values (:typeName, %s)",
                        dialect.nextVal("seq_TYPE"));
        int update = template.update(sql, params("typeName", typeName));
        if (1 == update) {
            log("created type " + typeName);
        }
    }

    private void addDirectPropertyTypes(
            final Class<? extends Info> clazz, final NamedParameterJdbcOperations template) {

        log("Creating property mappings for " + clazz.getName());

        final ClassProperties classProperties = new ClassProperties(clazz);

        List<String> properties = Lists.newArrayList(classProperties.properties());
        Collections.sort(properties);

        for (String propertyName : properties) {
            propertyName = fixCase(propertyName);
            Method getter = classProperties.getter(propertyName, null);
            if (getter == null) {
                continue;
            }

            Class<?> returnType = getter.getReturnType();

            if (returnType.isPrimitive()
                    || returnType.isEnum()
                    || INDEXABLE_TYPES.contains(returnType)) {

                final Class<?> componentType =
                        returnType.isArray() ? returnType.getComponentType() : returnType;
                boolean isText =
                        componentType.isEnum()
                                || CharSequence.class.isAssignableFrom(componentType);

                isText &= !"id".equals(propertyName); // id is not on the full text search list of
                // properties
                addPropertyType(template, clazz, propertyName, null, false, isText);
            } else {
                log("Ignoring property " + propertyName + ":" + returnType.getSimpleName());
            }
        }

        log("----------------------");
    }

    /** */
    private void addNestedPropertyTypes(
            final NamedParameterJdbcOperations template,
            Collection<PropertyTypeDef> nestedPropDefs) {

        for (PropertyTypeDef ptd : nestedPropDefs) {
            final Class<?> propertyOf = ptd.propertyOf;
            final String propertyName = ptd.propertyName;
            final boolean isCollection = ptd.isCollection;
            final Class<?> targetPropertyOf = ptd.targetPropertyOf;
            final String targetPropertyName = ptd.targetPropertyName;
            final Boolean isText = ptd.isText;

            PropertyType targetPropertyType = null;
            if (targetPropertyOf != null) {
                final Integer targetPropId = getTypeId(targetPropertyOf);
                checkState(
                        null != targetPropId,
                        Joiner.on("")
                                .join(
                                        "Property ",
                                        propertyOf.getName(),
                                        ".",
                                        propertyName,
                                        " references property ",
                                        targetPropertyOf.getName(),
                                        ".",
                                        targetPropertyName,
                                        " but target property typ does not exist"));

                Map<String, PropertyType> targetPropertyTypes;
                targetPropertyTypes = this.propertyTypes.get(targetPropId);
                checkState(
                        targetPropertyTypes != null,
                        "PropertyTypes of target type "
                                + targetPropertyOf.getName()
                                + " not found while adding property "
                                + propertyName
                                + " of "
                                + propertyOf.getName());

                targetPropertyType = targetPropertyTypes.get(targetPropertyName);
                checkState(targetPropertyType != null);
            }
            boolean text = isText == null ? false : isText.booleanValue();
            addPropertyType(
                    template, propertyOf, propertyName, targetPropertyType, isCollection, text);
        }
    }

    public PropertyType getPropertyType(Integer propId) {
        for (Entry<Integer, Map<String, PropertyType>> e : this.propertyTypes.entrySet()) {
            for (PropertyType pt : e.getValue().values()) {
                if (pt.getOid().equals(propId)) {
                    return pt;
                }
            }
        }
        throw new IllegalArgumentException("PropertyType not found: " + propId);
    }

    /**
     * @return the newly added property type, or {@code null} if it was not added to the database
     *     (i.e. already exists)
     */
    private PropertyType addPropertyType(
            final NamedParameterJdbcOperations template,
            final Class<?> infoClazz,
            final String propertyName,
            @Nullable final PropertyType targetProperty,
            final boolean isCollection,
            final boolean isText) {

        checkNotNull(template);
        checkNotNull(infoClazz);
        checkNotNull(propertyName);
        final Integer typeId = getTypeId(infoClazz);
        if (null == typeId) {
            throw new IllegalStateException("Unknown type id for " + infoClazz.getName());
        }

        Map<String, ?> params;

        log("Checking for ", propertyName);
        String query =
                "select count(*) from property_type " //
                        + "where type_id = :objectType and name = :propName";
        params = params("objectType", typeId, "propName", propertyName);
        logStatement(query, params);
        final int exists = template.queryForObject(query, params, Integer.class);

        PropertyType pType;

        if (exists == 0) {
            log("Adding ", propertyName);

            Integer targetPropertyOid = targetProperty == null ? null : targetProperty.getOid();

            String insert =
                    String.format(
                            "insert into property_type (oid, target_property, type_id, name, collection, text) "
                                    + "values (%s, :target, :type, :name, :collection, :isText)",
                            dialect.nextVal("seq_PROPERTY_TYPE"));

            params =
                    params(
                            "target",
                            targetPropertyOid,
                            "type",
                            typeId,
                            "name",
                            propertyName,
                            "collection",
                            isCollection,
                            "isText",
                            isText);
            logStatement(insert, params);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            template.update(
                    insert, new MapSqlParameterSource(params), keyHolder, new String[] {"oid"});

            // looks like some db's return the pk different than others, so lets try both ways
            Number pTypeKey = (Number) keyHolder.getKeys().get("oid");
            if (pTypeKey == null) {
                pTypeKey = keyHolder.getKey();
            }
            pType =
                    new PropertyType(
                            pTypeKey.intValue(),
                            targetPropertyOid,
                            typeId,
                            propertyName,
                            isCollection,
                            isText);
        } else {
            log(
                    "Not adding property type ",
                    infoClazz.getSimpleName(),
                    ".",
                    propertyName,
                    " as it already exists");
            pType = null;
        }

        if (pType != null) {
            Map<String, PropertyType> map = this.propertyTypes.get(typeId);
            if (map == null) {
                map = Maps.newHashMap();
                this.propertyTypes.put(typeId, map);
            }
            map.put(pType.getPropertyName(), pType);
        }
        return pType;
    }

    /** @param propertyName */
    private String fixCase(String propertyName) {
        if (propertyName.length() > 1) {
            char first = propertyName.charAt(0);
            char second = propertyName.charAt(1);
            if (!Character.isUpperCase(second)) {
                propertyName = Character.toLowerCase(first) + propertyName.substring(1);
            }
        }
        return propertyName;
    }

    private void log(String... msg) {
        String message = Joiner.on("").join(msg).toString();
        // System.err.println(message);
        LOGGER.finer(message);
    }

    /** @param queryType */
    @SuppressWarnings("unchecked")
    public List<Integer> getConcreteQueryTypes(Class<?> queryType) {
        ClassMappings mappings = ClassMappings.fromInterface((Class<? extends Info>) queryType);
        Class<? extends Info>[] concreteInterfaces = mappings.concreteInterfaces();

        List<Integer> inValues = new ArrayList<Integer>(concreteInterfaces.length);
        for (Class<?> type : concreteInterfaces) {
            Integer typeId = getTypeId(type);
            inValues.add(typeId);
        }

        return inValues;
    }

    @SuppressWarnings("unchecked")
    public Set<PropertyType> getPropertyTypes(final Class<?> queryType, String propertyName) {
        checkArgument(queryType.isInterface(), "queryType should be an interface");

        propertyName = removeIndexes(propertyName);

        Set<PropertyType> matches = Sets.newHashSet();

        ClassMappings classMappings;
        classMappings = ClassMappings.fromInterface((Class<? extends Info>) queryType);
        checkState(classMappings != null, "ClassMappings not found for " + queryType);
        Class<? extends Info>[] concreteInterfaces = classMappings.concreteInterfaces();

        for (Class<? extends Info> concreteType : concreteInterfaces) {
            Map<String, PropertyType> propTypes = getPropertyTypes(concreteType);
            if (null == propTypes) {
                continue;
            }
            if (Predicates.ANY_TEXT.getPropertyName().equals(propertyName)) {
                for (PropertyType propertyType : propTypes.values()) {
                    if (propertyType.isText()) {
                        matches.add(propertyType);
                    }
                }
            } else {
                PropertyType propertyType = propTypes.get(propertyName);
                if (null != propertyType) {
                    matches.add(propertyType);
                }
            }
        }
        return matches;
    }

    public Set<Integer> getPropertyTypeIds(Class<?> targetQueryType, String targetPropertyName) {
        Set<PropertyType> propertyTypes = getPropertyTypes(targetQueryType, targetPropertyName);
        Set<Integer> concretePropertyTypeIds = new TreeSet<Integer>();
        for (PropertyType pt : propertyTypes) {
            concretePropertyTypeIds.add(pt.getOid());
        }
        return concretePropertyTypeIds;
    }

    public Map<String, PropertyType> getPropertyTypes(final Class<?> queryType) {
        checkArgument(queryType.isInterface(), "queryType should be an interface");
        final Integer typeId = getTypeId(queryType);
        Map<String, PropertyType> propTypes = this.propertyTypes.get(typeId);
        return propTypes;
    }

    /** @param info */
    public Iterable<Property> properties(Info object) {
        checkArgument(!(object instanceof Proxy));
        final ClassMappings classMappings = ClassMappings.fromImpl(object.getClass());
        checkNotNull(classMappings);
        return properties(object, classMappings);
    }

    public Iterable<Property> changedProperties(Info oldObject, Info object) {
        checkArgument(!(oldObject instanceof Proxy));
        final ClassMappings classMappings = ClassMappings.fromImpl(oldObject.getClass());
        checkNotNull(classMappings);

        ImmutableSet<Property> oldProperties = properties(oldObject, classMappings);
        ImmutableSet<Property> newProperties = properties(object, classMappings);

        Set<Property> changedProps = Sets.difference(newProperties, oldProperties);
        return changedProps;
    }

    private ImmutableSet<Property> properties(Info object, final ClassMappings classMappings) {
        final Class<? extends Info> type = classMappings.getInterface();
        final Integer typeId = getTypeId(type);

        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        final ImmutableList<PropertyType> typeProperties = getTypeProperties(typeId);

        ImmutableSet.Builder<Property> builder = ImmutableSet.builder();
        for (PropertyType pt : typeProperties) {
            String propertyName = pt.getPropertyName();
            Object value;
            if (object instanceof NamespaceInfo && "name".equalsIgnoreCase(propertyName)) {
                // HACK for derived property, ModificationProxy evaluates it to the old value
                value = ((NamespaceInfo) object).getPrefix();
            } else if (object instanceof LayerInfo && "name".equalsIgnoreCase(propertyName)) {
                // HACK for derived property, ModificationProxy evaluates it to old value. Remove
                // when layer name is decoupled from resource name
                value = ((LayerInfo) object).getResource().getName();
            } else if (object instanceof LayerInfo && "title".equalsIgnoreCase(propertyName)) {
                // HACK for derived property, ModificationProxy evaluates it to old value. Remove
                // when layer name is decoupled from resource name
                value = ((LayerInfo) object).getResource().getTitle();
            } else if (object instanceof PublishedInfo
                    && "prefixedName".equalsIgnoreCase(propertyName)) {
                // HACK for derived property, it is not a regular javabean property
                value = ((PublishedInfo) object).prefixedName();
            } else {
                // proceed as it should
                value = ff.property(propertyName).evaluate(object);
            }
            Property prop = new Property(pt, value);
            builder.add(prop);
        }
        return builder.build();
    }

    /** @param typeId */
    private ImmutableList<PropertyType> getTypeProperties(Integer typeId) {
        Map<String, PropertyType> properties = this.propertyTypes.get(typeId);
        return ImmutableList.copyOf(properties.values());
    }

    private String removeIndexes(String propName) {
        int idx;
        while ((idx = propName.indexOf('[')) > 0) {
            String pre = propName.substring(0, idx);
            int closeIdx = propName.indexOf(']');
            String post = propName.substring(1 + closeIdx);
            propName = pre + post;
        }
        return propName;
    }
}
