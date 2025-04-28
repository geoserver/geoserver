package org.geoserver.smartdataloader.domain;

public interface IndexedDomainModelVisitor extends DomainModelVisitor {

    boolean isVisited(Object object);

    static boolean isIndexedAndVisited(DomainModelVisitor visitor, Object object) {
        return visitor instanceof IndexedDomainModelVisitor && ((IndexedDomainModelVisitor) visitor).isVisited(object);
    }
}
