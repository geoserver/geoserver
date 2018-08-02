package com.boundlessgeo.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;


/**
 * Generalized model object for Add Features, Update Features, Delete Features, and Apply Edits operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditResults {
    public final Integer id;

    public final List<EditResult> addResults;
    public final List<EditResult> updateResults;
    public final List<EditResult> deleteResults;

    /**
     * @param id The id of the layer, or null. Only applicable for Apply Edits
     * @param addResults the add results, or null. Only applicable for Apply Edits and Add Features;
     * @param updateResults the update results, or null. Only applicable for Apply Edits and Update Features;
     * @param deleteResults the delete results, or null. Only applicable for Apply Edits and Delete Features;
     */
    public EditResults(Integer id, List<EditResult> addResults, List<EditResult> updateResults, List<EditResult> deleteResults) {
        this.id = id;
        this.addResults = addResults;
        this.updateResults = updateResults;
        this.deleteResults = deleteResults;
    }

    /**
     * @param addResults the add results, or null. Only applicable for Apply Edits (Layer) and Add Features;
     * @param updateResults the update results, or null. Only applicable for Apply Edits (Layer) and Update Features;
     * @param deleteResults the delete results, or null. Only applicable for Apply Edits (Layer) and Delete Features;
     */
    public EditResults(List<EditResult> addResults, List<EditResult> updateResults, List<EditResult> deleteResults) {
        this(null, addResults, updateResults, deleteResults);
    }
}
