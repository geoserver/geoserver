/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features.tiled;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.AbstractLandingPageDocument;
import org.geoserver.api.ConformanceDocument;
import org.geoserver.api.DocumentCallback;
import org.geoserver.api.FreemarkerTemplateSupport;
import org.geoserver.api.HTMLExtensionCallback;
import org.geoserver.api.OpenAPICallback;
import org.geoserver.api.features.CollectionDocument;
import org.geoserver.api.features.CollectionsDocument;
import org.geoserver.api.features.FeaturesLandingPage;
import org.geoserver.api.tiles.TileMatrixSets;
import org.geoserver.api.tiles.TilesDocument;
import org.geoserver.api.tiles.TilesLandingPage;
import org.geoserver.api.tiles.TilesService;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Extends responses from the {@link org.geoserver.api.features.FeatureService} with tile handling
 * links and the like. For the time being, it allows only configured cached layers to be served,
 * while allowing dynamic, non cached filtering on them. We might want to extend it to full non
 * cached tiles serving, but would require a rewrite of the TileService machinery.
 */
@Component
public class TiledFeaturesExtension
        implements ApplicationListener<ContextRefreshedEvent>,
                OpenAPICallback,
                DocumentCallback,
                HTMLExtensionCallback {

    private static final String FEATURES = "Features";
    private static final String REQ_CONFORMANCE = "getConformanceDeclaration";
    private static final String REQ_COLLECTION = "describeCollection";
    private static final String REQ_COLLECTIONS = "getCollections";
    private static final String REQ_LANDING = "getLandingPage";
    private static final String REQ_API = "getApi";

    private final FreemarkerTemplateSupport templateSupport;
    private TiledFeatureService tiledFeatures;

    public TiledFeaturesExtension(FreemarkerTemplateSupport templateSupport) {
        this.templateSupport = templateSupport;
    }

    @Override
    public String getExtension(Request dr, Map model, List htmlExtensionArguments)
            throws IOException {
        if (dr.getService().equals(FEATURES)) {
            if (dr.getRequest().equals(REQ_LANDING)) {
                return templateSupport.processTemplate(
                        null, "landingPageTileMatrixExtension.ftl", getClass(), model);
            } else if (dr.getRequest().equals(REQ_COLLECTION)) {
                // get the collection provided by the function call and replace the model
                Map clonedModel = new HashMap(model);
                CollectionDocument collection = (CollectionDocument) model.get("model");
                if (tiledFeatures.isTiledVectorLayer(collection.getId())) {
                    clonedModel.put("collection", collection);
                    return templateSupport.processTemplate(
                            null, "collectionExtension.ftl", getClass(), clonedModel);
                }
            } else if (dr.getRequest().equals(REQ_COLLECTIONS)) {
                // get the collection provided by the function call and replace the model
                Map clonedModel = new HashMap(model);
                CollectionDocument collection = (CollectionDocument) htmlExtensionArguments.get(0);
                if (tiledFeatures.isTiledVectorLayer(collection.getId())) {
                    clonedModel.put("collection", collection);
                    return templateSupport.processTemplate(
                            null, "collectionExtension.ftl", getClass(), clonedModel);
                }
            }
        }

        return null;
    }

    @Override
    public void apply(Request dr, AbstractDocument document) {
        if (dr.getService().equals(FEATURES)) {
            if (document instanceof ConformanceDocument) {
                extendConformanceClasses((ConformanceDocument) document);
            } else if (document instanceof CollectionDocument) {
                extendCollectionDocument((CollectionDocument) document);
            } else if (document instanceof CollectionsDocument) {
                extendCollectionsDocument((CollectionsDocument) document);
            } else if (document instanceof FeaturesLandingPage) {
                extendLandingpage((AbstractLandingPageDocument) document);
            }
        }
    }

    @Override
    public void apply(Request dr, OpenAPI target) {
        if (dr.getService().equals(FEATURES)) {
            // get the tiles building blocks
            OpenAPI tilesAPI = tiledFeatures.tileServiceAPI();

            // add the tile matrix sets and single tile matrix set
            copyPathItem(target, tilesAPI, "/tileMatrixSets");
            copyPathItem(target, tilesAPI, "/tileMatrixSets/{tileMatrixSetId}");
            // add the tiles
            copyPathItem(
                    target,
                    tilesAPI,
                    "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
            // copy over the vector formats
            copyParameter(target, tilesAPI, "f-vector");
        }
    }

    private void copyParameter(OpenAPI target, OpenAPI tilesAPI, String parameterKey) {
        Parameter parameter = tilesAPI.getComponents().getParameters().get(parameterKey);
        if (parameter == null) {
            throw new RuntimeException("Could not find parameter " + parameterKey);
        }
        target.getComponents().getParameters().put(parameterKey, parameter);
    }

    private void copyPathItem(OpenAPI target, OpenAPI tilesAPI, String pathItemKey) {
        PathItem item = tilesAPI.getPaths().get(pathItemKey);
        if (item == null) {
            throw new RuntimeException("Could not find path item " + pathItemKey);
        }
        target.getPaths().addPathItem(pathItemKey, item);
    }

    private void extendLandingpage(AbstractLandingPageDocument landingPage) {
        // tile matrix sets
        landingPage.addLinksFor(
                "ogc/features/tileMatrixSets",
                TileMatrixSets.class,
                "Tile matrix set list as ",
                "tileMatrixSets",
                null,
                TilesLandingPage.REL_TILING_SCHEMES);
    }

    public void extendConformanceClasses(ConformanceDocument conformance) {
        conformance.getConformsTo().add(TilesService.CC_CORE);
    }

    private void extendCollectionDocument(CollectionDocument collection) {
        if (tiledFeatures.isTiledVectorLayer(collection.getId())) {
            String encodedId = ResponseUtils.urlEncode(collection.getId());
            collection.addLinksFor(
                    "ogc/features/collections/" + encodedId + "/tiles",
                    TilesDocument.class,
                    "Tiles metadata as ",
                    "dataTiles",
                    null,
                    "tiles");
        }
    }

    private void extendCollectionsDocument(CollectionsDocument collections) {
        collections.addCollectionDecorator(this::extendCollectionDocument);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.tiledFeatures = event.getApplicationContext().getBean(TiledFeatureService.class);
    }
}
