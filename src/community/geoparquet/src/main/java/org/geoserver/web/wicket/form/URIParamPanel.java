/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.form;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.data.store.panel.ParamPanel;

/**
 * A reusable UI component for inputting and validating URIs and file paths in GeoServer web forms.
 *
 * <p>This panel provides a labeled text field for entering URIs with comprehensive validation capabilities. It
 * integrates with GeoServer's web form framework through the {@link ParamPanel} interface, providing a consistent user
 * experience for configuring datastore connections.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Support for multiple URI schemes (file, http, https, s3, etc.)
 *   <li>Flexible configuration through method chaining
 *   <li>Validation feedback for invalid URIs
 *   <li>Optional file existence checking
 *   <li>Support for glob patterns to match multiple files
 *   <li>Configurable required/optional status
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * URIParamPanel uriPanel = new URIParamPanel("uri", model, labelModel, true)
 *     .allowGlob()                                  // Enable glob pattern support
 *     .allowNullSchemeFileURI()                     // Allow file paths without 'file:' prefix
 *     .fileMustExist()                              // Ensure the specified file exists or the glob pattern matches files
 *     .allowedSchemes("http", "https", "s3", "file"); // Limit allowed URI schemes
 * }</pre>
 *
 * <p>Examples of supported URI formats:
 *
 * <ul>
 *   <li>{@literal D:\data\file.parquet} - Windows absolute path
 *   <li>{@literal /data/file.parquet} - Unix absolute path
 *   <li>{@literal file:/data/file.parquet} - File URI with scheme
 *   <li>{@literal data/*.parquet} - Simple glob pattern
 *   <li>{@literal C:\overture\release\**}\*.parquet} - Windows path with recursive glob
 *   <li>{@literal /data/**}/*.parquet} - Unix path with recursive glob
 *   <li>{@literal s3://bucket/path/to/file.parquet} - S3 object URI
 *   <li>{@literal https://example.com/data/file.parquet} - HTTPS URI
 * </ul>
 *
 * <p>This component is primarily used for configuring GeoParquet datastores, but can be reused for any application that
 * requires URI input with validation.
 *
 * @see URIValidator
 * @see ParamPanel
 */
@SuppressWarnings("serial")
public class URIParamPanel extends Panel implements ParamPanel<String> {

    private final TextField<String> uriInput;

    private final URIValidator validator;

    /**
     * Creates a new panel with a URI input field and validation.
     *
     * @param id The component ID
     * @param paramValue The model containing the URI string value
     * @param paramLabelModel The model for the field label text
     * @param required Whether the field is required
     */
    public URIParamPanel(
            final String id,
            final IModel<String> paramValue,
            final IModel<String> paramLabelModel,
            final boolean required) {
        // make the value of the text field the model of this panel, for easy value
        // retrieval
        super(id, paramValue);

        // the label, with an asterisk if the field is required
        String requiredMark = required ? " *" : "";
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        validator = new URIValidator();

        // the text field, with a decorator for validations
        uriInput = new TextField<>("paramValue", paramValue);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        uriInput.setLabel(paramLabelModel);
        uriInput.setRequired(required);
        uriInput.add(validator);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(uriInput);
        add(feedback);
    }

    /**
     * Returns the internal form component for direct access.
     *
     * @return The TextField component used in this panel
     */
    @Override
    public FormComponent<String> getFormComponent() {
        return uriInput;
    }

    /**
     * Configures the validator to check that referenced files actually exist.
     *
     * <p>When this option is enabled, the validator will verify that:
     *
     * <ul>
     *   <li>For simple file paths: the file exists in the filesystem
     *   <li>For glob patterns: at least one matching file exists
     * </ul>
     *
     * @return This panel instance for method chaining
     */
    public URIParamPanel fileMustExist() {
        validator.fileMustExist();
        return this;
    }

    /**
     * Configures the validator to accept glob patterns in file paths.
     *
     * <p>Glob patterns allow matching multiple files with wildcards and special characters:
     *
     * <ul>
     *   <li>* - matches any sequence of characters within a path component
     *   <li>** - matches any sequence of characters across multiple path components
     *   <li>? - matches a single character
     *   <li>[] - matches a single character from a character class
     *   <li>{} - matches a sequence from a group of patterns
     * </ul>
     *
     * @return This panel instance for method chaining
     */
    public URIParamPanel allowGlob() {
        validator.allowGlob();
        return this;
    }

    /**
     * Configures the validator to accept file URIs without an explicit "file:" scheme.
     *
     * <p>When enabled, paths like "/data/file.parquet" will be treated as file URIs without requiring
     * "file:/data/file.parquet".
     *
     * @return This panel instance for method chaining
     */
    public URIParamPanel allowNullSchemeFileURI() {
        validator.allowNullSchemeFileURI();
        return this;
    }

    /**
     * Configures the validator to limit allowed URI schemes.
     *
     * <p>This method defines which URI protocols are acceptable in the input field. Common examples include "file",
     * "http", "https", "s3", etc.
     *
     * @param uriSchemes Zero or more allowed scheme strings. If none provided, all schemes are allowed.
     * @return This panel instance for method chaining
     */
    public URIParamPanel allowedSchemes(String... uriSchemes) {
        validator.allowedSchemes(uriSchemes);
        return this;
    }
}
