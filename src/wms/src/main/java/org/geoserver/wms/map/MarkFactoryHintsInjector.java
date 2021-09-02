/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.RenderingHints;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.wms.WMSInfo;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.MarkFactory;
import org.geotools.renderer.style.MarkFactoryListComparator;
import org.geotools.renderer.style.MarkFactoryListPredicate;

/**
 * Checks current MarkFactory filter and order configuration on GeoServer global and per workspace
 * WMSinfo instances. Injects the required {@link Comparator} and {@link Predicate} into the {@link
 * RenderingHints} map.
 */
public class MarkFactoryHintsInjector {

    public static final String MARK_FACTORY_LIST = "MarkFactoryList";

    private final GeoServer geoServer;

    public MarkFactoryHintsInjector(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /** Adds the filter and order hints to rendering process in base to workspace level WMS info. */
    public void addMarkFactoryHints(RenderingHints hints) {
        List<String> markFactoryList = markFactoryOrderByWorkspace();
        Predicate<MarkFactory> predicate = predicate(markFactoryList);
        if (predicate != null) hints.put(DynamicSymbolFactoryFinder.MARK_FACTORY_FILTER, predicate);
        Comparator<MarkFactory> comparator = comparator(markFactoryList);
        if (comparator != null) {
            hints.put(DynamicSymbolFactoryFinder.MARK_FACTORY_ORDER, comparator);
        }
    }

    private List<String> markFactoryOrderByWorkspace() {
        WMSInfo wmsInfo = null;
        WorkspaceInfo workspaceInfo = LocalWorkspace.get();
        if (workspaceInfo != null) {
            wmsInfo = geoServer.getService(workspaceInfo, WMSInfo.class);
        }
        if (wmsInfo == null) {
            wmsInfo = geoServer.getService(WMSInfo.class);
        }
        return getMarkFactoryList(wmsInfo);
    }

    private Predicate<MarkFactory> predicate(List<String> markFactoryList) {
        if (CollectionUtils.isEmpty(markFactoryList)) {
            return null;
        }
        return new MarkFactoryListPredicate(markFactoryList);
    }

    private Comparator<MarkFactory> comparator(List<String> markFactoryList) {
        if (CollectionUtils.isEmpty(markFactoryList)) {
            return null;
        }
        return new MarkFactoryListComparator(markFactoryList);
    }

    @SuppressWarnings("unchecked")
    private List<String> getMarkFactoryList(WMSInfo wmsInfo) {
        if (wmsInfo.getMetadata().containsKey(MARK_FACTORY_LIST)) {
            Object factoriesObj = wmsInfo.getMetadata().get(MARK_FACTORY_LIST);
            if (factoriesObj instanceof List) {
                return (List<String>) factoriesObj;
            }
        }
        return Collections.emptyList();
    }
}
