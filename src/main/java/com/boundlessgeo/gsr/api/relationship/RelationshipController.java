package com.boundlessgeo.gsr.api.relationship;

import com.boundlessgeo.gsr.model.relationship.RelationshipClass;
import com.boundlessgeo.gsr.model.relationship.RelationshipClassErrorMessage;
import com.boundlessgeo.gsr.translate.relationship.RelationshipDAO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * Controller for the Relationship Service
 */
@RestController
@RequestMapping(path = "/gsr/relationships/{workspaceName}", produces = MediaType.APPLICATION_JSON_VALUE)
public class RelationshipController {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(RelationshipController.class);

    private final RelationshipDAO relationshipDAO;
    public RelationshipController(RelationshipDAO relationshipDAO){
        this.relationshipDAO = relationshipDAO;
    }

    /**
     * Get RelationshipClass By ID
     * @param workspaceName Workspace name
     * @param relationshipId Relationship Id
     * @return
     */
    @RequestMapping(path="/{relationshipId}")
    public RelationshipClass getRelationshipClassByWorkspaceAndId(@PathVariable String workspaceName,@PathVariable Long relationshipId){
        return relationshipDAO.getRelationshipClass(workspaceName,relationshipId);
    }

    /**
     * Get RelationshipClass by the full id(OriginTable,DestinationTable,OriginPrimaryKey,OriginForeignKey) and the workspace name
     * @param workspaceName Workspace name
     * @param originTable The table or feature class that is associated to the destination table.
     * @param destinationTable The table that is associated to the origin table.
     * @param originPrimaryKey The field in the origin table that links it to the Origin Foreign Key field in the relationship class table.
     * @param originForeignKey The field in the relationship class table that links it to the Origin Primary Key field in the origin table.
     * @return
     */
    @RequestMapping(path="/originTable/{originTable}/destinationTable/{destinationTable}/originPrimaryKey/{originPrimaryKey}/originForeignKey/{originForeignKey}")
    public RelationshipClass getRelationshipClassByWorkspaceAndOriginAndDestinationAndKeys(@PathVariable String workspaceName,@PathVariable String originTable,
                                                                                           @PathVariable String destinationTable,@PathVariable String originPrimaryKey,
                                                                                           @PathVariable String originForeignKey){
        return relationshipDAO.getRelationshipClass(workspaceName,originTable,destinationTable,originPrimaryKey,originForeignKey);
    }

    /**
     * Get List of RelationshipClass by the workspace name and the origin table name
     * @param workspaceName Workspace name
     * @param originTable The table or feature class that is associated to the destination table.
     * @return
     */
    @RequestMapping(path="/originTable/{originTable}")
    public List<RelationshipClass> getRelationshipClassByWorkspaceAndOrigin(@PathVariable String workspaceName, @PathVariable String originTable){
        return relationshipDAO.getRelationshipClassesByWorkspaceAndOriginTable(workspaceName,originTable);
    }

    /**
     * Get List of RelationshipClass by the workspace name and the destination table name
     * @param workspaceName Workspace name
     * @param destinationTable The table or feature class that is associated to the origin table.
     * @return
     */
    @RequestMapping(path="/destinationTable/{destinationTable}")
    public List<RelationshipClass> getRelationshipClassByWorkspaceAndDestination(@PathVariable String workspaceName, @PathVariable String destinationTable){
        return relationshipDAO.getRelationshipClassesByWorkspaceAndDestinationTable(workspaceName,destinationTable);
    }

    /**
     * Create a new relationship class
     * @param workspaceName Workspace name
     * @param relationshipClass RelationshipClass object (all applicable fields should be populated, id fields(OriginTable,DestinationTable,OriginPrimaryKey,OriginForeignKey) are required. If relationshipId is not populated or is set to zero it will be replaced by a numerical hash computed from the composite id.
     * @return
     */
    @PostMapping
    public RelationshipClass createRelationshipClass(@PathVariable String workspaceName, @RequestBody RelationshipClass relationshipClass) {

        if (relationshipDAO.fieldTypeMatches(relationshipClass.getOriginTable(), relationshipClass.getOriginPrimaryKey(), relationshipClass.getDestinationTable(), relationshipClass.getOriginForeignKey())) {
            return relationshipDAO.upsertRelationshipClass(workspaceName, relationshipClass);
        } else {
            LOGGER.info("Unable to match the source and destination fields " + relationshipClass.getOriginTable() + ":" + relationshipClass.getOriginPrimaryKey() + " " + relationshipClass.getDestinationTable() + ":" + relationshipClass.getOriginForeignKey());
            throw new IllegalArgumentException(relationshipClass.toString());
        }
    }

    /**
     * Update an existing relationshipClass in the workspace
     * @param workspaceName Workspace name
     * @param relationshipClass RelationshipClass object (all applicable fields should be populated, id fields(OriginTable,DestinationTable,OriginPrimaryKey,OriginForeignKey) are required.  If the id fields do not match an existing entry, a new entry will be created.
     * @return
     */
    @PutMapping
    public RelationshipClass updateRelationshipClass(@PathVariable String workspaceName, @RequestBody RelationshipClass relationshipClass) {
        if (relationshipDAO.fieldTypeMatches(relationshipClass.getOriginTable(), relationshipClass.getOriginPrimaryKey(), relationshipClass.getDestinationTable(), relationshipClass.getOriginForeignKey())) {
            return relationshipDAO.upsertRelationshipClass(workspaceName, relationshipClass);
        } else {
            LOGGER.info("Unable to match the source and destination fields " + relationshipClass.getOriginTable() + ":" + relationshipClass.getOriginPrimaryKey() + " " + relationshipClass.getDestinationTable() + ":" + relationshipClass.getOriginForeignKey());
            throw new IllegalArgumentException(relationshipClass.toString());
        }
    }

    /**
     * Delete a relationship class in the workspace
     * @param workspaceName Workspace name
     * @param relationshipClass The relationship class to delete.  At a minimum the id fields(OriginTable,DestinationTable,OriginPrimaryKey,OriginForeignKey) need to be populated.
     * @return
     */
    @DeleteMapping
    public Boolean deleteRelationshipClass(@PathVariable String workspaceName, @RequestBody RelationshipClass relationshipClass){
        return relationshipDAO.deleteRelationshipClass(workspaceName,relationshipClass.getOriginTable(),relationshipClass.getDestinationTable(),relationshipClass.getOriginPrimaryKey(),relationshipClass.getOriginForeignKey());
    }

    /**
     * Delete RelationshipClass from Workspace using RelationshipId
     * @param workspaceName Workspace Name
     * @param relationshipId Relationship ID
     * @return
     */
    @RequestMapping(path="/delete/{relationshipId}")
    public Boolean deleteRelationshipClass(@PathVariable String workspaceName, @PathVariable Long relationshipId){
        return relationshipDAO.deleteRelationshipClass(workspaceName,relationshipId);
    }

    /**
     * Method to return an error object if the primary and foreign keys do not exist or do not have matching types.
     * @param ex
     * @return
     */
    @ExceptionHandler({ IllegalArgumentException.class })
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(IllegalArgumentException ex) {
        RelationshipClassErrorMessage relationshipClassErrorMessage = new RelationshipClassErrorMessage();
        relationshipClassErrorMessage.setRelationshipClass(ex.getMessage());
        relationshipClassErrorMessage.setErrorMessage("Unable to match the source and destination fields");
        return new ResponseEntity<Object>(relationshipClassErrorMessage, HttpStatus.BAD_REQUEST);
    }

}
