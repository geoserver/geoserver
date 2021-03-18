package org.geoserver.restconfig.client;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.openapi.model.catalog.NamespaceInfo;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.client.WorkspacesApi;
import org.geoserver.openapi.v1.model.NamedLink;
import org.geoserver.openapi.v1.model.WorkspaceResponseWrapper;
import org.geoserver.openapi.v1.model.WorkspaceSummary;
import org.geoserver.openapi.v1.model.WorkspaceWrapper;
import org.geoserver.openapi.v1.model.WorkspacesResponse;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class WorkspacesClient {

    private @NonNull GeoServerClient client;

    public WorkspacesApi api() {
        return client.api(WorkspacesApi.class);
    }

    public List<Link> findAll() {
        WorkspacesResponse response = api().getWorkspaces();
        WorkspaceResponseWrapper wrapper = response.getWorkspaces();
        List<NamedLink> workspaces = wrapper.getWorkspace();
        return Link.map(workspaces);
    }

    public List<String> findAllNames() {
        return findAll().stream().map(Link::getName).collect(Collectors.toList());
    }

    public Optional<WorkspaceSummary> getDefaultWorkspace() {
        return findByName("default");
    }

    public void setDeafultWorkspace(@NonNull String name) {
        WorkspaceInfo info =
                getAsInfo(name)
                        .orElseThrow(
                                () ->
                                        new ServerException.NotFound(
                                                "No such workspace: " + name, null, null));
        setDeafultWorkspace(info);
    }

    private void setDeafultWorkspace(@NonNull WorkspaceInfo ws) {
        update("default", ws);
    }

    public Optional<WorkspaceSummary> findByName(@NonNull String name) {
        try {
            return Optional.of(api().getWorkspace(name, true).getWorkspace());
        } catch (ServerException e) {
            if (e.getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public @NonNull WorkspaceSummary get(@NonNull String name) {
        return api().getWorkspace(name, true).getWorkspace();
    }

    public @NonNull Optional<WorkspaceInfo> getAsInfo(@NonNull String name) {
        return findByName(name).map(this::toInfo);
    }

    public void delete(@NonNull String name) {
        api().deleteWorkspace(name, false);
    }

    public void deleteRecursively(@NonNull String name) {
        api().deleteWorkspace(name, true);
    }

    public WorkspaceInfo create(@NonNull String name) {
        return create(new WorkspaceInfo().name(name));
    }

    public WorkspaceInfo create(@NonNull WorkspaceInfo ws) {
        api().createWorkspace(wrap(ws), false);
        WorkspaceInfo created =
                getAsInfo(ws.getName())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Workspace create endpoint didn't fail but workspace does not exist"));

        // workaround for geoserver bug
        if (Boolean.TRUE.equals(ws.getIsolated()) && !Boolean.TRUE.equals(created.getIsolated())) {
            setIsolatedNamespace(created);
            created = getAsInfo(ws.getName()).get();
            if (!Boolean.TRUE.equals(created.getIsolated())) {
                throw new IllegalStateException(
                        "Workspace isolated not set even after configuring the namespace");
            }
        }
        return created;
    }

    private void setIsolatedNamespace(final WorkspaceInfo wsInfo) {
        final NamespacesClient namespaces = client.namespaces();
        final String workspaceName = wsInfo.getName();
        // change the namespace URI associated to the workspace
        NamespaceInfo nsInfo =
                namespaces
                        .findByPrefix(workspaceName)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "NamespaceInfo not found for workspace "
                                                        + workspaceName));

        Objects.requireNonNull(nsInfo.getPrefix(), "namespace prefix not set");
        if (!Boolean.TRUE.equals(nsInfo.getIsolated())) {
            log.warn(
                    "NamespaceInfo isolated wasn't set to true during workspace creation. Fixing it...");
            nsInfo.setIsolated(Boolean.TRUE);
        }
        namespaces.update(nsInfo.getPrefix(), nsInfo);
    }

    public void createDeafult(@NonNull WorkspaceInfo workspaceInfo) {
        api().createWorkspace(wrap(workspaceInfo), true);
    }

    public void rename(@NonNull String currentName, @NonNull String newName) {
        update(currentName, toInfo(get(currentName).name(newName)));
    }

    public void update(@NonNull String currentName, WorkspaceInfo newValue) {
        api().modifyWorkspace(currentName, wrap(newValue));
    }

    public WorkspaceInfo toInfo(@NonNull WorkspaceSummary summary) {
        return new WorkspaceInfo().name(summary.getName()).isolated(bool(summary.getIsolated()));
    }

    private WorkspaceWrapper wrap(@NonNull WorkspaceInfo ws) {
        return new WorkspaceWrapper().workspace(ws);
    }

    private boolean bool(Boolean nullableValue) {
        return nullableValue == null ? false : nullableValue.booleanValue();
    }
}
