/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.LinkedHashSet;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes a record, its schema, its possible representations, in a pluggable way
 *
 * @author Andrea Aime - GeoSolutions
 * @author Niels Charlier
 */
public interface RecordDescriptor extends QueryablesMapping {

    /** The GeoTools feature type representing this kind of record */
    FeatureType getFeatureType();

    /** The GeoTools descriptor representing this kind of record */
    AttributeDescriptor getFeatureDescriptor();

    /** The outputSchema name for this feature type */
    String getOutputSchema();

    /**
     * The set of feature properties to be returned for the specified elementSetName (only needs to answer for the
     * ElementSetType#BRIEF and ElementSetType#SUMMARY). The chosen Set implementation must respect the order in which
     * the attributes are supposed to be encoded ({@link LinkedHashSet} will do)
     */
    List<Name> getPropertiesForElementSet(ElementSetType elementSet);

    /** Provides the namespace support needed to handle all schemas used/referenced by this record */
    NamespaceSupport getNamespaceSupport();

    /**
     * Checks that the spatial filters are actually referring to a spatial property. The {@link SpatialFilterChecker}
     * utility class can be used against simple records (like CSW), but more complex record types will need a more
     * sophisticated approach
     */
    void verifySpatialFilters(Filter filter);

    /**
     * Optional support for multiple queryables mappings
     *
     * @param mappingName name of the queryables mapping
     * @return the queryables
     */
    default QueryablesMapping getQueryablesMapping(String mappingName) {
        return this;
    }

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
}
