/* (c) 2026 Open Source Geospatial Foundation
 * This code is licensed under the GPL 2.0 license.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only catalog metadata discovery.
 *
 * <p>Returns selected metadata fields without exposing catalog configuration objects.
 *
 * <p>All endpoints require an authenticated metadata reader or GeoServer administrator.
 *
 * <p>Catalog resource visibility is determined by GeoServer's existing security mechanisms.
 */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogMetadataController {

    private final Catalog catalog;

    public CatalogMetadataController(@Qualifier("catalog") Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Require an authenticated user.
     *
     * <p>Access to the metadata REST endpoints is controlled by GeoServer's existing REST security rules. Individual
     * catalog resources remain subject to GeoServer's layer-level security restrictions.
     */
    private void requireMetadataReader() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new RestException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Determine whether a catalog layer permits READ access.
     *
     * <p>Metadata-only visibility is insufficient for schema discovery.
     *
     * <p>Layers without a security wrapper are assumed to have already passed GeoServer's secured catalog filtering.
     */
    private boolean isReadableLayer(LayerInfo layer) {

        if (layer instanceof SecuredLayerInfo secured) {

            AccessLevel level = secured.getWrapperPolicy().getAccessLevel();

            return level == AccessLevel.READ_ONLY || level == AccessLevel.READ_WRITE;
        }

        return true;
    }

    /**
     * List workspaces containing layers readable by the authenticated user.
     *
     * <p>Workspaces are derived from readable published layers rather than catalog.getWorkspaces(), because GeoServer
     * applies different security filtering to workspace and layer discovery.
     *
     * <p>Workspaces without readable published layers are omitted.
     */
    @GetMapping({"/workspaces", "/workspaces.json"})
    public Map<String, List<String>> workspaces() {

        requireMetadataReader();

        List<String> workspaces = catalog.getLayers().stream()
                .filter(this::isReadableLayer)
                .map(layer -> layer.getResource().getNamespace().getPrefix())
                .distinct()
                .sorted()
                .toList();

        return Map.of("workspaces", workspaces);
    }

    /**
     * List layers readable by the authenticated user.
     *
     * <p>Only the qualified layer name is returned.
     *
     * <p>Layers with metadata-only visibility are excluded.
     */
    @GetMapping({"/layers", "/layers.json"})
    public Map<String, List<String>> layers() {

        requireMetadataReader();

        List<String> layers = catalog.getLayers().stream()
                .filter(this::isReadableLayer)
                .map(layer -> layer.getResource().getNamespace().getPrefix() + ":" + layer.getName())
                .sorted()
                .toList();

        return Map.of("layers", layers);
    }

    /**
     * Return the readable attributes of a published vector layer.
     *
     * <p>Only explicitly selected metadata is returned. Datastore configuration is never serialized.
     *
     * <p>The secured feature type is used so configured attribute-level restrictions are applied to the returned
     * schema.
     */
    @GetMapping("/workspaces/{workspaceName}/featuretypes/{layerName}.json")
    public Map<String, Object> featureType(@PathVariable String workspaceName, @PathVariable String layerName) {

        requireMetadataReader();

        /*
         * Search the security-filtered catalog.
         *
         * Exclude metadata-only layers before accessing their resources.
         */
        LayerInfo layer = catalog.getLayers().stream()
                .filter(this::isReadableLayer)
                .filter(l -> layerName.equals(l.getName()))
                .filter(l -> l.getResource() instanceof FeatureTypeInfo)
                .filter(l -> workspaceName.equals(l.getResource().getNamespace().getPrefix()))
                .findFirst()
                .orElse(null);

        if (layer == null) {
            throw new RestException("Feature type not found", HttpStatus.NOT_FOUND);
        }

        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layer.getResource();

        try {

            /*
             * Obtain the secured feature type rather than using
             * FeatureTypeInfo.attributes(), which may expose attributes
             * that have been restricted by GeoServer security rules.
             */
            FeatureType schema = featureTypeInfo.getFeatureType();

            List<Map<String, Object>> attributes = new ArrayList<>();

            for (PropertyDescriptor descriptor : schema.getDescriptors()) {

                if (descriptor instanceof AttributeDescriptor attribute) {

                    Class<?> binding = attribute.getType().getBinding();

                    attributes.add(Map.of(
                            "name",
                            attribute.getLocalName(),
                            "type",
                            binding == null ? "Object" : binding.getSimpleName(),
                            "nillable",
                            attribute.isNillable(),
                            "geometry",
                            attribute instanceof GeometryDescriptor));
                }
            }

            return Map.of(
                    "workspace", workspaceName,
                    "name", layerName,
                    "attributes", attributes);

        } catch (IOException e) {

            throw new RestException("Unable to read feature type schema", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
