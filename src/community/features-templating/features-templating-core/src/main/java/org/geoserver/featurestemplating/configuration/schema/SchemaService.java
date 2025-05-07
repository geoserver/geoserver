/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

/** This class provides the business logic to handle save, update and delete operation on a template. */
public class SchemaService {

    private SchemaLoader loader;
    private SchemaFileManager schemaFileManager;
    private SchemaInfoDAO schemaInfoDAO;

    public SchemaService() {
        this.loader = SchemaLoader.get();
        this.schemaFileManager = SchemaFileManager.get();
        this.schemaInfoDAO = SchemaInfoDAO.get();
    }

    /**
     * Save or update a Template. Clean the cache from the previous template if necessary.
     *
     * @param schemaInfo the Template Info to save or update.
     * @param rawTemplate the raw Template to save or update.
     */
    public void saveOrUpdate(SchemaInfo schemaInfo, String rawTemplate) {
        schemaFileManager.saveTemplateFile(schemaInfo, rawTemplate);
        SchemaInfo current = schemaInfoDAO.findById(schemaInfo.getIdentifier());
        if (current != null && !current.getFullName().equals(schemaInfo.getFullName())) {
            if (schemaFileManager.delete(current)) {
                loader.removeAllWithIdentifier(schemaInfo.getIdentifier());
            }
        }
        schemaInfoDAO.saveOrUpdate(schemaInfo);
    }

    /**
     * Delete a Template and removes it from the cache.
     *
     * @param schemaInfo the Template Info object to delete.
     */
    public void delete(SchemaInfo schemaInfo) {
        schemaFileManager.delete(schemaInfo);
        loader.removeAllWithIdentifier(schemaInfo.getIdentifier());
        schemaInfoDAO.delete(schemaInfo);
    }
}
