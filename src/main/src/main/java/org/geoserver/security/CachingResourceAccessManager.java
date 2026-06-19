/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Memoizes {@link ResourceAccessManager} results within a single HTTP request.
 *
 * <p>When no request is bound (background threads, startup), every call delegates directly with no caching. Results are
 * assumed read-only by callers (callers that need to mutate must clone first).
 */
public class CachingResourceAccessManager extends ResourceAccessManagerWrapper {

    private static final String CACHE_ATTR = CachingResourceAccessManager.class.getName();

    private enum Kind {
        WORKSPACE,
        LAYER,
        LAYER_CTX,
        RESOURCE,
        STYLE,
        LAYER_GROUP,
        LAYER_GROUP_CTX,
        SECURITY_FILTER,
        IS_WS_ADMIN
    }

    private record Key(Kind kind, String user, String target, String containers) {}

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        return cached(Kind.WORKSPACE, user, workspace.getId(), null, () -> delegate.getAccessLimits(user, workspace));
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return cached(Kind.LAYER, user, layer.getId(), null, () -> delegate.getAccessLimits(user, layer));
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        return cached(
                Kind.LAYER_CTX,
                user,
                layer.getId(),
                containerKey(containers),
                () -> delegate.getAccessLimits(user, layer, containers));
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        return cached(Kind.RESOURCE, user, resource.getId(), null, () -> delegate.getAccessLimits(user, resource));
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        return cached(Kind.STYLE, user, style.getId(), null, () -> delegate.getAccessLimits(user, style));
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        return cached(
                Kind.LAYER_GROUP, user, layerGroup.getId(), null, () -> delegate.getAccessLimits(user, layerGroup));
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        return cached(
                Kind.LAYER_GROUP_CTX,
                user,
                layerGroup.getId(),
                containerKey(containers),
                () -> delegate.getAccessLimits(user, layerGroup, containers));
    }

    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        return cached(Kind.SECURITY_FILTER, user, clazz.getName(), null, () -> delegate.getSecurityFilter(user, clazz));
    }

    @Override
    public boolean isWorkspaceAdmin(Authentication user, Catalog catalog) {
        Boolean result = cached(Kind.IS_WS_ADMIN, user, null, null, () -> delegate.isWorkspaceAdmin(user, catalog));
        return Boolean.TRUE.equals(result);
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(Kind kind, Authentication auth, String targetId, String containers, Supplier<T> loader) {
        // if targetId is null we can't build a safe key - two different objects with null IDs
        // would wrongly share a cache entry
        if (targetId == null && kind != Kind.IS_WS_ADMIN) return loader.get();

        // bypass if there is no current request context (e.g., background thread, post-request cleanup)
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) return loader.get();

        Map<Key, Object> cache;
        try {
            cache = (Map<Key, Object>) attrs.getAttribute(CACHE_ATTR, RequestAttributes.SCOPE_REQUEST);
            if (cache == null) {
                cache = new HashMap<>();
                attrs.setAttribute(CACHE_ATTR, cache, RequestAttributes.SCOPE_REQUEST);
            }
        } catch (IllegalStateException e) {
            // request already completed but attributes still bound to thread (importer, WPS post-processing);
            // clear the stale binding so subsequent calls on this thread skip the cache cleanly
            RequestContextHolder.resetRequestAttributes();
            return loader.get();
        }

        Key key = new Key(kind, userKey(auth), targetId, containers);
        if (cache.containsKey(key)) {
            return (T) cache.get(key);
        }
        T result = loader.get();
        cache.put(key, result);
        return result;
    }

    private static String userKey(Authentication auth) {
        return auth == null ? "" : auth.getName();
    }

    private static String containerKey(List<LayerGroupInfo> containers) {
        if (containers == null || containers.isEmpty()) return "";
        return containers.stream().map(LayerGroupInfo::getId).collect(Collectors.joining(","));
    }
}
