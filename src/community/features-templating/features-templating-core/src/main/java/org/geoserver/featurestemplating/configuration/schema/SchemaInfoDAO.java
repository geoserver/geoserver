/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;

/** Base interface for SchemaInfo Data Access. */
public interface SchemaInfoDAO {

    static SchemaInfoDAOImpl get() {
        return GeoServerExtensions.bean(SchemaInfoDAOImpl.class);
    }

    public static final String SCHEMA_DIR = "features-templating/schemas";

    /** @return all the saved schema info. */
    public List<SchemaInfo> findAll();

    /**
     * Find all the schema info that can be used for the FeatureTypeInfo. It means that all the schemas that are in the
     * global directory plus all the schemas in the workspace directory to which the FeatureTypeInfo belongs plus all
     * the schemas in the FeatureTypeInfo directory will be returned.
     *
     * @param featureTypeInfo
     * @return
     */
    public List<SchemaInfo> findByFeatureTypeInfo(FeatureTypeInfo featureTypeInfo);

    /**
     * @param id the identifier of the schema info to retrieve.
     * @return the SchemaInfo object.
     */
    public SchemaInfo findById(String id);

    /**
     * Save or update the schema info.
     *
     * @param schemaData the schema to save or update.
     * @return the schema saved or updated.
     */
    public SchemaInfo saveOrUpdate(SchemaInfo schemaData);

    /**
     * Delete all the schema info in the list.
     *
     * @param schemaInfos list of schema info to delete.
     */
    public void delete(List<SchemaInfo> schemaInfos);

    /** @param schemaData the schema info to delete. */
    public void delete(SchemaInfo schemaData);

    /** Deletes all the schema info. */
    public void deleteAll();

    /**
     * Add a listener.
     *
     * @param listener the listener to add.
     */
    public void addSchemaListener(SchemaDAOListener listener);

    /**
     * Find a SchemaInfo from full name. By full name is meant the name of the schema file preceded by the workspace
     * name and the feature type name if defined for the schema. Format for a full name is like the following:
     * schemaName or workspaceName:schemaName or workspaceName:featureTypeName:schemaName
     *
     * @param fullName the full name of the schema.
     * @return the corresponding SchemaInfo.
     */
    public SchemaInfo findByFullName(String fullName);
}
