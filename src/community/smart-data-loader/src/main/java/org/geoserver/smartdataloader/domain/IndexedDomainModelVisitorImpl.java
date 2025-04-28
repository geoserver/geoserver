package org.geoserver.smartdataloader.domain;

import java.util.HashSet;
import java.util.Set;

public class IndexedDomainModelVisitorImpl extends DomainModelVisitorImpl implements IndexedDomainModelVisitor {

    protected final Set<Object> visitedEntities = new HashSet<>();

    @Override
    public boolean isVisited(Object object) {
        return visitedEntities.contains(object);
    }
}
