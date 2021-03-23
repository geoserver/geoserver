package org.geoserver.restconfig.client;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.openapi.model.catalog.NamespaceInfo;
import org.geoserver.openapi.v1.client.NamespacesApi;
import org.geoserver.openapi.v1.model.NamespaceWrapper;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class NamespacesClient {

    private @NonNull GeoServerClient client;

    public NamespacesApi api() {
        return client.api(NamespacesApi.class);
    }

    public void create(@NonNull NamespaceInfo ns) {
        api().addNamespace(new NamespaceWrapper().namespace((ns)));
    }

    public void update(@NonNull String currentPrefix, @NonNull NamespaceInfo newValue) {
        api().modifyNamespace(currentPrefix, new NamespaceWrapper().namespace((newValue)));
    }

    public List<Link> findAll() {
        throw new UnsupportedOperationException();
    }

    public List<String> findAllNames() {
        throw new UnsupportedOperationException();
    }

    public Optional<NamespaceInfo> getDefaultNamespace() {
        throw new UnsupportedOperationException();
    }

    public Optional<NamespaceInfo> findByPrefix(@NonNull String namespacePrefix) {
        try {
            return Optional.of(api().getNamespace(namespacePrefix, Boolean.TRUE).getNamespace());
        } catch (ServerException e) {
            if (e.getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
