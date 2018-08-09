package com.boundlessgeo.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;


/**
 * Generalized model object for Add Features, Update Features, Delete Features, and Apply Edits operations
 *
 * See https://developers.arcgis.com/rest/services-reference/results-returned-from-feature-service-edit-operations.htm
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditResults {
    public Integer id;
    public Long editMoment;

    public final List<EditResult> addResults;
    public final List<EditResult> updateResults;
    public final List<EditResult> deleteResults;

    /**
     * @param id The id of the layer, or null. Only applicable for Apply Edits
     * @param editMoment the timestamp the edit was applied
     * @param addResults the add results, or null. Only applicable for Apply Edits and Add Features;
     * @param updateResults the update results, or null. Only applicable for Apply Edits and Update Features;
     * @param deleteResults the delete results, or null. Only applicable for Apply Edits and Delete Features;
     */
    public EditResults(Integer id, Long editMoment, List<EditResult> addResults, List<EditResult> updateResults, List<EditResult> deleteResults) {
        this.id = id;
        this.editMoment = editMoment;
        this.addResults = addResults;
        this.updateResults = updateResults;
        this.deleteResults = deleteResults;
    }

    /**
     * @param id The id of the layer, or null. Only applicable for Apply Edits
     * @param addResults the add results, or null. Only applicable for Apply Edits and Add Features;
     * @param updateResults the update results, or null. Only applicable for Apply Edits and Update Features;
     * @param deleteResults the delete results, or null. Only applicable for Apply Edits and Delete Features;
     */
    public EditResults(Integer id, List<EditResult> addResults, List<EditResult> updateResults, List<EditResult> deleteResults) {
        this(id, null, addResults, updateResults, deleteResults);
    }

    /**
     * @param addResults the add results, or null. Only applicable for Apply Edits (Layer) and Add Features;
     * @param updateResults the update results, or null. Only applicable for Apply Edits (Layer) and Update Features;
     * @param deleteResults the delete results, or null. Only applicable for Apply Edits (Layer) and Delete Features;
     */
    public EditResults(List<EditResult> addResults, List<EditResult> updateResults, List<EditResult> deleteResults) {
        this(null, addResults, updateResults, deleteResults);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEditMoment(Long editMoment) {
        this.editMoment = editMoment;
    }
}
