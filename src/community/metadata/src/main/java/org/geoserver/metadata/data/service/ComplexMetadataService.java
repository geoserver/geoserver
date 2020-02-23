/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.util.List;
import java.util.Map;
import org.geoserver.metadata.data.model.ComplexMetadataMap;

/**
 * TODO consolidate methods.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public interface ComplexMetadataService {

    /**
     * Initialize a new map, making sure that there are null values to make multi-valued complex
     * attributes match sizes
     */
    default void init(ComplexMetadataMap map) {
        init(map, null);
    }

    /**
     * Initialize a new submap, making sure that there are null values to make multi-valued complex
     * attributes match sizes
     */
    void init(ComplexMetadataMap subMap, String typeName);

    /**
     * The values in the template are applied in reverse order, i.e. the first child has the highest
     * priority.
     */
    void merge(
            ComplexMetadataMap destination,
            List<ComplexMetadataMap> sources,
            Map<String, List<Integer>> derivedAtts);

    /**
     * Copy from one submap to another
     *
     * @param source source submap
     * @param dest target submap
     * @param typeConfiguration the configuration of the type
     */
    default void copy(ComplexMetadataMap source, ComplexMetadataMap dest, String typeName) {
        copy(source, dest, typeName, false);
    }

    /**
     * Copy from one submap to another
     *
     * @param source source submap
     * @param dest target submap
     * @param typeConfiguration the configuration of the type
     * @param ingoreUUID whether to ignore UUID's
     */
    void copy(
            ComplexMetadataMap source,
            ComplexMetadataMap dest,
            String typeName,
            boolean ignoreUUID);

    /**
     * Tests if two submaps are equal to each other
     *
     * @param map the map
     * @param other the other map
     * @param typeName the type
     * @return whether they are equal
     */
    boolean equals(ComplexMetadataMap map, ComplexMetadataMap other, String typeName);

    /** Fill derived attributes. */
    void derive(ComplexMetadataMap map);

    /**
     * Clean the map of all rubbish metadata (not according to config)
     *
     * @param map the map
     */
    void clean(ComplexMetadataMap map);
}
