/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.List;
import org.geotools.api.data.Query;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.expression.PropertyName;

/**
 * A mapping of CSW queryables to properties of the metadata record schema. Provides functionality for translating
 * properties and adapting queries.
 */
public interface QueryablesMapping {

    /**
     * Allow the descriptor to adjust the query to the internal representation of records. For example, in the case of
     * SimpleLiteral we have a complex type with simple content, something that we cannot readily represent in GeoTools
     *
     * <p>Must provide a copy, not change the original query.
     */
    Query adaptQuery(Query query);

    /**
     * Return the property name (with dots) for the bounding box property
     *
     * @return the bounding box property name
     */
    String getBoundingBoxPropertyName();

    /**
     * Translate a property from a queryable name to a propertyname, possibly converting to an x-path
     *
     * @return the property name
     */
    List<PropertyName> translateProperty(Name name);
}
