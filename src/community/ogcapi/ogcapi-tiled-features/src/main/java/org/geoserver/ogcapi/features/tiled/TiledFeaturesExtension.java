/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features.tiled;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.DocumentCallback;
import org.geoserver.ogcapi.FreemarkerTemplateSupport;
import org.geoserver.ogcapi.HTMLExtensionCallback;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ogcapi.OpenAPICallback;
import org.geoserver.ogcapi.features.CollectionDocument;
import org.geoserver.ogcapi.features.CollectionsDocument;
import org.geoserver.ogcapi.features.FeaturesLandingPage;
import org.geoserver.ogcapi.tiles.TileMatrixSets;
import org.geoserver.ogcapi.tiles.TiledCollectionDocument;
import org.geoserver.ogcapi.tiles.TilesDocument;
import org.geoserver.ogcapi.tiles.TilesLandingPage;
import org.geoserver.ogcapi.tiles.TilesService;
import org.geoserver.ows.Request;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Extends responses from the {@link org.geoserver.ogcapi.features.FeatureService} with tile
 * handling links and the like. For the time being, it allows only configured cached layers to be
 * served, while allowing dynamic, non cached filtering on them. We might want to extend it to full
 * non cached tiles serving, but would require a rewrite of the TileService machinery.
 */
@Component
public class TiledFeaturesExtension
        implements ApplicationListener<ContextRefreshedEvent>,
                OpenAPICallback,
                DocumentCallback,
                HTMLExtensionCallback {

    private static final String FEATURES = "Features";
    private static final String REQ_COLLECTION = "describeCollection";
    private static final String REQ_COLLECTIONS = "getCollections";
    private static final String REQ_LANDING = "getLandingPage";

    private final FreemarkerTemplateSupport templateSupport;
    private TiledFeatureService tiledFeatures;
    private TilesService tilesService;

    public TiledFeaturesExtension(FreemarkerTemplateSupport templateSupport) {
        this.templateSupport = templateSupport;
    }

    @Override
    public String getExtension(
            Request dr, Map<String, Object> model, Charset charset, List htmlExtensionArguments)
            throws IOException {
        if (dr.getService().equals(FEATURES)) {
            if (dr.getRequest().equals(REQ_LANDING)) {
                return templateSupport.processTemplate(
                        null, "landingPageTileMatrixExtension.ftl", getClass(), model, charset);
            } else if (dr.getRequest().equals(REQ_COLLECTION)) {
                // get the collection provided by the function call and replace the model
                Map<String, Object> clonedModel = new HashMap<>(model);
                CollectionDocument collection = (CollectionDocument) model.get("model");
                if (tiledFeatures.isTiledVectorLayer(collection.getId())) {
                    clonedModel.put("collection", collection);
                    return templateSupport.processTemplate(
                            null, "collectionExtension.ftl", getClass(), clonedModel, charset);
                }
            } else if (dr.getRequest().equals(REQ_COLLECTIONS)) {
                // get the collection provided by the function call and replace the model
                Map<String, Object> clonedModel = new HashMap<>(model);
                CollectionDocument collection = (CollectionDocument) htmlExtensionArguments.get(0);
                if (tiledFeatures.isTiledVectorLayer(collection.getId())) {
                    clonedModel.put("collection", collection);
                    return templateSupport.processTemplate(
                            null, "collectionExtension.ftl", getClass(), clonedModel, charset);
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
                extendLandingpage(document);
            }
        }
    }

    @Override
    public void apply(Request dr, OpenAPI target) throws IOException {
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

    private void extendLandingpage(AbstractDocument landingPage) {
        // tile matrix sets
        new LinksBuilder(TileMatrixSets.class, "ogc/features/tileMatrixSets")
                .title("Tile matrix set list as ")
                .rel(TilesLandingPage.REL_TILING_SCHEMES)
                .add(landingPage);
    }

    public void extendConformanceClasses(ConformanceDocument conformance) {
        conformance.getConformsTo().add(ConformanceClass.CORE);
        ConformanceDocument cd = tilesService.conformance();
        cd.getConformsTo()
                .forEach(
                        c -> {
                            if (!conformance.getConformsTo().contains(c))
                                conformance.getConformsTo().add(c);
                        });
    }

    private void extendCollectionDocument(CollectionDocument collection) {
        if (tiledFeatures.isTiledVectorLayer(collection.getId())) {
            new LinksBuilder(TilesDocument.class, "ogc/features/collections/")
                    .segment(collection.getId(), true)
                    .segment("tiles")
                    .title("Tiles metadata as ")
                    .rel(TiledCollectionDocument.REL_TILESETS_VECTOR)
                    .add(collection);
        }
    }

    private void extendCollectionsDocument(CollectionsDocument collections) {
        collections.addCollectionDecorator(this::extendCollectionDocument);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.tiledFeatures = event.getApplicationContext().getBean(TiledFeatureService.class);
        this.tilesService = event.getApplicationContext().getBean(TilesService.class);
    }
}
