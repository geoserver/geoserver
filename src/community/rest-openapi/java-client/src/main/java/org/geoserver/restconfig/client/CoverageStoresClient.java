package org.geoserver.restconfig.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.openapi.model.catalog.CoverageStoreInfo;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.client.CoveragestoresApi;
import org.geoserver.openapi.v1.model.CoverageStoreInfoWrapper;
import org.geoserver.openapi.v1.model.CoverageStoreListWrapper;
import org.geoserver.openapi.v1.model.CoverageStoreResponse;
import org.geoserver.openapi.v1.model.CoverageStoreResponseWrapper;
import org.geoserver.openapi.v1.model.CoverageStoresResponse;
import org.geoserver.openapi.v1.model.PurgeOption;
import org.geoserver.restconfig.client.ServerException.NotFound;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CoverageStoresClient {

    private @NonNull GeoServerClient client;

    CoveragestoresApi api() {
        return this.client.api(CoveragestoresApi.class);
    }

    public CoverageStoreResponse create(
            @NonNull String workspace,
            @NonNull String name,
            String description,
            @NonNull CoverageStoreInfo.TypeEnum type,
            @NonNull String url) {

        CoverageStoreInfo info = new CoverageStoreInfo();
        info.setName(name);
        info.setDescription(description);
        info.setEnabled(true);
        info.setType(type);
        info.setUrl(url);
        info.setWorkspace(new WorkspaceInfo().name(workspace));
        return this.create(info);
    }

    public CoverageStoreResponse create(@NonNull CoverageStoreInfo info) {
        String workspace = info.getWorkspace().getName();
        this.api()
                .createCoverageStore(workspace, new CoverageStoreInfoWrapper().coverageStore(info));
        return this.findByWorkspaceAndName(workspace, info.getName())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "CoverageStore not found after being created: "
                                                + info.getName()));
    }

    public CoverageStoreResponse update(
            @NonNull String oldWorkspace,
            @NonNull String oldName,
            @NonNull CoverageStoreInfo info) {
        this.api()
                .modifyCoverageStore(
                        oldWorkspace, oldName, new CoverageStoreInfoWrapper().coverageStore(info));
        return this.findByWorkspaceAndName(info.getWorkspace().getName(), info.getName())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "CoverageStoreInfo not found after being updated: "
                                                + info.getName()));
    }

    public CoverageStoreResponse update(@NonNull CoverageStoreInfo info) {
        String workspace = info.getWorkspace().getName();
        this.api()
                .modifyCoverageStore(
                        workspace,
                        info.getName(),
                        new CoverageStoreInfoWrapper().coverageStore(info));
        return this.findByWorkspaceAndName(workspace, info.getName())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "CoverageStoreInfo not found after being updated: "
                                                + info.getName()));
    }

    public void delete(@NonNull String ws, @NonNull String name) {
        throw new UnsupportedOperationException(
                "Not safe to delete a datastore non recursively until the following issue is fixed: https://osgeo-org.atlassian.net/browse/GEOS-9189");
        // api().deleteDatastore(ws, name, false);
    }

    public void deleteRecursive(@NonNull String ws, @NonNull String name) {
        this.api().deleteCoverageStore(ws, name, PurgeOption.ALL, true);
    }

    public List<Link> findByWorkspace(@NonNull WorkspaceInfo workspace) {
        return this.findByWorkspace(workspace.getName());
    }

    public List<Link> findByWorkspace(@NonNull String workspace) {
        return Optional.ofNullable(this.api().getCoverageStores(workspace))
                .map(CoverageStoresResponse::getCoverageStores)
                .map(CoverageStoreListWrapper::getCoverageStore)
                .map(Link::map)
                .orElse(Collections.emptyList());
    }

    /** @throws NotFound */
    public CoverageStoreResponse getByWorkspaceAndName(
            @NonNull String workspaceName, @NonNull String storeName) {
        CoverageStoreResponseWrapper response;
        response = this.api().getCoverageStore(workspaceName, storeName, true);
        return response.getCoverageStore();
    }

    public Optional<CoverageStoreResponse> findByWorkspaceAndName(
            @NonNull String workspaceName, @NonNull String storeName) {
        CoverageStoreResponse response;
        try {
            response = getByWorkspaceAndName(workspaceName, storeName);
        } catch (ServerException.NotFound nf) {
            return Optional.empty();
        }
        return Optional.of(response);
    }
}
