/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.geoserver.smartdataloader.domain.entities.DomainAttributeType;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;
import org.geoserver.smartdataloader.metadata.AttributeMetadata;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;

/**
 * Smart AppSchema model builder. Given a DomainModelConfig object and a DataStoreMetadata it allows
 * to get the Smart AppSchema model.
 */
public final class DomainModelBuilder {

    private final DataStoreMetadata dataStoreMetadata;
    private final DomainModelConfig domainModelConfig;

    private final Map<String, DomainEntity> domainEntitiesIndex = new HashMap<>();
    private final Set<String> visitedEntities = new HashSet<>();

    public DomainModelBuilder(
            DataStoreMetadata dataStoreMetadata, DomainModelConfig domainModelConfig) {
        this.dataStoreMetadata = dataStoreMetadata;
        this.domainModelConfig = domainModelConfig;
    }

    public DomainModel buildDomainModel() {
        EntityMetadata rootEntityMetadata =
                dataStoreMetadata.getEntityMetadata(domainModelConfig.getRootEntityName());
        if (rootEntityMetadata == null) {
            throw new RuntimeException(
                    "Root entity name '"
                            + domainModelConfig.getRootEntityName()
                            + "' does not exists!");
        }
        DomainEntity rootEntity = this.buildRootDomainEntity(rootEntityMetadata.getName());
        DomainModel dm = new DomainModel(this.dataStoreMetadata, rootEntity);
        return dm;
    }

    private DomainEntity buildRootDomainEntity(String entityName) {
        return buildDomainEntity(entityName, null);
    }

    private DomainEntity indexEntity(EntityMetadata entityMetadata) {
        // let's try to retrieve the domain entity
        DomainEntity entity = domainEntitiesIndex.get(entityMetadata.getName());
        if (entity == null) {
            // first time we are visiting this entity metadata so we need to build a domain entity
            entity = new DomainEntity();
            entity.setName(entityMetadata.getName());
            domainEntitiesIndex.put(entity.getName(), entity);
        } else {
            // we already have our entity
            return entity;
        }
        // we got our entity, it still an empty shell at this stage
        return entity;
    }

    private DomainEntity buildDomainEntity(String entityName, DomainRelation fromRelation) {
        if (visitedEntities.contains(entityName)) {
            // we are dealing with a cyclic dependency, we currently don't support this
            throw new RuntimeException(
                    "Cyclic dependency detected for entity '" + entityName + "'");
        }
        visitedEntities.add(entityName);
        // retrieve the metadata for our entity
        EntityMetadata entityMetadata = dataStoreMetadata.getEntityMetadata(entityName);
        if (entityMetadata == null) {
            // looks like there is not metadata for our entity, we are done
            throw new RuntimeException("Could not find metadata for entity '" + entityName + "'");
        }
        // let's try to retrieve the domain entity or create it if needed
        DomainEntity entity = indexEntity(entityMetadata);
        // let's add the relations of our entity
        entityMetadata
                .getRelations()
                .forEach(
                        relation -> {
                            if (fromRelation == null
                                    || !relation.participatesInRelation(
                                            fromRelation.getContainingEntity().getName())) {
                                DomainRelation domainRelation =
                                        buildDomainRelation(entity, relation, fromRelation);
                                entity.add(domainRelation);
                            }
                        });
        // let's add attributes of our entity, excluding all attributes that are foreign keys
        entityMetadata
                .getAttributes()
                .forEach(
                        attribute -> {
                            // exclude external attributes references
                            if (!attribute.isExternalReference()) {
                                DomainEntitySimpleAttribute domainAttribute =
                                        buildDomainEntitySimpleAttribute(attribute);
                                entity.add(domainAttribute);
                            }
                        });
        visitedEntities.remove(entityName);
        return entity;
    }

    private DomainRelation buildDomainRelation(
            DomainEntity containingDomainEntity,
            RelationMetadata relationMetadata,
            DomainRelation fromDomainRelation) {
        // retrieve the source and targeted attributes of the relation
        AttributeMetadata sourceAttribute = relationMetadata.getSourceAttribute();
        AttributeMetadata destinationAttribute = relationMetadata.getDestinationAttribute();
        if (destinationAttribute.getEntity().getName().equals(containingDomainEntity.getName())) {
            // the containing entity was actually the destination, we need to swap the attributes
            sourceAttribute = relationMetadata.getDestinationAttribute();
            destinationAttribute = relationMetadata.getSourceAttribute();
        }
        // let's build our domain relation for our containing entity
        DomainRelation domainRelation = new DomainRelation();
        // set the containing entity and attribute
        domainRelation.setContainingEntity(containingDomainEntity);
        domainRelation.setContainingKeyAttribute(buildDomainEntitySimpleAttribute(sourceAttribute));
        // set the destination entity and attribute
        DomainEntity destinationDomainEntity =
                buildDomainEntity(destinationAttribute.getEntity().getName(), domainRelation);
        domainRelation.setDestinationEntity(destinationDomainEntity);
        domainRelation.setDestinationKeyAttribute(
                buildDomainEntitySimpleAttribute(destinationAttribute));
        return domainRelation;
    }

    private DomainEntitySimpleAttribute buildDomainEntitySimpleAttribute(
            AttributeMetadata attributeMetadata) {
        DomainEntitySimpleAttribute domainAttribute = new DomainEntitySimpleAttribute();
        domainAttribute.setName(attributeMetadata.getName());
        String attribType = attributeMetadata.getType().toLowerCase();
        domainAttribute.setIdentifier(attributeMetadata.isIdentifier());
        // clean composed types to get only the type. ie. "public"."geometry" -> geometry
        String[] composedAttribType = attribType.split(Pattern.quote("."));
        if (composedAttribType.length == 2) {
            attribType = composedAttribType[1].substring(1, composedAttribType[1].length() - 1);
        }

        switch (attribType) {
            case "number":
            case "numeric":
            case "float8":
            case "float4":
            case "decimal":
                domainAttribute.setType(DomainAttributeType.NUMBER);
                break;
            case "serial":
            case "smallint":
            case "int4":
                domainAttribute.setType(DomainAttributeType.INT);
                break;
            case "bigint":
            case "bigserial":
                domainAttribute.setType(DomainAttributeType.INTEGER);
            case "text":
            case "varchar":
                domainAttribute.setType(DomainAttributeType.TEXT);
                break;
            case "time":
            case "date":
            case "timestamptz":
            case "interval":
            case "timestamp":
                domainAttribute.setType(DomainAttributeType.DATE);
                break;
            case "geometry":
            case "geography":
                domainAttribute.setType(DomainAttributeType.GEOMETRY);
                break;
            case "bool":
            case "boolean":
                domainAttribute.setType(DomainAttributeType.BOOLEAN);
                break;
            default:
                throw new RuntimeException(
                        String.format(
                                "Attribute type '%s' is unknown.",
                                attributeMetadata.getType().toLowerCase()));
        }
        return domainAttribute;
    }
}
