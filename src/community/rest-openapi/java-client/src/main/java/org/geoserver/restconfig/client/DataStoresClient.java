package org.geoserver.restconfig.client;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.openapi.model.catalog.DataStoreInfo;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.client.DatastoresApi;
import org.geoserver.openapi.v1.model.ConnectionParameterEntry;
import org.geoserver.openapi.v1.model.ConnectionParameters;
import org.geoserver.openapi.v1.model.DataStoreInfoWrapper;
import org.geoserver.openapi.v1.model.DataStoreListWrapper;
import org.geoserver.openapi.v1.model.DataStoreResponse;
import org.geoserver.openapi.v1.model.DataStoreWrapper;
import org.geoserver.openapi.v1.model.DataStoresListResponse;
import org.geoserver.restconfig.client.DataStoreParams.Shapefile;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DataStoresClient {

    private @NonNull GeoServerClient client;

    DatastoresApi api() {
        return this.client.api(DatastoresApi.class);
    }

    public DataStoreResponse create(
            @NonNull String workspace,
            @NonNull String name,
            String description,
            @NonNull DataStoreParams connectionParameters) {
        return this.create(workspace, name, description, connectionParameters.asMap());
    }

    public DataStoreResponse create(
            @NonNull String workspace,
            @NonNull String name,
            String description,
            @NonNull Map<String, String> connectionParameters) {

        DataStoreInfo info = new DataStoreInfo();
        info.setName(name);
        info.setDescription(description);
        info.setEnabled(true);
        info.setConnectionParameters(connectionParameters);
        return this.create(workspace, info);
    }

    public DataStoreResponse create(@NonNull String workspace, @NonNull DataStoreInfo info) {
        this.api().createDatastore(workspace, new DataStoreInfoWrapper().dataStore(info));
        return this.findByWorkspaceAndName(workspace, info.getName())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "DataStore not found after being created: "
                                                + info.getName()));
    }

    public DataStoreResponse createShapefileDataStore(
            @NonNull String workspace, @NonNull String storeName, @NonNull URI shpfile) {
        String uri = shpfile.toString();
        Shapefile connectionParameters = new DataStoreParams.Shapefile().uri(uri);
        DataStoreResponse created = this.create(workspace, storeName, null, connectionParameters);
        return created;
    }

    public DataStoreResponse update(@NonNull String workspace, @NonNull DataStoreInfo info) {
        this.api()
                .modifyDataStore(
                        workspace, info.getName(), new DataStoreInfoWrapper().dataStore(info));
        return this.findByWorkspaceAndName(workspace, info.getName())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "DataStore not found after being updated: "
                                                + info.getName()));
    }

    public void delete(@NonNull String ws, @NonNull String name) {
        throw new UnsupportedOperationException(
                "Not safe to delete a datastore non recursively until the following issue is fixed: https://osgeo-org.atlassian.net/browse/GEOS-9189");
        // api().deleteDatastore(ws, name, false);
    }

    public void deleteRecursive(@NonNull String ws, @NonNull String name) {
        this.api().deleteDatastore(ws, name, true);
    }

    public List<Link> findByWorkspace(@NonNull WorkspaceInfo workspace) {
        return this.findByWorkspace(workspace.getName());
    }

    public List<Link> findByWorkspace(@NonNull String workspace) {
        return Optional.ofNullable(this.api().getDatastores(workspace))
                .map(DataStoresListResponse::getDataStores)
                .map(DataStoreListWrapper::getDataStore)
                .map(Link::map)
                .orElse(Collections.emptyList());
    }

    public Optional<DataStoreResponse> findByWorkspaceAndName(
            @NonNull String workspaceName, @NonNull String storeName) {
        DataStoreWrapper response;
        try {
            response = this.api().getDataStore(workspaceName, storeName, Boolean.TRUE);
        } catch (ServerException.NotFound nf) {
            return Optional.empty();
        }
        return Optional.of(response.getDataStore());
    }

    public Map<String, String> toConnectionParameters(@NonNull ConnectionParameters params) {
        return params.getEntry().stream()
                .collect(
                        Collectors.toMap(
                                ConnectionParameterEntry::getAtKey,
                                ConnectionParameterEntry::getValue));
    }

    public ConnectionParameters toConnectionParameters(@NonNull Map<String, String> params) {
        return new ConnectionParameters()
                .entry(
                        params.entrySet().stream()
                                .map(this::connectionParameterEntry)
                                .collect(Collectors.toList()));
    }

    private ConnectionParameterEntry connectionParameterEntry(Map.Entry<String, String> e) {
        return new ConnectionParameterEntry().atKey(e.getKey()).value(e.getValue());
    }

    public DataStoreInfo toInfo(DataStoreResponse response) {
        DataStoreInfo info = new DataStoreInfo();
        info.setName(response.getName());
        info.setDescription(response.getDescription());
        info.setEnabled(response.getEnabled());
        if (response.getWorkspace() != null) {
            info.setWorkspace(new WorkspaceInfo().name(response.getWorkspace().getName()));
        }
        info.setConnectionParameters(toConnectionParameters(response.getConnectionParameters()));
        return info;
    }
}
