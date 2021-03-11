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

/**
 * Smart AppSchema model objects visitor interface. Defined with the purpose of accessing elements
 * on model and visiting them in order to build output structure data.
 */
public interface DomainModelVisitor {

    void visitDataStoreMetadata(DataStoreMetadata dataStoreMetadata);

    void visitDomainModel(DomainModel model);

    void visitDomainRootEntity(DomainEntity entity);

    void visitDomainChainedEntity(DomainEntity entity);

    void visitDomainEntitySimpleAttribute(DomainEntitySimpleAttribute attribute);

    void visitDomainRelation(DomainRelation relation);
}
