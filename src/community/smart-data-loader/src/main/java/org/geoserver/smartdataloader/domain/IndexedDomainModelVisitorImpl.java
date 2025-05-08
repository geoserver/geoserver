/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation for a domain model visitor that keeps track of visited entities.
 *
 * <p>This class extends the {@link DomainModelVisitorImpl} class and implements the {@link IndexedDomainModelVisitor}
 * interface. It maintains a set of visited entities to avoid processing the same entity multiple times.
 */
public class IndexedDomainModelVisitorImpl extends DomainModelVisitorImpl implements IndexedDomainModelVisitor {

    protected final Set<Object> visitedEntities = new HashSet<>();

    @Override
    public boolean isVisited(Object object) {
        return visitedEntities.contains(object);
    }
}
