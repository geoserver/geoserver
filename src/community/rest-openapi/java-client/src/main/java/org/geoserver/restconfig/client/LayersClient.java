package org.geoserver.restconfig.client;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.openapi.model.catalog.LayerInfo;
import org.geoserver.openapi.v1.client.LayersApi;
import org.geoserver.openapi.v1.model.Layer;
import org.geoserver.openapi.v1.model.LayerInfoWrapper;
import org.geoserver.openapi.v1.model.LayerReference;
import org.geoserver.openapi.v1.model.Layers;
import org.geoserver.openapi.v1.model.NamedLink;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LayersClient {

    private @NonNull GeoServerClient client;

    LayersApi api() {
        return client.api(LayersApi.class);
    }

    /** Adapter for {@link LayersApi#getLayer(String)} */
    public Optional<Layer> getLayer(@NonNull String qualifiedLayerName) {
        try {
            return Optional.of(api().getLayer(qualifiedLayerName).getLayer());
        } catch (ServerException.NotFound nf) {
            return Optional.empty();
        }
    }

    /** Adapter for {@link LayersApi#getLayerByWorkspace} */
    public Optional<Layer> getLayer(@NonNull String workspace, @NonNull String layerName) {
        try {
            // tell the server not to flood the logs if not found, as odd as it seems
            Boolean quietOnNotFound = Boolean.TRUE;
            return Optional.of(
                    api().getLayerByWorkspace(workspace, layerName, quietOnNotFound).getLayer());
        } catch (ServerException.NotFound nf) {
            return Optional.empty();
        }
    }

    /** Adapter for {@link LayersApi#getLayers} */
    public List<NamedLink> getLayers() {
        Layers layers = api().getLayers();
        LayerReference ref = layers.getLayers();
        return ref.getLayer();
    }

    /** Adapter for {@link LayersApi#getLayersByWorkspace} */
    public List<NamedLink> getLayers(@NonNull String workspaceName) {
        Layers layers = api().getLayersByWorkspace(workspaceName);
        LayerReference ref = layers.getLayers();
        return ref.getLayer();
    }

    /** Adapter for {@link LayersApi#deleteLayer(String, Boolean)} with {@code recurse = false} */
    public void deleteLayer(@NonNull String qualifiedLayerName) {
        if (!qualifiedLayerName.contains(":")) {
            throw new IllegalArgumentException(
                    "Layer name must be qualified (prefixed with workspace name, e.g. ws:name");
        }
        boolean recurse = true;
        api().deleteLayer(qualifiedLayerName, recurse);
    }

    /**
     * Recursively removes the layer from all layer groups which reference it. If this results in an
     * empty layer group, also deletes the layer group.
     *
     * <p>Adapter for {@link LayersApi#deleteLayer(String, Boolean)} with {@code recurse = true}
     */
    public void deleteLayerRecursively(@NonNull String qualifiedLayerName) {
        if (!qualifiedLayerName.contains(":")) {
            throw new IllegalArgumentException(
                    "Layer name must be qualified (prefixed with workspace name, e.g. ws:name");
        }
        boolean recurse = true;
        api().deleteLayer(qualifiedLayerName, recurse);
    }

    /**
     * Adapter for {@link LayersApi#deleteLayerByWorkspace(String, String, Boolean)} with {@code
     * recurse = false}
     */
    public void deleteLayer(@NonNull String workspaceName, @NonNull String layerName) {
        boolean recurse = false;
        api().deleteLayerByWorkspace(workspaceName, layerName, recurse);
    }

    /**
     * Recursively removes the layer from all layer groups which reference it. If this results in an
     * empty layer group, also deletes the layer group.
     *
     * <p>Adapter for {@link LayersApi#deleteLayerByWorkspace(String, String, Boolean)} with {@code
     * recurse = true}
     */
    public void deleteLayerRecursively(@NonNull String workspaceName, @NonNull String layerName) {
        boolean recurse = true;
        api().deleteLayerByWorkspace(workspaceName, layerName, recurse);
    }

    /**
     * Adapter for {@link LayersApi#updateLayerByWorkspace(String, String, LayerInfoWrapper))
     */
    public void updateLayer(
            @NonNull String workspaceName,
            @NonNull String layerName,
            @NonNull LayerInfo layerInfo) {
        api().updateLayerByWorkspace(
                        workspaceName, layerName, new LayerInfoWrapper().layer(layerInfo));
    }
}
