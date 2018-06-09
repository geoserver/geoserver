/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;

/**
 * Utility visitor that will extract the differences between catalog objects. This visitor can be
 * used to fully compare two catalogs.
 */
public final class CatalogDiffVisitor implements CatalogVisitor {

    private final Catalog otherCatalog;
    private final GeoServerDataDirectory dataDir;
    private final GeoServerDataDirectory otherDataDir;

    private final List<InfoDiff> differences = new ArrayList<>();

    public CatalogDiffVisitor(
            Catalog otherCatalog,
            GeoServerDataDirectory dataDir,
            GeoServerDataDirectory otherDataDir) {
        this.otherCatalog = otherCatalog;
        this.dataDir = dataDir;
        this.otherDataDir = otherDataDir;
    }

    @Override
    public void visit(Catalog catalog) {
        listDiffOther(catalog.getWorkspaces(), otherCatalog.getWorkspaces());
        listDiffOther(catalog.getNamespaces(), otherCatalog.getNamespaces());
        listDiffOther(catalog.getDataStores(), otherCatalog.getDataStores());
        listDiffOther(catalog.getCoverageStores(), otherCatalog.getCoverageStores());
        listDiffOther(
                catalog.getStores(WMSStoreInfo.class), otherCatalog.getStores(WMSStoreInfo.class));
        listDiffOther(catalog.getFeatureTypes(), otherCatalog.getFeatureTypes());
        listDiffOther(catalog.getCoverages(), otherCatalog.getCoverages());
        listDiffOther(catalog.getLayers(), otherCatalog.getLayers());
        listDiffOther(
                catalog.getResources(WMSLayerInfo.class),
                otherCatalog.getResources(WMSLayerInfo.class));
        listDiffOther(catalog.getStyles(), otherCatalog.getStyles());
        listDiffOther(catalog.getLayerGroups(), otherCatalog.getLayerGroups());
        catalog.getWorkspaces().forEach(info -> info.accept(this));
        catalog.getNamespaces().forEach(info -> info.accept(this));
        catalog.getDataStores().forEach(info -> info.accept(this));
        catalog.getCoverageStores().forEach(info -> info.accept(this));
        catalog.getStores(WMSStoreInfo.class).forEach(info -> info.accept(this));
        catalog.getFeatureTypes().forEach(info -> info.accept(this));
        catalog.getCoverages().forEach(info -> info.accept(this));
        catalog.getLayers().forEach(info -> info.accept(this));
        catalog.getResources(WMSLayerInfo.class).forEach(info -> info.accept(this));
        catalog.getStyles().forEach(info -> info.accept(this));
        catalog.getLayerGroups().forEach(info -> info.accept(this));
    }

    @Override
    public void visit(WorkspaceInfo workspace) {
        WorkspaceInfo otherWorkspace = otherCatalog.getWorkspace(workspace.getId());
        if (otherWorkspace == null) {
            differences.add(new InfoDiff(workspace, null));
        } else if (!(Objects.equals(workspace.getName(), otherWorkspace.getName())
                && checkEquals(workspace.getMetadata(), otherWorkspace.getMetadata()))) {
            differences.add(new InfoDiff(workspace, otherWorkspace));
        }
    }

    @Override
    public void visit(NamespaceInfo namespace) {
        NamespaceInfo otherNamespace = otherCatalog.getNamespace(namespace.getId());
        if (!(Objects.equals(namespace, otherNamespace)
                && checkEquals(namespace.getMetadata(), namespace.getMetadata()))) {
            differences.add(new InfoDiff(namespace, otherNamespace));
        }
    }

    @Override
    public void visit(DataStoreInfo dataStore) {
        DataStoreInfo otherDataStore = otherCatalog.getDataStore(dataStore.getId());
        if (!(Objects.equals(dataStore, otherDataStore)
                && Objects.equals(dataStore.getType(), otherDataStore.getType())
                && checkEquals(dataStore.getMetadata(), otherDataStore.getMetadata())
                && checkEquals(
                        dataStore.getConnectionParameters(),
                        otherDataStore.getConnectionParameters()))) {
            differences.add(new InfoDiff(dataStore, otherDataStore));
        }
    }

    @Override
    public void visit(CoverageStoreInfo coverageStore) {
        CoverageStoreInfo otherCoverageStore = otherCatalog.getCoverageStore(coverageStore.getId());
        if (!(Objects.equals(coverageStore, otherCoverageStore)
                && Objects.equals(coverageStore.getType(), otherCoverageStore.getType())
                && Objects.equals(coverageStore.getFormat(), otherCoverageStore.getFormat())
                && Objects.equals(coverageStore.getURL(), otherCoverageStore.getURL())
                && checkEquals(coverageStore.getMetadata(), otherCoverageStore.getMetadata())
                && checkEquals(
                        coverageStore.getConnectionParameters(),
                        otherCoverageStore.getConnectionParameters()))) {
            differences.add(new InfoDiff(coverageStore, otherCoverageStore));
        }
    }

    @Override
    public void visit(WMSStoreInfo wmsStore) {
        WMSStoreInfo otherWmsStore = otherCatalog.getStore(wmsStore.getId(), WMSStoreInfo.class);
        if (!(Objects.equals(wmsStore, otherWmsStore)
                && Objects.equals(wmsStore.getType(), otherWmsStore.getType())
                && Objects.equals(wmsStore.getCapabilitiesURL(), otherWmsStore.getCapabilitiesURL())
                && Objects.equals(wmsStore.getUsername(), otherWmsStore.getUsername())
                && Objects.equals(wmsStore.getPassword(), otherWmsStore.getPassword())
                && Objects.equals(wmsStore.getMaxConnections(), otherWmsStore.getMaxConnections())
                && Objects.equals(wmsStore.getReadTimeout(), otherWmsStore.getReadTimeout())
                && Objects.equals(wmsStore.getConnectTimeout(), otherWmsStore.getConnectTimeout())
                && checkEquals(wmsStore.getMetadata(), otherWmsStore.getMetadata())
                && checkEquals(
                        wmsStore.getConnectionParameters(),
                        otherWmsStore.getConnectionParameters()))) {
            differences.add(new InfoDiff(wmsStore, otherWmsStore));
        }
    }

    @Override
    public void visit(FeatureTypeInfo featureType) {
        FeatureTypeInfo otherFeatureType = otherCatalog.getFeatureType(featureType.getId());
        if (otherFeatureType != null) {
            otherFeatureType.setProjectionPolicy(featureType.getProjectionPolicy());
        }
        if (!(Objects.equals(featureType, otherFeatureType)
                && checkEquals(featureType.getAttributes(), otherFeatureType.getAttributes())
                && checkEquals(featureType.getResponseSRS(), otherFeatureType.getResponseSRS())
                && checkEquals(featureType.getAlias(), otherFeatureType.getAlias())
                && checkEquals(featureType.getKeywords(), otherFeatureType.getKeywords())
                && checkEquals(featureType.getDataLinks(), otherFeatureType.getDataLinks())
                && checkEquals(featureType.getMetadataLinks(), otherFeatureType.getMetadataLinks())
                && checkEquals(featureType.getMetadata(), otherFeatureType.getMetadata()))) {
            differences.add(new InfoDiff(featureType, otherFeatureType));
        }
    }

    @Override
    public void visit(CoverageInfo coverage) {
        CoverageInfo otherCoverage = otherCatalog.getCoverage(coverage.getId());
        if (!(Objects.equals(coverage, otherCoverage)
                && checkEquals(coverage.getDimensions(), otherCoverage.getDimensions())
                && checkEquals(
                        coverage.getInterpolationMethods(), otherCoverage.getInterpolationMethods())
                && checkEquals(coverage.getParameters(), otherCoverage.getParameters())
                && checkEquals(coverage.getResponseSRS(), otherCoverage.getResponseSRS())
                && checkEquals(coverage.getAlias(), otherCoverage.getAlias())
                && checkEquals(coverage.getKeywords(), otherCoverage.getKeywords())
                && checkEquals(coverage.getDataLinks(), otherCoverage.getDataLinks())
                && checkEquals(coverage.getMetadataLinks(), otherCoverage.getMetadataLinks())
                && checkEquals(coverage.getMetadata(), otherCoverage.getMetadata()))) {
            differences.add(new InfoDiff(coverage, otherCoverage));
        }
    }

    @Override
    public void visit(LayerInfo layer) {
        LayerInfo otherLayer = otherCatalog.getLayer(layer.getId());
        if (!(Objects.equals(layer, otherLayer)
                && Objects.equals(layer.isAdvertised(), otherLayer.isAdvertised())
                && checkEquals(layer.getAuthorityURLs(), otherLayer.getAuthorityURLs())
                && checkEquals(layer.getIdentifiers(), otherLayer.getIdentifiers())
                && checkEquals(layer.getStyles(), otherLayer.getStyles())
                && checkEquals(layer.getMetadata(), otherLayer.getMetadata()))) {
            differences.add(new InfoDiff(layer, otherLayer));
        }
    }

    @Override
    public void visit(StyleInfo style) {
        StyleInfo otherStyle = otherCatalog.getStyle(style.getId());
        if (!(Objects.equals(style, otherStyle)
                && getStyleFile(style, dataDir).equals(getStyleFile(otherStyle, otherDataDir)))) {
            differences.add(new InfoDiff(style, otherStyle));
        }
    }

    @Override
    public void visit(LayerGroupInfo layerGroup) {
        LayerGroupInfo otherLayerGroup = otherCatalog.getLayerGroup(layerGroup.getId());
        if (!(Objects.equals(layerGroup, otherLayerGroup)
                && checkEquals(layerGroup.getStyles(), otherLayerGroup.getStyles())
                && checkEquals(layerGroup.getAuthorityURLs(), otherLayerGroup.getAuthorityURLs())
                && checkEquals(layerGroup.getIdentifiers(), otherLayerGroup.getIdentifiers())
                && checkEquals(layerGroup.getMetadataLinks(), otherLayerGroup.getMetadataLinks())
                && checkEquals(layerGroup.getMetadata(), otherLayerGroup.getMetadata())
                && checkEquals(layerGroup.getLayers(), otherLayerGroup.getLayers()))) {
            differences.add(new InfoDiff(layerGroup, otherLayerGroup));
        }
    }

    @Override
    public void visit(WMSLayerInfo wmsLayer) {
        WMSLayerInfo otherWmsLayer = otherCatalog.getResource(wmsLayer.getId(), WMSLayerInfo.class);
        if (!(Objects.equals(wmsLayer, otherWmsLayer)
                && checkEquals(wmsLayer.getAlias(), otherWmsLayer.getAlias())
                && checkEquals(wmsLayer.getKeywords(), otherWmsLayer.getKeywords())
                && checkEquals(wmsLayer.getDataLinks(), otherWmsLayer.getDataLinks())
                && checkEquals(wmsLayer.getMetadataLinks(), otherWmsLayer.getMetadataLinks())
                && checkEquals(wmsLayer.getMetadata(), otherWmsLayer.getMetadata()))) {
            differences.add(new InfoDiff(wmsLayer, otherWmsLayer));
        }
    }

    /** Returns the differences found. */
    public List<InfoDiff> differences() {
        return differences;
    }

    /**
     * Register has differences the elements from the other collection that are not present in
     * collection.
     */
    private <T extends CatalogInfo> void listDiffOther(
            Collection<T> collection, Collection<T> otherCollection) {
        differences.addAll(
                otherCollection
                        .stream()
                        .filter(info -> !containsElementWithId(info.getId(), collection))
                        .map(info -> new InfoDiff(null, info))
                        .collect(Collectors.toList()));
    }

    /** Searches a catalog info based in is ID in a provided collection- */
    private <T extends CatalogInfo> boolean containsElementWithId(
            String id, Collection<T> collection) {
        for (T element : collection) {
            if (element.getId().equals(id)) {
                // catalog info element found
                return true;
            }
        }
        // catalog info element not found
        return false;
    }

    /** Retrieves a style file content has a string. */
    private String getStyleFile(StyleInfo styleInfo, GeoServerDataDirectory dataDir) {
        Resource resource = dataDir.get(styleInfo, styleInfo.getFilename());
        try (InputStream input = resource.in();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // reads the style file content to the byte array
            IOUtils.copy(input, output);
            return new String(output.toByteArray());
        } catch (IOException exception) {
            throw new RuntimeException(
                    String.format("Error reading style '%s'.", styleInfo.getName()));
        }
    }

    /** Return TRUE if the two collection contains exactly the same elements in any order. */
    private boolean checkEquals(Collection<?> collectionA, Collection<?> collectionB) {
        if (collectionA.size() != collectionB.size()) {
            // collections have a different size so their are different
            return false;
        }
        for (Object object : collectionA) {
            if (!collectionB.contains(object)) {
                // this element is not present in collection B
                return false;
            }
        }
        // the two collections contain the same elements
        return true;
    }

    /** Checks that the contained metadata is the same. */
    private boolean checkEquals(MetadataMap metadataA, MetadataMap metadataB) {
        return checkEquals(metadataA.getMap(), metadataB.getMap());
    }

    /** Checks that two maps have exactly the same elements. */
    private boolean checkEquals(Map<String, Serializable> mapA, Map<String, Serializable> mapB) {
        if (mapA.size() != mapB.size()) {
            // the have a different number of elements, so their are different
            return false;
        }
        for (Map.Entry<String, Serializable> entry : mapA.entrySet()) {
            if (!Objects.equals(entry.getValue(), mapB.get(entry.getKey()))) {
                // element doesn't exists in map B or is value is different
                return false;
            }
        }
        // the two maps contain the same elements
        return true;
    }

    @Override
    public void visit(WMTSStoreInfo wmsStore) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(WMTSLayerInfo wmtsLayerInfo) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
