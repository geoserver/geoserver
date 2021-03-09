/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.smartdataloader.domain.DomainModelBuilder;
import org.geoserver.smartdataloader.domain.DomainModelConfig;
import org.geoserver.smartdataloader.domain.DomainModelVisitorImpl;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;

/**
 * DomainModelVisitor that based on list of exclusions tails a domainmodel and returns a new one
 * with removed domainmodel objects.
 */
public class ExclusionsDomainModelVisitor extends DomainModelVisitorImpl {

    private List<String> exclusions;
    private DomainEntity currentEntity;
    private Map<String, DomainRelation> relationsToRemove = new HashMap<>();
    private Map<DomainEntitySimpleAttribute, DomainEntity> attributesToRemove = new HashMap<>();

    /**
     * Returns a DomainModel, clone of the original, that excludes domain objects listed in
     * parameter list.
     *
     * @param model The original domain model.
     * @param excludedObjects List of domain objects to exclude (entities, attributes and
     *     relations).
     * @return Cloned and reduced domainmodel
     */
    public static DomainModel buildDomainModel(DomainModel model, List<String> excludedObjects) {
        String rootEntityName = model.getRootEntity().getName();
        // if root node was excluded, then domainmodel is not build, and will return null
        if (excludedObjects.contains(rootEntityName)) {
            throw new RuntimeException("Root entity of domainmodel is in exclusion list!");
        }
        DomainModelConfig domainModelConfig = new DomainModelConfig();
        domainModelConfig.setRootEntityName(rootEntityName);
        DomainModelBuilder dmb =
                new DomainModelBuilder(model.getDataStoreMetadata(), domainModelConfig);
        DomainModel clonedDomainModel = dmb.buildDomainModel();
        ExclusionsDomainModelVisitor dmv = new ExclusionsDomainModelVisitor(excludedObjects);
        clonedDomainModel.accept(dmv);
        Map<DomainEntitySimpleAttribute, DomainEntity> attributesToRemove = dmv.attributesToRemove;
        for (DomainEntitySimpleAttribute key : attributesToRemove.keySet()) {
            DomainEntity entity = attributesToRemove.get(key);
            entity.getAttributes().remove(key);
        }
        Map<String, DomainRelation> relationsToRemove = dmv.relationsToRemove;
        for (String key : relationsToRemove.keySet()) {
            DomainRelation relation = relationsToRemove.get(key);
            relation.getContainingEntity().getRelations().remove(relation);
        }
        return clonedDomainModel;
    }

    public ExclusionsDomainModelVisitor(List<String> excludedObjects) {
        exclusions = excludedObjects;
    }

    @Override
    public void visitDomainModel(DomainModel model) {
        String rootEntityName = model.getRootEntity().getName();
        // if root node was excluded, then domainmodel is not build, and will return null
        if (exclusions.contains(rootEntityName)) {
            throw new RuntimeException("Root entity of domainmodel is in exclusion list!");
        }
    }

    @Override
    public void visitDomainRootEntity(DomainEntity entity) {
        currentEntity = entity;
    }

    @Override
    public void visitDomainChainedEntity(DomainEntity entity) {
        currentEntity = entity;
    }

    @Override
    public void visitDomainEntitySimpleAttribute(DomainEntitySimpleAttribute attribute) {
        String domainObjectName = currentEntity.getName() + "." + attribute.getName();
        // look for the attribute in the clone entity and remove it
        if (exclusions.contains(domainObjectName)) {
            attributesToRemove.put(attribute, currentEntity);
        }
    }

    @Override
    public void visitDomainRelation(DomainRelation relation) {
        String domainObjectName =
                relation.getContainingEntity().getName()
                        + "."
                        + relation.getDestinationEntity().getName();
        // if relation is in exclusion list, remove it from cloneEntity and add entity to list of
        // removed entities
        if (exclusions.contains(domainObjectName)) {
            relationsToRemove.put(domainObjectName, relation);
        }
    }
}
