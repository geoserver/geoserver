/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.LinkedHashSet;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geotools.data.Query;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes a record, its schema, its possible representations, in a pluggable way
 *
 * @author Andrea Aime - GeoSolutions
 * @author Niels Charlier
 */
public interface RecordDescriptor {

    /** The GeoTools feature type representing this kind of record */
    FeatureType getFeatureType();

    /** The GeoTools descriptor representing this kind of record */
    AttributeDescriptor getFeatureDescriptor();

    /** The outputSchema name for this feature type */
    String getOutputSchema();

    /**
     * The set of feature properties to be returned for the specified elementSetName (only needs to
     * answer for the ElementSetType#BRIEF and ElementSetType#SUMMARY). The chosen Set
     * implementation must respect the order in which the attributes are supposed to be encoded
     * ({@link LinkedHashSet} will do)
     */
    List<Name> getPropertiesForElementSet(ElementSetType elementSet);

    /**
     * Provides the namespace support needed to handle all schemas used/referenced by this record
     */
    NamespaceSupport getNamespaceSupport();

    /**
     * Allow the descriptor to adjust the query to the internal representation of records. For
     * example, in the case of SimpleLiteral we have a complex type with simple content, something
     * that we cannot readily represent in GeoTools
     */
    Query adaptQuery(Query query);

    /**
     * Return the property name (with dots) for the bounding box property
     *
     * @return the bounding box property name
     */
    String getBoundingBoxPropertyName();

    /**
     * Return the queryables for this type of record (for getcapabilities)
     *
     * @return list of queryable property names
     */
    List<Name> getQueryables();

    /**
     * Return a description of the queriables according to iso standard (for getcapabilities)
     *
     * @return the description string
     */
    String getQueryablesDescription();

    /**
     * Translate a property from a queryable name to a propertyname, possibly converting to an
     * x-path
     *
     * @return the property name
     */
    PropertyName translateProperty(Name name);

    /**
     * Checks that the spatial filters are actually referring to a spatial property. The {@link
     * SpatialFilterChecker} utility class can be used against simple records (like CSW), but more
     * complex record types will need a more sophisticated approach
     */
    void verifySpatialFilters(Filter filter);
}
