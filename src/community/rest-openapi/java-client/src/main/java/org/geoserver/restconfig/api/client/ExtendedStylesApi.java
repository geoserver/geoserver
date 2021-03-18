package org.geoserver.restconfig.api.client;

import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.openapi.client.internal.ApiClient;
import org.geoserver.openapi.client.internal.EncodingUtils;
import org.geoserver.openapi.v1.client.StylesApi;
import org.geoserver.openapi.v1.model.StyleInfoPost;
import org.geoserver.openapi.v1.model.StyleInfoWrapper;
import org.geoserver.openapi.v1.model.StyleList;
import org.geoserver.openapi.v1.model.StyleListWrapper;

/**
 * Extended {@link StylesApi} Feign client interface to overcome the openapi-generator limitation in
 * supporting multiple response types per API entry point
 */
public interface ExtendedStylesApi extends ApiClient.Api {

    @RequestLine("GET /styles/{style}")
    @Headers({"Accept: application/vnd.geoserver.mbstyle+json"})
    String getStyleBodyMapbox(@Param("style") String style);

    @RequestLine("GET /workspaces/{workspace}/styles/{style}")
    @Headers({"Accept: application/vnd.geoserver.mbstyle+json"})
    String getStyleBodyMapbox(@Param("workspace") String workspace, @Param("style") String style);

    @RequestLine("GET /styles/{style}")
    @Headers({"Accept: application/vnd.ogc.sld+xml"})
    String getStyleBodySLD10(@Param("style") String style);

    @RequestLine("GET /workspaces/{workspace}/styles/{style}")
    @Headers({"Accept: application/vnd.ogc.sld+xml"})
    String getStyleBodySLD10(@Param("workspace") String workspace, @Param("style") String style);

    /**
     * @param style Name of the style to retrieve. (required)
     * @param body The style body of a request. (required)
     * @param raw When set to \&quot;true\&quot;, will forgo parsing and encoding of the uploaded
     *     style content, and instead the style will be streamed directly to the GeoServer
     *     configuration. Use this setting if the content and formatting of the style is to be
     *     preserved exactly. May result in an invalid and unusable style if the payload is
     *     malformed. Allowable values are \&quot;true\&quot; or \&quot;false\&quot; (default). Only
     *     used when uploading a style file. (optional, default to false)
     */
    @RequestLine("PUT /styles/{style}?raw={raw}")
    @Headers({
        "Content-Type: application/vnd.geoserver.mbstyle+json",
        "Accept: application/json",
    })
    void uploadStyleMapbox(@Param("style") String style, byte[] body, @Param("raw") Boolean raw);

    /**
     * @param workspace Name of the workspace for style definitions (required)
     * @param style Name of the style to retrieve. (required)
     * @param body The style body of a request. (required)
     * @param raw When set to \&quot;true\&quot;, will forgo parsing and encoding of the uploaded
     *     style content, and instead the style will be streamed directly to the GeoServer
     *     configuration. Use this setting if the content and formatting of the style is to be
     *     preserved exactly. May result in an invalid and unusable style if the payload is
     *     malformed. Allowable values are \&quot;true\&quot; or \&quot;false\&quot; (default). Only
     *     used when uploading a style file. (optional)
     */
    @RequestLine("PUT /workspaces/{workspace}/styles/{style}?raw={raw}")
    @Headers({
        "Content-Type: application/vnd.geoserver.mbstyle+json",
        "Accept: application/json",
    })
    void uploadStyleMapbox(
            @Param("workspace") String workspace,
            @Param("style") String style,
            byte[] body,
            @Param("raw") Boolean raw);

    /**
     * @param style Name of the style to retrieve. (required)
     * @param body The style body of a request. (required)
     * @param raw When set to \&quot;true\&quot;, will forgo parsing and encoding of the uploaded
     *     style content, and instead the style will be streamed directly to the GeoServer
     *     configuration. Use this setting if the content and formatting of the style is to be
     *     preserved exactly. May result in an invalid and unusable style if the payload is
     *     malformed. Allowable values are \&quot;true\&quot; or \&quot;false\&quot; (default). Only
     *     used when uploading a style file. (optional, default to false)
     */
    @RequestLine("PUT /styles/{style}?raw={raw}")
    @Headers({
        "Content-Type: application/vnd.ogc.sld+xml",
        "Accept: application/json",
    })
    void uploadStyleSLD(@Param("style") String style, byte[] body, @Param("raw") Boolean raw);

    /**
     * @param workspace Name of the workspace for style definitions (required)
     * @param style Name of the style to retrieve. (required)
     * @param body The style body of a request. (required)
     * @param raw When set to \&quot;true\&quot;, will forgo parsing and encoding of the uploaded
     *     style content, and instead the style will be streamed directly to the GeoServer
     *     configuration. Use this setting if the content and formatting of the style is to be
     *     preserved exactly. May result in an invalid and unusable style if the payload is
     *     malformed. Allowable values are \&quot;true\&quot; or \&quot;false\&quot; (default). Only
     *     used when uploading a style file. (optional)
     */
    @RequestLine("PUT /workspaces/{workspace}/styles/{style}?raw={raw}")
    @Headers({
        "Content-Type: application/vnd.ogc.sld+xml",
        "Accept: application/json",
    })
    void uploadStyleSLD(
            @Param("workspace") String workspace,
            @Param("style") String style,
            byte[] body,
            @Param("raw") Boolean raw);

    @RequestLine("PUT /styles/{style}")
    @Headers({
        "Content-Type: application/json",
        "Accept: application/json",
    })
    void update(@Param("style") String style, StyleInfoWrapper info);

    @RequestLine("PUT /workspaces/{workspace}/styles/{style}?raw={raw}")
    @Headers({
        "Content-Type: application/json",
        "Accept: application/json",
    })
    void update(
            @Param("workspace") String workspace,
            @Param("style") String style,
            StyleInfoWrapper info);
    /////////////////// methods copied from generated StylesApi.java
    /////////////////// ///////////////////////////

    /**
     * Add a new style Adds a new style entry to the layer. The style named in styleBody must alread
     * exist, and will not be altered by this request.
     *
     * @param layer Name of the layer to manage styles for (required)
     * @param styleInfoPost Style body information naming an existing style to add to the layer
     *     (required)
     * @param _default Whether to make this the default style for the layer. (optional, default to
     *     false)
     */
    @RequestLine("POST /layers/{layer}/styles?default={_default}")
    @Headers({
        "Content-Type: application/xml",
        "Accept: application/json",
    })
    void addStyleToLayer(
            @Param("layer") String layer,
            StyleInfoPost styleInfoPost,
            @Param("_default") Boolean _default);

    /**
     * Add a new style Adds a new style entry to the layer. The style named in styleBody must alread
     * exist, and will not be altered by this request. Note, this is equivalent to the other <code>
     * addStyleToLayer</code> method, but with the query parameters collected into a single Map
     * parameter. This is convenient for services with optional query parameters, especially when
     * used with the {@link AddStyleToLayerQueryParams} class that allows for building up this map
     * in a fluent style.
     *
     * @param layer Name of the layer to manage styles for (required)
     * @param styleInfoPost Style body information naming an existing style to add to the layer
     *     (required)
     * @param queryParams Map of query parameters as name-value pairs
     *     <p>The following elements may be specified in the query map:
     *     <ul>
     *       <li>_default - Whether to make this the default style for the layer. (optional, default
     *           to false)
     *     </ul>
     */
    @RequestLine("POST /layers/{layer}/styles?default={_default}")
    @Headers({
        "Content-Type: application/xml",
        "Accept: application/json",
    })
    void addStyleToLayer(
            @Param("layer") String layer,
            StyleInfoPost styleInfoPost,
            @QueryMap(encoded = true) Map<String, Object> queryParams);

    /**
     * A convenience class for generating query parameters for the <code>addStyleToLayer</code>
     * method in a fluent style.
     */
    public static class AddStyleToLayerQueryParams extends HashMap<String, Object> {
        public AddStyleToLayerQueryParams _default(final Boolean value) {
            put("default", EncodingUtils.encode(value));
            return this;
        }
    }

    /**
     * Add a new style Adds a new style entry to the server. Using POST with the
     * &#x60;application/xml&#x60; or &#x60;application/json&#x60; content only adds the style info
     * to the catalog and does not upload style content. PUT to &#x60;/styles/{style}&#x60; to
     * upload the style in this case. Use POST with a style file
     * (&#x60;application/vnd.ogc.sld+xml&#x60; or &#x60;application/vnd.ogc.sld+xml&#x60; for SLD;
     * additional style types are added by extensions) to generate a style info and upload the style
     * all at once. Then seperately PUT the style info at &#x60;/styles/{style}&#x60; to make any
     * desired changes to the generated catalog entry. You can also use POST with a ZIP file to
     * upload a SLD 1.0 (&#x60;application/vnd.ogc.sld+xml&#x60;) file and any associated icon
     * files, and then separately PUT the style info at /styles/{style}. POST with a ZIP file does
     * not support any other style types.
     *
     * @param styleInfoWrapper The StyleInfo body of a request. (required)
     * @param name The name of the style. Used only when POSTing a style file or ZIP bundle, to
     *     determine the name of the style in the catalog. Generated from the filename if not
     *     provided. (optional)
     * @return String
     */
    @RequestLine("POST /styles?name={name}")
    @Headers({
        "Content-Type: application/json",
        "Accept: text/plain",
    })
    String createStyle(StyleInfoWrapper styleInfoWrapper, @Param("name") String name);

    /**
     * Add a new style Adds a new style entry to the server. Using POST with the
     * &#x60;application/xml&#x60; or &#x60;application/json&#x60; content only adds the style info
     * to the catalog and does not upload style content. PUT to &#x60;/styles/{style}&#x60; to
     * upload the style in this case. Use POST with a style file
     * (&#x60;application/vnd.ogc.sld+xml&#x60; or &#x60;application/vnd.ogc.sld+xml&#x60; for SLD;
     * additional style types are added by extensions) to generate a style info and upload the style
     * all at once. Then seperately PUT the style info at &#x60;/styles/{style}&#x60; to make any
     * desired changes to the generated catalog entry. You can also use POST with a ZIP file to
     * upload a SLD 1.0 (&#x60;application/vnd.ogc.sld+xml&#x60;) file and any associated icon
     * files, and then separately PUT the style info at /styles/{style}. POST with a ZIP file does
     * not support any other style types. Note, this is equivalent to the other <code>createStyle
     * </code> method, but with the query parameters collected into a single Map parameter. This is
     * convenient for services with optional query parameters, especially when used with the {@link
     * CreateStyleQueryParams} class that allows for building up this map in a fluent style.
     *
     * @param styleInfoWrapper The StyleInfo body of a request. (required)
     * @param queryParams Map of query parameters as name-value pairs
     *     <p>The following elements may be specified in the query map:
     *     <ul>
     *       <li>name - The name of the style. Used only when POSTing a style file or ZIP bundle, to
     *           determine the name of the style in the catalog. Generated from the filename if not
     *           provided. (optional)
     *     </ul>
     *
     * @return String
     */
    @RequestLine("POST /styles?name={name}")
    @Headers({
        "Content-Type: application/json",
        "Accept: text/plain",
    })
    String createStyle(
            StyleInfoWrapper styleInfoWrapper,
            @QueryMap(encoded = true) Map<String, Object> queryParams);

    /**
     * A convenience class for generating query parameters for the <code>createStyle</code> method
     * in a fluent style.
     */
    public static class CreateStyleQueryParams extends HashMap<String, Object> {
        public CreateStyleQueryParams name(final String value) {
            put("name", EncodingUtils.encode(value));
            return this;
        }
    }

    /**
     * Add a new style to a given workspace Adds a new style entry to the server. Using POST with
     * the &#x60;application/xml&#x60; or &#x60;application/json&#x60; content only adds the style
     * info to the catalog and does not upload style content. PUT to
     * &#x60;/workspaces/{workspace}/styles/{style}&#x60; to upload the style in this case. Use POST
     * with a style file (&#x60;application/vnd.ogc.sld+xml&#x60; or
     * &#x60;application/vnd.ogc.sld+xml&#x60; for SLD; additional style types are added by
     * extensions) to generate a style info and upload the style all at once. Then seperately PUT
     * the style info at &#x60;/workspaces/{workspace}/styles/{style}&#x60; to make any desired
     * changes to the generated catalog entry. You can also use POST with a ZIP file to upload a SLD
     * 1.0 (&#x60;application/vnd.ogc.sld+xml&#x60;) file and any associated icon files, and then
     * separately PUT the style info at /workspaces/{workspace}/styles/{style}. POST with a ZIP file
     * does not support any other style types.
     *
     * @param workspace Name of workspace (required)
     * @param styleInfoWrapper The StyleInfo body of a request. (required)
     * @param name The name of the style. Used only when POSTing a style file or ZIP bundle, to
     *     determine the name of the style in the catalog. Generated from the filename if not
     *     provided. (optional)
     * @return String
     */
    @RequestLine("POST /workspaces/{workspace}/styles?name={name}")
    @Headers({
        "Content-Type: application/json",
        "Accept: text/plain",
    })
    String createStyleByWorkspace(
            @Param("workspace") String workspace,
            StyleInfoWrapper styleInfoWrapper,
            @Param("name") String name);

    /**
     * Add a new style to a given workspace Adds a new style entry to the server. Using POST with
     * the &#x60;application/xml&#x60; or &#x60;application/json&#x60; content only adds the style
     * info to the catalog and does not upload style content. PUT to
     * &#x60;/workspaces/{workspace}/styles/{style}&#x60; to upload the style in this case. Use POST
     * with a style file (&#x60;application/vnd.ogc.sld+xml&#x60; or
     * &#x60;application/vnd.ogc.sld+xml&#x60; for SLD; additional style types are added by
     * extensions) to generate a style info and upload the style all at once. Then seperately PUT
     * the style info at &#x60;/workspaces/{workspace}/styles/{style}&#x60; to make any desired
     * changes to the generated catalog entry. You can also use POST with a ZIP file to upload a SLD
     * 1.0 (&#x60;application/vnd.ogc.sld+xml&#x60;) file and any associated icon files, and then
     * separately PUT the style info at /workspaces/{workspace}/styles/{style}. POST with a ZIP file
     * does not support any other style types. Note, this is equivalent to the other <code>
     * createStyleByWorkspace</code> method, but with the query parameters collected into a single
     * Map parameter. This is convenient for services with optional query parameters, especially
     * when used with the {@link CreateStyleByWorkspaceQueryParams} class that allows for building
     * up this map in a fluent style.
     *
     * @param workspace Name of workspace (required)
     * @param styleInfoWrapper The StyleInfo body of a request. (required)
     * @param queryParams Map of query parameters as name-value pairs
     *     <p>The following elements may be specified in the query map:
     *     <ul>
     *       <li>name - The name of the style. Used only when POSTing a style file or ZIP bundle, to
     *           determine the name of the style in the catalog. Generated from the filename if not
     *           provided. (optional)
     *     </ul>
     *
     * @return String
     */
    @RequestLine("POST /workspaces/{workspace}/styles?name={name}")
    @Headers({
        "Content-Type: application/json",
        "Accept: text/plain",
    })
    String createStyleByWorkspace(
            @Param("workspace") String workspace,
            StyleInfoWrapper styleInfoWrapper,
            @QueryMap(encoded = true) Map<String, Object> queryParams);

    /**
     * A convenience class for generating query parameters for the <code>createStyleByWorkspace
     * </code> method in a fluent style.
     */
    public static class CreateStyleByWorkspaceQueryParams extends HashMap<String, Object> {
        public CreateStyleByWorkspaceQueryParams name(final String value) {
            put("name", EncodingUtils.encode(value));
            return this;
        }
    }

    /**
     * Delete style Deletes a style.
     *
     * @param style Name of the style to retrieve. (required)
     * @param purge Specifies whether the underlying file containing the style should be deleted on
     *     disk. (optional, default to false)
     * @param recurse Removes references to the specified style in existing layers. (optional,
     *     default to false)
     */
    @RequestLine("DELETE /styles/{style}?purge={purge}&recurse={recurse}")
    @Headers({
        "Accept: application/json",
    })
    void deleteStyle(
            @Param("style") String style,
            @Param("purge") Boolean purge,
            @Param("recurse") Boolean recurse);

    /**
     * Delete style Deletes a style. Note, this is equivalent to the other <code>deleteStyle</code>
     * method, but with the query parameters collected into a single Map parameter. This is
     * convenient for services with optional query parameters, especially when used with the {@link
     * DeleteStyleQueryParams} class that allows for building up this map in a fluent style.
     *
     * @param style Name of the style to retrieve. (required)
     * @param queryParams Map of query parameters as name-value pairs
     *     <p>The following elements may be specified in the query map:
     *     <ul>
     *       <li>purge - Specifies whether the underlying file containing the style should be
     *           deleted on disk. (optional, default to false)
     *       <li>recurse - Removes references to the specified style in existing layers. (optional,
     *           default to false)
     *     </ul>
     */
    @RequestLine("DELETE /styles/{style}?purge={purge}&recurse={recurse}")
    @Headers({
        "Accept: application/json",
    })
    void deleteStyle(
            @Param("style") String style,
            @QueryMap(encoded = true) Map<String, Object> queryParams);

    /**
     * A convenience class for generating query parameters for the <code>deleteStyle</code> method
     * in a fluent style.
     */
    public static class DeleteStyleQueryParams extends HashMap<String, Object> {
        public DeleteStyleQueryParams purge(final Boolean value) {
            put("purge", EncodingUtils.encode(value));
            return this;
        }

        public DeleteStyleQueryParams recurse(final Boolean value) {
            put("recurse", EncodingUtils.encode(value));
            return this;
        }
    }

    /**
     * Delete style in a given workspace Deletes a style in a given workspace.
     *
     * @param workspace Name of the workspace for style definitions (required)
     * @param style Name of the style to retrieve. (required)
     * @param purge Specifies whether the underlying file containing the style should be deleted on
     *     disk. (optional, default to false)
     * @param recurse Removes references to the specified style in existing layers. (optional,
     *     default to false)
     */
    @RequestLine("DELETE /workspaces/{workspace}/styles/{style}?purge={purge}&recurse={recurse}")
    @Headers({
        "Accept: application/json",
    })
    void deleteStyleByWorkspace(
            @Param("workspace") String workspace,
            @Param("style") String style,
            @Param("purge") Boolean purge,
            @Param("recurse") Boolean recurse);

    /**
     * Delete style in a given workspace Deletes a style in a given workspace. Note, this is
     * equivalent to the other <code>deleteStyleByWorkspace</code> method, but with the query
     * parameters collected into a single Map parameter. This is convenient for services with
     * optional query parameters, especially when used with the {@link
     * DeleteStyleByWorkspaceQueryParams} class that allows for building up this map in a fluent
     * style.
     *
     * @param workspace Name of the workspace for style definitions (required)
     * @param style Name of the style to retrieve. (required)
     * @param queryParams Map of query parameters as name-value pairs
     *     <p>The following elements may be specified in the query map:
     *     <ul>
     *       <li>purge - Specifies whether the underlying file containing the style should be
     *           deleted on disk. (optional, default to false)
     *       <li>recurse - Removes references to the specified style in existing layers. (optional,
     *           default to false)
     *     </ul>
     */
    @RequestLine("DELETE /workspaces/{workspace}/styles/{style}?purge={purge}&recurse={recurse}")
    @Headers({
        "Accept: application/json",
    })
    void deleteStyleByWorkspace(
            @Param("workspace") String workspace,
            @Param("style") String style,
            @QueryMap(encoded = true) Map<String, Object> queryParams);

    /**
     * A convenience class for generating query parameters for the <code>deleteStyleByWorkspace
     * </code> method in a fluent style.
     */
    public static class DeleteStyleByWorkspaceQueryParams extends HashMap<String, Object> {
        public DeleteStyleByWorkspaceQueryParams purge(final Boolean value) {
            put("purge", EncodingUtils.encode(value));
            return this;
        }

        public DeleteStyleByWorkspaceQueryParams recurse(final Boolean value) {
            put("recurse", EncodingUtils.encode(value));
            return this;
        }
    }

    /**
     * Retrieve a style Retrieves a single style. Used to both request the style info and the style
     * defintion body, depending on the media type requested. The media type can be specified either
     * by using the \&quot;Accept:\&quot; header or by appending an extension to the endpoint. For
     * example, a style info can be requested in XML format using \&quot;/styles/{style}.xml\&quot;
     * or \&quot;Accept: application/xml\&quot;. (Also available: \&quot;{style}.json\&quot;,
     * \&quot;Accept: application/json\&quot; \&quot;{style}.html\&quot;, and \&quot;Accept:
     * text/html\&quot;). The style definition body can be requested by either appending the file
     * extension of the style file (e.g., \&quot;{style}.sld\&quot; or \&quot;{style}.css\&quot;) or
     * by specifying the correct media type for the style definition in the \&quot;Accept\&quot;
     * header. Below are common style formats and the corresponding media types that can be used in
     * the Accept header to request the style definition body. - application/vnd.ogc.sld+xml for SLD
     * 1.0.0 SLDs - application/vnd.ogc.se+xml for SLD 1.1.0 SLDs -
     * application/vnd.geoserver.geocss+css for css styles - application/vnd.geoserver.ysld+yaml for
     * ysld styles - application/vnd.geoserver.mbstyle+json for mb styles
     *
     * @param style Name of the style to retrieve. (required)
     * @return StyleInfoWrapper
     */
    @RequestLine("GET /styles/{style}")
    @Headers({
        "Accept: application/json",
    })
    StyleInfoWrapper getStyle(@Param("style") String style);

    /**
     * Retrieve a style from a given workspace Retrieves a single style. Used to both request the
     * style info and the style defintion body, depending on the media type requested. The media
     * type can be specified either by using the \&quot;Accept:\&quot; header or by appending an
     * extension to the endpoint. For example, a style info can be requested in XML format using
     * \&quot;/styles/{style}.xml\&quot; or \&quot;Accept: application/xml\&quot;. (Also available:
     * \&quot;{style}.json\&quot;, \&quot;Accept: application/json\&quot;
     * \&quot;{style}.html\&quot;, and \&quot;Accept: text/html\&quot;). The style definition body
     * can be requested by either appending the file extension of the style file (e.g.,
     * \&quot;{style}.sld\&quot; or \&quot;{style}.css\&quot;) or by specifying the correct media
     * type for the style definition in the \&quot;Accept\&quot; header. Below are common style
     * formats and the corresponding media types that can be used in the Accept header to request
     * the style definition body. - application/vnd.ogc.sld+xml for SLD 1.0.0 SLDs -
     * application/vnd.ogc.se+xml for SLD 1.1.0 SLDs - application/vnd.geoserver.geocss+css for css
     * styles - application/vnd.geoserver.ysld+yaml for ysld styles -
     * application/vnd.geoserver.mbstyle+json for mb styles
     *
     * @param workspace Name of the workspace for style definitions (required)
     * @param style Name of the style to retrieve. (required)
     * @return StyleInfoWrapper
     */
    @RequestLine("GET /workspaces/{workspace}/styles/{style}")
    @Headers({
        "Accept: application/json",
    })
    StyleInfoWrapper getStyleByWorkspace(
            @Param("workspace") String workspace, @Param("style") String style);

    /**
     * Get a list of styles Displays a list of all styles on the server.
     *
     * @return StyleListWrapper
     */
    @RequestLine("GET /styles")
    @Headers({
        "Accept: application/json",
    })
    StyleListWrapper getStyles();

    /**
     * Get a list of layer alternate styles Displays a list of all alternate styles for a given
     * layer. Use the \&quot;Accept:\&quot; header to specify format or append an extension to the
     * endpoint (example \&quot;/layers/{layer}/styles.xml\&quot; for XML).
     *
     * @param layer Name of the layer to manage styles for (required)
     * @return StyleList
     */
    @RequestLine("GET /layers/{layer}/styles")
    @Headers({
        "Accept: application/json",
    })
    StyleList getStylesByLayer(@Param("layer") String layer);

    /**
     * Get a list of styles in a given workspace Displays a list of all styles in a given workspace.
     * Use the \&quot;Accept:\&quot; header to specify format or append an extension to the endpoint
     * (example \&quot;/workspaces/{workspace}/styles.xml\&quot; for XML).
     *
     * @param workspace Name of workspace (required)
     * @return StyleListWrapper
     */
    @RequestLine("GET /workspaces/{workspace}/styles")
    @Headers({
        "Accept: application/json",
    })
    StyleListWrapper getStylesByWorkspace(@Param("workspace") String workspace);

    /**
     * A convenience class for generating query parameters for the <code>uploadStyle</code> method
     * in a fluent style.
     */
    public static class UploadStyleQueryParams extends HashMap<String, Object> {
        public UploadStyleQueryParams raw(final Boolean value) {
            put("raw", EncodingUtils.encode(value));
            return this;
        }
    }
}
