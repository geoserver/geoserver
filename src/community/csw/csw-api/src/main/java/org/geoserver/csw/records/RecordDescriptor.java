/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.LinkedHashSet;
import java.util.Set;

import net.opengis.cat.csw20.ElementSetType;

import org.geotools.data.Query;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes a record, its schema, its possible representations, in a pluggable way
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface RecordDescriptor {

    /**
     * The GeoTools feature type representing this kind of record
     * @return
     */
    FeatureType getFeatureType();
    
    /**
     * The outputSchema name for this feature type
     */
    String getOutputSchema();
    
    /**
     * The set of feature properties to be returned for the specified elementSetName (only needs
     * to answer for the ElementSetType#BRIEF and ElementSetType#SUMMARY). 
     * The chosen Set implementation must respect the order in which the attributes are supposed
     * to be encoded ({@link LinkedHashSet} will do)
     */
    Set<Name> getPropertiesForElementSet(ElementSetType elementSet);
    
    /**
     * Provides the namespace support needed to handle all schemas used/referenced by this record 
     * @return
     */
    NamespaceSupport getNamespaceSupport();
    
    /**
     * Allow the descriptor to adjust the query to the internal representation of records.
     * For example, in the case of SimpleLiteral we have a complex type with simple content,
     * something that we cannot readily represent in GeoTools
     * 
     * @param query
     * @return
     */
    Query adaptQuery(Query query);
}
