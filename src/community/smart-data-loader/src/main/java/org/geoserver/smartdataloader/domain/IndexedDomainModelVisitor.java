/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain;

/**
 * Interface for a domain model visitor that keeps track of visited entities.
 *
 * <p>This interface extends the {@link DomainModelVisitor} interface and adds a method to check if an object has been
 * visited.
 */
public interface IndexedDomainModelVisitor extends DomainModelVisitor {

    /**
     * Checks if the given object has been visited.
     *
     * @param object the object to check
     * @return true if the object has been visited, false otherwise
     */
    boolean isVisited(Object object);

    /**
     * Checks if the given object has been visited by the specified visitor, only if the visitor is an instance of
     * IndexedDomainModelVisitor. It will return false if the visitor is not an instance of IndexedDomainModelVisitor.
     *
     * @param visitor the visitor to check
     * @param object the object to check
     * @return true if the object has been visited by the visitor, false otherwise
     */
    static boolean isIndexedAndVisited(DomainModelVisitor visitor, Object object) {
        return visitor instanceof IndexedDomainModelVisitor && ((IndexedDomainModelVisitor) visitor).isVisited(object);
    }
}
