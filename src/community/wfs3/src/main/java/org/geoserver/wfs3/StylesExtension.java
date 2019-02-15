/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.SLDPackageHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs3.response.CollectionDocument;
import org.geoserver.wfs3.response.Link;
import org.geotools.util.Version;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** WFS3 extension adding support for get/put styles (as per OGC VTP pilot) */
public class StylesExtension extends AbstractWFS3Extension implements ApplicationContextAware {

    private static final String STYLES_SPECIFICATION;
    static final String STYLES_PATH = "/styles";
    static final String STYLE_PATH = "/styles/{styleId}";
    static final String COLLECTION_STYLES_PATH = "/collections/{collectionId}/styles";
    static final String COLLECTION_STYLE_PATH = "/collections/{collectionId}/styles/{styleId}";

    static {
        try (InputStream is = StylesExtension.class.getResourceAsStream("styles.yml")) {
            STYLES_SPECIFICATION = IOUtils.toString(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the styles.yaml template", e);
        }
    }

    private ArrayList<String> formats;

    @Override
    public void extendAPI(OpenAPI api) {
        // load the pre-cooked building blocks
        OpenAPI template = readTemplate(StylesExtension.STYLES_SPECIFICATION);

        copyAndCustomize(template, api, STYLES_PATH);
        copyAndCustomize(template, api, STYLE_PATH);
        copyAndCustomize(template, api, COLLECTION_STYLES_PATH);
        copyAndCustomize(template, api, COLLECTION_STYLE_PATH);

        addSchemasAndParameters(api, template);
    }

    public void copyAndCustomize(OpenAPI template, OpenAPI target, String stylesPath) {
        PathItem pathItem = template.getPaths().get(stylesPath);
        customizeContentTypes(pathItem.getPost());
        customizeContentTypes(pathItem.getPut());
        target.getPaths().addPathItem(stylesPath, pathItem);
    }

    private void customizeContentTypes(Operation operation) {
        if (operation != null
                && operation.getRequestBody() != null
                && operation.getRequestBody().getContent() != null) {
            Content content = operation.getRequestBody().getContent();
            if (content == null) {
                content = new Content();
                operation.getRequestBody().setContent(content);
            }
            content.clear();
            for (String format : formats) {
                content.addMediaType(format, new MediaType());
            }
        }
    }

    @Override
    public void extendCollection(CollectionDocument collection, BaseRequest request) {
        String collectionId = collection.getName();

        // links
        String baseUrl = request.getBaseUrl();
        String styleAPIUrl =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs3/collections/" + collectionId + "/styles",
                        Collections.emptyMap(),
                        URLMangler.URLType.SERVICE);
        collection
                .getLinks()
                .add(
                        new Link(
                                styleAPIUrl,
                                "styles",
                                "application/json",
                                collectionId + " associated styles."));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        final List<StyleHandler> handlers =
                GeoServerExtensions.extensions(StyleHandler.class, applicationContext);
        this.formats = new ArrayList<>();
        for (StyleHandler handler : handlers) {
            // skip this handler, it's core but does not receive proper handling here (yet)
            if (handler instanceof SLDPackageHandler) {
                continue;
            }
            for (Version version : handler.getVersions()) {
                formats.add(handler.mimeType(version));
            }
        }
    }
}
