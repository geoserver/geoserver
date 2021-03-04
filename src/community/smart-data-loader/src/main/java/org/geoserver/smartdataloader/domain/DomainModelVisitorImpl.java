/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain;

import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;

/** Default implementation for a domain model visitor. */
public class DomainModelVisitorImpl implements DomainModelVisitor {

    @Override
    public void visitDataStoreMetadata(DataStoreMetadata dataStoreMetadata) {}

    @Override
    public void visitDomainModel(DomainModel model) {}

    @Override
    public void visitDomainRootEntity(DomainEntity entity) {}

    @Override
    public void visitDomainChainedEntity(DomainEntity entity) {}

    @Override
    public void visitDomainEntitySimpleAttribute(DomainEntitySimpleAttribute attribute) {}

    @Override
    public void visitDomainRelation(DomainRelation relation) {}
}
