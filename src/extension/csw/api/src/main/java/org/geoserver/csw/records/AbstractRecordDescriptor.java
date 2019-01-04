/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.LinkedList;
import java.util.List;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes a record, its schema, its possible representations, in a pluggable way The Abstract
 * class provides some default behaviour.
 *
 * @author Niels Charlier
 */
public abstract class AbstractRecordDescriptor implements RecordDescriptor {

    public static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    public static final String DEFAULT_CRS_NAME = "urn:x-ogc:def:crs:EPSG:6.11:4326";

    public static final CoordinateReferenceSystem DEFAULT_CRS;

    static {
        // build the default CRS
        try {
            DEFAULT_CRS = CRS.decode(DEFAULT_CRS_NAME);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to decode the default CRS, this should never happen!", e);
        }
    }

    /**
     * The GeoTools feature type representing this kind of record The default method retrieves type
     * from the descriptor
     *
     * @return the feature type
     */
    public FeatureType getFeatureType() {
        return (FeatureType) getFeatureDescriptor().getType();
    }

    /**
     * Helper method to create a list of names from namespace support and a sequence of strings
     *
     * @param ns Namespace Support
     * @param names Sequence of name strings
     * @return the List of Names
     */
    protected static List<Name> createNameList(NamespaceSupport ns, String... names) {
        List<Name> result = new LinkedList<Name>();
        for (String name : names) {
            String[] splitted = name.split(":");
            String uri, localName;
            if (splitted.length == 1) {
                uri = ns.getURI("");
                localName = splitted[0];
            } else {
                uri = ns.getURI(splitted[0]);
                localName = splitted[1];
            }
            result.add(new NameImpl(uri, localName));
        }

        return result;
    }

    /**
     * Helper method to build a property name from a simple name (not an x-path) with namespace
     * support.
     *
     * @param namespaces Namespace support
     * @param name the Name
     * @return the PropertyName
     */
    public static PropertyName buildPropertyName(NamespaceSupport namespaces, Name name) {
        String ns = name.getNamespaceURI();
        String localName = name.getLocalPart();

        String prefix = namespaces.getPrefix(ns);
        // build the xpath with the prefix, or not if we don't have one
        String xpath;
        if (prefix != null && !"".equals(prefix)) {
            xpath = prefix + ":" + localName;
        } else {
            xpath = localName;
        }

        PropertyName property = ff.property(xpath, namespaces);
        return property;
    }
}
