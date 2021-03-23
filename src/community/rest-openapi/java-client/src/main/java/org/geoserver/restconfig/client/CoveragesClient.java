package org.geoserver.restconfig.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.openapi.model.catalog.CoverageInfo;
import org.geoserver.openapi.model.catalog.CoverageStoreInfo;
import org.geoserver.openapi.v1.client.CoveragesApi;
import org.geoserver.openapi.v1.model.CoverageInfoWrapper;
import org.geoserver.openapi.v1.model.CoverageListWrapper;
import org.geoserver.openapi.v1.model.CoverageResponse;
import org.geoserver.openapi.v1.model.CoverageResponseWrapper;
import org.geoserver.openapi.v1.model.CoveragesResponse;
import org.geoserver.openapi.v1.model.NamedLink;
import org.geoserver.restconfig.api.v1.mapper.CoverageResponseMapper;
import org.mapstruct.factory.Mappers;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public class CoveragesClient {

    private @NonNull GeoServerClient client;

    private CoverageResponseMapper mapper = Mappers.getMapper(CoverageResponseMapper.class);

    CoveragesApi api() {
        return this.client.api(CoveragesApi.class);
    }

    /**
     * Create a new coverage, the underlying data store must exists.
     *
     * <p>RequestLine: {@code "POST /workspaces/{workspace}/coveragestores/{store}/coverages"}
     *
     * @param workspace The name of the workspace (required)
     * @param store The name of the coverage data store (required)
     * @param info The body of the coverage to POST (required)
     */
    public CoverageInfo create(
            @NonNull String workspace, @NonNull String store, @NonNull CoverageInfo info) {
        Objects.requireNonNull(info.getNativeCoverageName(), "nativeCoverageName is null");
        if (info.getStore() != null && !store.equals(info.getStore().getName())) {
            log.info(
                    "Unsetting CoverageInfo store '{}' to make sure geoserver creates it on the requested store '{}'",
                    info.getStore().getName(),
                    store);
            info = client.clone(info);
            info.setStore(null);
        }

        // find the store in order to provide a consistent exception, geoserver would
        // return a 500 status code produced by a null pointer exception if calling
        // create with a non existent store
        client.coverageStores().getByWorkspaceAndName(workspace, store);

        if (info.getName() == null) {
            info.setName(info.getNativeCoverageName());
        }
        final String name = info.getName();
        api().createCoverageAtStore(workspace, store, new CoverageInfoWrapper().coverage(info));
        return findByStore(workspace, store, name)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        String.format(
                                                "Coverage '%s.%s.%s' not found right after creation",
                                                workspace, store, name)));
    }

    /**
     * Create a new coverage, the coverage definition needs to reference a store.
     *
     * <p>{@code POST /workspaces/{workspace}/coverages}
     *
     * @param workspace The name of the workspace (required)
     * @param info The body of the coverage to POST (required)
     * @return
     */
    public CoverageInfo create(@NonNull String workspace, @NonNull CoverageInfo info) {
        Objects.requireNonNull(
                info.getNativeCoverageName(), "precondition violation: nativeCoverageName is null");
        Objects.requireNonNull(info.getStore(), "precondition violation: store is null");
        Objects.requireNonNull(
                info.getStore().getName(), "precondition violation: store name is null");
        // geoserver fails to discriminate the provided store and workspace in case
        // there's another store with the same name (probably in the default workspace).
        // Setting the store to null forces it to lookup the parametrized workspace and
        // store names
        info = client.clone(info);
        CoverageStoreInfo store = info.getStore();
        info.setStore(null);

        if (info.getName() == null) {
            info.setName(info.getNativeCoverageName());
        }
        info.setNativeName(info.getNativeCoverageName());

        final String name = info.getName();
        api().createCoverageAtStore(
                        workspace, store.getName(), new CoverageInfoWrapper().coverage(info));
        return findByStore(workspace, store.getName(), name)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        String.format(
                                                "Coverage '%s.%s.%s' not found right after creation",
                                                workspace, store.getName(), name)));
    }

    /**
     * Delete a coverage (optionally recursively deleting layers).
     *
     * <p>{@code DELETE
     * /workspaces/{workspace}/coveragestores/{store}/coverages/{coverage}?recurse={recurse}}
     *
     * @param workspace The name of the workspace (required)
     * @param store The name of the coverage datastore (required)
     * @param coverage The name of the coverage (required)
     * @param recurse The recurse controls recursive deletion. When set to true all stores
     *     containing the resource are also removed. (optional, default to false)
     */
    public void delete(
            @NonNull String workspace,
            @NonNull String store,
            @NonNull String coverage,
            Boolean recurse) {
        api().deleteCoverage(workspace, store, coverage, recurse);
    }

    public void deleteRecursively(
            @NonNull String workspace, @NonNull String store, @NonNull String coverage) {
        delete(workspace, store, coverage, Boolean.TRUE);
    }

    /**
     * Get an individual coverage.
     *
     * <p>{@code GET
     * /workspaces/{workspace}/coveragestores/{store}/coverages/{coverage}?quietOnNotFound=true}
     *
     * @param workspace The name of the workspace (required)
     * @param store The name of the coverage datastore (required)
     * @param coverage The name of the coverage (required)
     * @param quietOnNotFound The quietOnNotFound parameter avoids to log an Exception when the
     *     coverage is not present. Note that 404 status code will be returned anyway. (optional,
     *     default to false)
     */
    public Optional<CoverageInfo> findByStore(
            @NonNull String workspace, @NonNull String store, @NonNull String coverage) {
        try {
            CoverageResponseWrapper wrapper =
                    api().findCoverageByStore(workspace, store, coverage, Boolean.TRUE);
            CoverageResponse coverageResponse = wrapper.getCoverage();
            return Optional.of(mapper.map(coverageResponse));
        } catch (ServerException.NotFound nf) {
            return Optional.empty();
        }
    }

    /**
     * Get the coverages available for the provided workspace and data store.
     *
     * <p>{@code GET /workspaces/{workspace}/coveragestores/{store}/coverages?[includeUnpublished?
     * "list=all" : ""]}}
     *
     * @param workspace The name of the workspace (required)
     * @param store The name of the coverage data store (required)
     * @param includeUnpublished If {@code true}, all the coverages available in the data source
     *     (even the non published ones) will be returned.
     */
    public List<NamedLink> findAllByStore(
            @NonNull String workspace, @NonNull String store, boolean includeUnpublished) {
        String list = includeUnpublished ? "all" : null;
        CoveragesResponse response = api().findCoveragesByStore(workspace, store, list);
        CoverageListWrapper listWrapper = response.getCoverageStores();
        return listWrapper == null ? Collections.emptyList() : listWrapper.getCoverage();
    }

    /**
     * Get the published coverages available for the provided workspace and data store.
     *
     * <p>{@code GET /workspaces/{workspace}/coveragestores/{store}/coverages}
     *
     * @param workspace The name of the workspace (required)
     * @param store The name of the coverage data store (required)
     */
    public List<NamedLink> findAllByStore(@NonNull String workspace, @NonNull String store) {
        return findAllByStore(workspace, store, false);
    }

    /**
     * Get the coverages available for the provided workspace.
     *
     * <p>{@code GET /workspaces/{workspace}/coverages?list={list}}
     *
     * @param workspace The name of the workspace (required)
     * @param includeUnpublished If {@code true}, all the coverages available in the worskspace
     *     (even the non published ones) will be returned.
     */
    public List<NamedLink> findAllByWorkspace(
            @NonNull String workspace, boolean includeUnpublished) {
        String list = includeUnpublished ? "all" : null;
        CoveragesResponse response = api().findCoveragesByWorkspace(workspace, list);
        CoverageListWrapper listWrapper = response.getCoverageStores();
        return listWrapper == null ? Collections.emptyList() : listWrapper.getCoverage();
    }

    /**
     * Update an individual coverage forcing the recalculation of native and lat-lon bounding boxes
     *
     * <p>{@code PUT
     * /workspaces/{workspace}/coveragestores/{store}/coverages/{coverage}?calculate=nativebbox,latlonbbox}
     *
     * @param workspace The name of the workspace (required)
     * @param store The name of the coverage datastore (required)
     * @param coverage The name of the coverage (required)
     * @param coverageInfo The body of the coverage to PUT (required)
     * @return
     */
    public CoverageInfo update(
            @NonNull String workspace,
            @NonNull String store,
            @NonNull String coverage,
            @NonNull CoverageInfo coverageInfo) {
        return update(workspace, store, coverage, coverageInfo, true, true);
    }

    /**
     * Update an individual coverage
     *
     * <p>{@code PUT /workspaces/{workspace}/coveragestores/{store}/coverages/{coverage}?calculate=}
     *
     * @param workspace The name of the workspace (required)
     * @param store The name of the coverage datastore (required)
     * @param coverage The name of the coverage (required)
     * @param infcoverageInfo The body of the coverage to PUT (required)
     * @param calculateNativeBbox Force recalculation of the native bounding box
     * @param calculateLatLonBbox Force recalculation of the lat-long bounding box
     * @return
     */
    public CoverageInfo update(
            @NonNull String workspace,
            @NonNull String store,
            @NonNull String coverage,
            final @NonNull CoverageInfo coverageInfo,
            boolean calculateNativeBbox,
            boolean calculateLatLonBbox) {

        CoverageInfo info = client.clone(coverageInfo);
        if (info.getName() == null) {
            info.setName(coverage);
        }
        info.setKeywords(null);
        info.setSupportedFormats(null);
        //		info = new
        // CoverageInfo().name(coverageInfo.getName()).title(coverageInfo.getTitle())._abstract(coverageInfo.getAbstract());

        List<String> calculate = null;

        if (calculateNativeBbox || calculateLatLonBbox) {
            calculate = new ArrayList<>(2);
            if (calculateLatLonBbox) calculate.add("latlonbbox");
            if (calculateNativeBbox) calculate.add("nativebbox");
        }
        api().updateCoverage(
                        workspace,
                        store,
                        coverage,
                        new CoverageInfoWrapper().coverage(info),
                        calculate);
        final String newName = info.getName();
        return findByStore(workspace, store, newName)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        String.format(
                                                "Coverage %s not found after update", newName)));
    }
}
