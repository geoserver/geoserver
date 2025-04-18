/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.geoparquet;

import static org.geotools.data.geoparquet.GeoParquetDataStoreFactory.MAX_HIVE_DEPTH;
import static org.geotools.data.geoparquet.GeoParquetDataStoreFactory.URI_PARAM;

import java.util.Map;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.form.ConverterModel;
import org.geoserver.web.wicket.form.NumberParamPanel;
import org.geoserver.web.wicket.form.URIParamPanel;
import org.geotools.api.data.DataAccessFactory.Param;

/**
 * Specific edit panel for GeoParquet data stores.
 *
 * <p>This panel provides a user interface for configuring GeoParquet datastores in the GeoServer admin UI. It extends
 * {@link StoreEditPanel} to create a form with fields specific to the GeoParquet format's connection parameters.
 *
 * <p>The panel includes configuration for:
 *
 * <ul>
 *   <li>URI input with support for local files, HTTP/HTTPS, S3, and glob patterns
 *   <li>Hive partitioning depth control for partitioned datasets
 * </ul>
 *
 * @see org.geotools.data.geoparquet.GeoParquetDataStoreFactory
 */
@SuppressWarnings("serial")
public class GeoParquetDataStoreEditPanel extends StoreEditPanel {

    /**
     * Creates a new GeoParquet-specific parameters panel with a list of input fields matching the {@link Param}s for
     * the factory.
     *
     * @param componentId the id for this component instance
     * @param storeEditForm the form being built by the calling class, whose model is the {@link DataStoreInfo} being
     *     edited
     */
    public GeoParquetDataStoreEditPanel(
            final String componentId, @SuppressWarnings("rawtypes") final Form storeEditForm) {
        super(componentId, storeEditForm);

        @SuppressWarnings("unchecked")
        final IModel<DataStoreInfo> model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, Object>> paramsModel = new PropertyModel<>(model, "connectionParameters");

        add(buildUriParamPanel(paramsModel));

        add(buildMaxHiveDepth(paramsModel));
    }

    /**
     * Creates a number field panel for configuring the maximum depth of Hive partitioning.
     *
     * <p>Hive partitioning is a technique used to organize data in a hierarchical folder structure where each level
     * represents a partition dimension (e.g., year=2023/month=01/day=15). This parameter controls how deep the
     * partitioning structure should be traversed.
     *
     * <p>The panel is configured to:
     *
     * <ul>
     *   <li>Accept only integer values
     *   <li>Have a minimum value of 0 (no partitioning)
     *   <li>Use an increment step of 1
     *   <li>Be required or optional based on the factory's parameter definition
     * </ul>
     *
     * <p>A {@link ConverterModel} is used to ensure proper type conversion, since the connection parameter might be
     * stored as a String but needs to be used as an Integer in the UI. This is particularly important when editing
     * existing datastores where the parameter may have been stored with a different type than expected.
     *
     * @param paramsModel The model containing all datastore connection parameters
     * @return A configured {@link NumberParamPanel} for the max_hive_depth parameter
     */
    private NumberParamPanel<Integer> buildMaxHiveDepth(final IModel<Map<String, Object>> paramsModel) {
        String key = MAX_HIVE_DEPTH.key;
        boolean required = MAX_HIVE_DEPTH.required;

        // Use ConverterModel to ensure proper conversion from String to Integer
        // This handles cases where the max_hive_depth might be stored as a String
        // but needs to be presented and handled as an Integer in the UI
        IModel<Object> rawModel = new MapModel<>(paramsModel, key);
        IModel<Integer> model = new ConverterModel<>(rawModel, Integer.class);

        ParamResourceModel resourceModel = new ParamResourceModel(key, this);

        return new NumberParamPanel<>(key, model, resourceModel, Integer.class)
                .setRequired(required)
                .setMinimum(0)
                .setStep(1);
    }

    /**
     * Creates a URI input panel for configuring the GeoParquet data source location.
     *
     * <p>This panel provides a text field for entering URIs pointing to GeoParquet datasets, with validation support
     * for various protocols and patterns. The panel is configured to:
     *
     * <ul>
     *   <li>Accept local file paths with or without the "file:" scheme prefix
     *   <li>Support HTTP, HTTPS, and S3 protocols for remote datasets
     *   <li>Allow glob patterns like "**" and "*" for matching multiple files
     *   <li>Verify that specified files/directories exist
     * </ul>
     *
     * <p>Examples of valid input include:
     *
     * <ul>
     *   <li>/data/folder/file.parquet
     *   <li>file:/data/folder/file.parquet
     *   <li>/data/folder/*.parquet
     *   <li>{@literal /data/**}/*.parquet
     *   <li>https://example.com/data/file.parquet
     *   <li>{@literal s3://bucket/path/to/datasets/**}/*.parquet
     * </ul>
     *
     * @param paramsModel The model containing all datastore connection parameters
     * @return A configured {@link URIParamPanel} for the uri parameter
     */
    protected Panel buildUriParamPanel(final IModel<Map<String, Object>> paramsModel) {
        String key = URI_PARAM.key;
        MapModel<String> model = new MapModel<>(paramsModel, key);
        ParamResourceModel resourceModel = new ParamResourceModel(key, this);
        boolean required = true;
        return new URIParamPanel(key, model, resourceModel, required)
                .allowGlob()
                .allowNullSchemeFileURI()
                .fileMustExist()
                .allowedSchemes("http", "https", "s3", "file");
    }
}
