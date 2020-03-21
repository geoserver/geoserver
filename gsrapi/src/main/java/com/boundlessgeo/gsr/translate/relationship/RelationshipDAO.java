package com.boundlessgeo.gsr.translate.relationship;

import com.boundlessgeo.gsr.model.relationship.RelationshipClass;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

@Service
public class RelationshipDAO {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(RelationshipDAO.class);
    public static final String RELATIONSHIP_CLASS_KEY = "gsr.relationshipClasses";

    private final GeoServer geoServer;
    private final Catalog catalog;

    public RelationshipDAO(GeoServer geoServer){
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
    }

    /**
     * @see com.boundlessgeo.gsr.api.relationship.RelationshipController#getRelationshipClassByWorkspaceAndOriginAndDestinationAndKeys(String, String, String, String, String)
     * @param workspaceName
     * @param originTable
     * @param destinationTable
     * @param originPrimaryKey
     * @param originForeignKey
     * @return
     */
    public RelationshipClass getRelationshipClass(String workspaceName,String originTable,String destinationTable,String originPrimaryKey, String originForeignKey){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        RelationshipClass searchRelationshipClass = buildPartialRelationshipClass(workspaceName, originTable, destinationTable, originPrimaryKey, originForeignKey);
        Iterator<RelationshipClass> it = relationshipClasses.iterator();
        while(it.hasNext()){
            RelationshipClass relationshipClass = it.next();
            if(relationshipClass.equals(searchRelationshipClass)){
                return relationshipClass;
            }
        }
        return null;

    }

    /**
     * @see com.boundlessgeo.gsr.api.relationship.RelationshipController#getRelationshipClassByWorkspaceAndId(String, Long)
     * @param workspaceName
     * @param relationshipId
     * @return
     */
    public RelationshipClass getRelationshipClass(String workspaceName,Long relationshipId){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        Iterator<RelationshipClass> it = relationshipClasses.iterator();
        while(it.hasNext()){
            RelationshipClass relationshipClass = it.next();
            if(relationshipClass.getRelationshipId().equals(relationshipId)){
                return relationshipClass;
            }
        }
        return null;
    }

    /**
     * Returns all relationship classes associated with the workspace
     * @param workspaceName workspace name
     * @return
     */
    public List<RelationshipClass>getAllRelationshipClassesByWorkspace(String workspaceName){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        if(relationshipClasses!=null) {
            return new ArrayList(relationshipClasses);
        }else{
            return new ArrayList<>();
        }
    }

    /**
     * @see com.boundlessgeo.gsr.api.relationship.RelationshipController#getRelationshipClassByWorkspaceAndOrigin(String, String)
     * @param workspaceName
     * @param originTable
     * @return
     */
    public List<RelationshipClass> getRelationshipClassesByWorkspaceAndOriginTable(String workspaceName, String originTable){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        List<RelationshipClass> relationshipClassList = new ArrayList<>();
        Iterator<RelationshipClass> it = relationshipClasses.iterator();
        while(it.hasNext()){
            RelationshipClass relationshipClass = it.next();
            if(relationshipClass.getWorkspaceName().equals(workspaceName)&&relationshipClass.getOriginTable().equals(originTable)){
                relationshipClassList.add(relationshipClass);
            }
        }
        return relationshipClassList;
    }

    /**
     * @see com.boundlessgeo.gsr.api.relationship.RelationshipController#getRelationshipClassByWorkspaceAndDestination(String, String)
     * @param workspaceName
     * @param destinationTable
     * @return
     */
    public List<RelationshipClass> getRelationshipClassesByWorkspaceAndDestinationTable(String workspaceName, String destinationTable){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        List<RelationshipClass> relationshipClassList = new ArrayList<>();
        Iterator<RelationshipClass> it = relationshipClasses.iterator();
        while(it.hasNext()){
            RelationshipClass relationshipClass = it.next();
            if(relationshipClass.getWorkspaceName().equals(workspaceName)&&relationshipClass.getDestinationTable().equals(destinationTable)){
                relationshipClassList.add(relationshipClass);
            }
        }
        return relationshipClassList;
    }

    /**
     * @see com.boundlessgeo.gsr.api.relationship.RelationshipController#createRelationshipClass(String, RelationshipClass)
     * @param workspaceName
     * @param relationshipClass
     * @return
     */
    public RelationshipClass upsertRelationshipClass(String workspaceName, RelationshipClass relationshipClass){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        if(relationshipClass.getWorkspaceName()==null||!relationshipClass.getWorkspaceName().equals(workspaceName)){
            relationshipClass.setWorkspaceName(workspaceName);
        }
        Boolean alreadyThere = relationshipClasses.contains(relationshipClass);
        if(alreadyThere){
            LOGGER.info("The Relationship Class being inserted is going to overwrite an existing one with the same source table name, destination table name, primary key and foreign key: "+relationshipClass.getOriginTable()+","+relationshipClass.getDestinationTable()+","+relationshipClass.getOriginPrimaryKey()+relationshipClass.getOriginForeignKey());
            relationshipClasses.remove(relationshipClass);
            relationshipClasses.add(relationshipClass);
        }else{
            if(relationshipClass.getRelationshipId()==null||relationshipClass.getRelationshipId()==0){
                relationshipClass.setRelationshipId(generateLongHashFromWorkspaceSourceTableAndKeys(relationshipClass.getWorkspaceName(),relationshipClass.getOriginTable(),
                        relationshipClass.getDestinationTable(),relationshipClass.getOriginPrimaryKey(),relationshipClass.getOriginForeignKey()));
            }
            relationshipClasses.add(relationshipClass);
        }
        saveWorkspaceChanges(workspaceName,relationshipClasses);
        return relationshipClass;
    }

    public static Long generateLongHashFromWorkspaceSourceTableAndKeys(String workspaceName,String originTable,String destinationTable,String originPrimaryKey,String originForeignKey){
        String key = workspaceName+originTable+destinationTable+originPrimaryKey+originForeignKey;
        return UUID.nameUUIDFromBytes(key.getBytes()).getMostSignificantBits();
    }

    /**
     * Confirm that the layer/table name and field name combination exists in the catalog
     * @param tableName Table/Layer name
     * @param fieldName Fieldname
     * @return
     */
    public Boolean fieldExists(String tableName, String fieldName){
        LayerInfo layerInfo = catalog.getLayerByName(tableName);
        if(layerInfo!=null){
            if (layerInfo.getResource() instanceof FeatureTypeInfo) {
                List<AttributeTypeInfo> attributeTypeInfos = getAttributeTypeInfos(layerInfo);
                for(AttributeTypeInfo attributeTypeInfo:attributeTypeInfos){
                    if(attributeTypeInfo.getName().equals(fieldName)){
                        return true;
                    }
                }
                LOGGER.info("Layer with name " + tableName+ " was found but the attribute "+fieldName+" was not");
                return false;
            }else{
                LOGGER.info("Layer with name " + tableName+" is not a FeatureLayer");
                return false;
            }
        }else{
            LOGGER.info("Layer with name "+tableName+" not found in catalog");
            return false;
        }

    }

    /**
     * Get detailed metadata object associated with the field name for a given Layer/table
     * @param tableName Layer/table name
     * @param fieldName Fieldname
     * @return
     */
    public AttributeTypeInfo getAttributeTypeInfo(String tableName, String fieldName){
        LayerInfo layerInfo = catalog.getLayerByName(tableName);
        if(layerInfo!=null){
            if (layerInfo.getResource() instanceof FeatureTypeInfo) {
                List<AttributeTypeInfo> attributeTypeInfos = getAttributeTypeInfos(layerInfo);
                for(AttributeTypeInfo attributeTypeInfo:attributeTypeInfos){
                    if(attributeTypeInfo.getName().equals(fieldName)){
                        return attributeTypeInfo;
                    }
                }
                LOGGER.info("Layer with name " + tableName+ " was found but the attribute "+fieldName+" was not");
                return null;
            }else{
                LOGGER.info("Layer with name " + tableName+" is not a FeatureLayer");
                return null;
            }
        }else{
            LOGGER.info("Layer with name "+tableName+" not found in catalog");
            return null;
        }
    }

    /**
     * Validate that source and destination fields exist in the layers/tables and that their data types match
     * @param sourceTableName Source layer/table name
     * @param sourceFieldName Source field name
     * @param destinationTableName Destination layer/table name
     * @param destinationFieldName Destination field name
     * @return
     */
    public Boolean fieldTypeMatches(String sourceTableName, String sourceFieldName, String destinationTableName, String destinationFieldName){
        AttributeTypeInfo sourceAttributeTypeInfo = getAttributeTypeInfo(sourceTableName,sourceFieldName);
        AttributeTypeInfo destinationAttributeTypeInfo = getAttributeTypeInfo(destinationTableName,destinationFieldName);
        if(sourceAttributeTypeInfo!=null){
            if(destinationAttributeTypeInfo!=null){
                return sourceAttributeTypeInfo.getBinding().equals(destinationAttributeTypeInfo.getBinding());
            }else{
                LOGGER.info("Unable to get the field type for " + destinationTableName + ":"+destinationFieldName);
                return false;
            }
        }else{
            LOGGER.info("Unable to get the field type for " + sourceTableName + ":"+sourceFieldName);
            return false;
        }
    }

    /**
     * Confirm that the attribute/field metadata matches the passed in Java Class/Type
     * @param attributeTypeInfo Field metadata
     * @param matchClass Java Class/Type
     * @return
     */
    public Boolean fieldTypeMatches(AttributeTypeInfo attributeTypeInfo, Class matchClass){
            Class attributeClassBinding = attributeTypeInfo.getBinding();
            return attributeClassBinding.equals(matchClass);
    }

    /**
     * Get the field/attribute metadatas for a given Layer/table
     * @param layerInfo Layer/Table metadata
     * @return
     */
    public List<AttributeTypeInfo> getAttributeTypeInfos(LayerInfo layerInfo) {
        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layerInfo.getResource();
        List<AttributeTypeInfo> attributeTypeInfos = null;
        try {
            attributeTypeInfos = featureTypeInfo.attributes();
        }catch(IOException ioe){
            LOGGER.info("Layer with name " + featureTypeInfo.getName()+ " was found but there was an IOException when retrieving the attributes.");

        }
        return attributeTypeInfos;
    }

    /**
     * @see com.boundlessgeo.gsr.api.relationship.RelationshipController#deleteRelationshipClass(String, RelationshipClass)
     * @param workspaceName
     * @param originTable
     * @param destinationTable
     * @param originPrimaryKey
     * @param originForeignKey
     * @return
     */
    public Boolean deleteRelationshipClass(String workspaceName,String originTable,String destinationTable,String originPrimaryKey, String originForeignKey){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        RelationshipClass searchRelationshipClass = buildPartialRelationshipClass(workspaceName, originTable, destinationTable, originPrimaryKey, originForeignKey);
        Boolean removed = relationshipClasses.remove(searchRelationshipClass);
        saveWorkspaceChanges(workspaceName,relationshipClasses);
        return removed;
    }

    /**
     * @see com.boundlessgeo.gsr.api.relationship.RelationshipController#deleteRelationshipClass(String, Long)
     * @param workspaceName
     * @param relationshipId
     * @return
     */
    public Boolean deleteRelationshipClass(String workspaceName,Long relationshipId){
        HashSet<RelationshipClass> relationshipClasses = getRelationshipClasses(workspaceName);
        RelationshipClass searchRelationshipClass = getRelationshipClass(workspaceName,relationshipId);
        Boolean removed = relationshipClasses.remove(searchRelationshipClass);
        saveWorkspaceChanges(workspaceName,relationshipClasses);
        return removed;
    }

    /**
     * Build a RelationshipClass with only the id fields.  Useful for some lookups.
     * @param workspaceName Workspace name
     * @param originTable Origin table/layer
     * @param destinationTable Destination table/layer
     * @param originPrimaryKey Origin primary key
     * @param originForeignKey Origin foreign key
     * @return
     */
    public RelationshipClass buildPartialRelationshipClass(String workspaceName, String originTable, String destinationTable, String originPrimaryKey, String originForeignKey) {
        RelationshipClass searchRelationshipClass = new RelationshipClass();
        searchRelationshipClass.setWorkspaceName(workspaceName);
        searchRelationshipClass.setOriginTable(originTable);
        searchRelationshipClass.setDestinationTable(destinationTable);
        searchRelationshipClass.setOriginPrimaryKey(originPrimaryKey);
        searchRelationshipClass.setOriginForeignKey(originForeignKey);
        return searchRelationshipClass;
    }

    /**
     * Retrieve the RelationshipClass Set for a workspace
     * @param workspaceName Workspace name
     * @return
     */
    public HashSet<RelationshipClass> getRelationshipClasses(String workspaceName) {
        HashSet<RelationshipClass> relationshipClasses = null;
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspaceName);
        MetadataMap metadataMap = workspaceInfo.getMetadata();
        Serializable metadataRelationshipClasses = metadataMap.get(RELATIONSHIP_CLASS_KEY);
        if(metadataRelationshipClasses!=null){
            relationshipClasses = (HashSet<RelationshipClass>)metadataRelationshipClasses;
        }else{
            relationshipClasses = new HashSet<RelationshipClass>();
            metadataMap.put(RELATIONSHIP_CLASS_KEY,relationshipClasses);
        }
        return relationshipClasses;
    }

    /**
     * Required call to store any changes back to the workspace object in the catalog
     * @param workspaceName Workspace name
     * @param relationshipClasses Set of RelationshipClasses that contain the changes
     */
    public void saveWorkspaceChanges(String workspaceName, HashSet<RelationshipClass> relationshipClasses){
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspaceName);
        workspaceInfo.getMetadata().put(RELATIONSHIP_CLASS_KEY,relationshipClasses);
        catalog.save(workspaceInfo);
    }




}
