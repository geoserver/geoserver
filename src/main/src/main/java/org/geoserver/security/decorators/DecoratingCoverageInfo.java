/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.AbstractDecorator;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Delegates all methods to the provided delegate. Suclasses will override methods in order to
 * perform their decoration work
 *
 * @author Andrea Aime - TOPP
 * @param <T>
 * @param <F>
 */
public class DecoratingCoverageInfo extends AbstractDecorator<CoverageInfo>
        implements CoverageInfo {

    public DecoratingCoverageInfo(CoverageInfo delegate) {
        super(delegate);
    }

    public String getAbstract() {
        return delegate.getAbstract();
    }

    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return delegate.getAdapter(adapterClass, hints);
    }

    public List<String> getAlias() {
        return delegate.getAlias();
    }

    public ReferencedEnvelope boundingBox() throws Exception {
        return delegate.boundingBox();
    }

    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    public void setCatalog(Catalog catalog) {
        delegate.setCatalog(catalog);
    }

    public CoordinateReferenceSystem getCRS() {
        return delegate.getCRS();
    }

    public String getDefaultInterpolationMethod() {
        return delegate.getDefaultInterpolationMethod();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public List<CoverageDimensionInfo> getDimensions() {
        return delegate.getDimensions();
    }

    public GridGeometry getGrid() {
        return delegate.getGrid();
    }

    public GridCoverage getGridCoverage(ProgressListener listener, Hints hints) throws IOException {
        return delegate.getGridCoverage(listener, hints);
    }

    public GridCoverage getGridCoverage(
            ProgressListener listener, ReferencedEnvelope envelope, Hints hints)
            throws IOException {
        return delegate.getGridCoverage(listener, envelope, hints);
    }

    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException {
        return delegate.getGridCoverageReader(listener, hints);
    }

    public String getId() {
        return delegate.getId();
    }

    public List<String> getInterpolationMethods() {
        return delegate.getInterpolationMethods();
    }

    @Override
    public List<KeywordInfo> getKeywords() {
        return delegate.getKeywords();
    }

    public List<String> keywordValues() {
        return delegate.keywordValues();
    }

    public ReferencedEnvelope getLatLonBoundingBox() {
        return delegate.getLatLonBoundingBox();
    }

    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    public List<MetadataLinkInfo> getMetadataLinks() {
        return delegate.getMetadataLinks();
    }

    @Override
    public List<DataLinkInfo> getDataLinks() {
        return delegate.getDataLinks();
    }

    public String getName() {
        return delegate.getName();
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedName() */
    public Name getQualifiedName() {
        return delegate.getQualifiedName();
    }

    public NamespaceInfo getNamespace() {
        return delegate.getNamespace();
    }

    public ReferencedEnvelope getNativeBoundingBox() {
        return delegate.getNativeBoundingBox();
    }

    public CoordinateReferenceSystem getNativeCRS() {
        return delegate.getNativeCRS();
    }

    public String getNativeFormat() {
        return delegate.getNativeFormat();
    }

    public String getNativeName() {
        return delegate.getNativeName();
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedNativeName() */
    public Name getQualifiedNativeName() {
        return delegate.getQualifiedNativeName();
    }

    public Map<String, Serializable> getParameters() {
        return delegate.getParameters();
    }

    public String prefixedName() {
        return delegate.prefixedName();
    }

    public ProjectionPolicy getProjectionPolicy() {
        return delegate.getProjectionPolicy();
    }

    public List<String> getRequestSRS() {
        return delegate.getRequestSRS();
    }

    public List<String> getResponseSRS() {
        return delegate.getResponseSRS();
    }

    public String getSRS() {
        return delegate.getSRS();
    }

    public CoverageStoreInfo getStore() {
        return delegate.getStore();
    }

    public List<String> getSupportedFormats() {
        return delegate.getSupportedFormats();
    }

    public String getTitle() {
        return delegate.getTitle();
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public boolean enabled() {
        return delegate.enabled();
    }

    public void setAbstract(String _abstract) {
        delegate.setAbstract(_abstract);
    }

    public void setDefaultInterpolationMethod(String defaultInterpolationMethod) {
        delegate.setDefaultInterpolationMethod(defaultInterpolationMethod);
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void setGrid(GridGeometry grid) {
        delegate.setGrid(grid);
    }

    public void setLatLonBoundingBox(ReferencedEnvelope box) {
        delegate.setLatLonBoundingBox(box);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setNamespace(NamespaceInfo namespace) {
        delegate.setNamespace(namespace);
    }

    public void setNativeBoundingBox(ReferencedEnvelope box) {
        delegate.setNativeBoundingBox(box);
    }

    public void setNativeCRS(CoordinateReferenceSystem nativeCRS) {
        delegate.setNativeCRS(nativeCRS);
    }

    public void setNativeFormat(String nativeFormat) {
        delegate.setNativeFormat(nativeFormat);
    }

    public void setNativeName(String nativeName) {
        delegate.setNativeName(nativeName);
    }

    public void setProjectionPolicy(ProjectionPolicy policy) {
        delegate.setProjectionPolicy(policy);
    }

    public void setSRS(String srs) {
        delegate.setSRS(srs);
    }

    public void setStore(StoreInfo store) {
        delegate.setStore(store);
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
    }

    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public boolean isAdvertised() {
        return delegate.isAdvertised();
    }

    @Override
    public void setAdvertised(boolean advertised) {
        delegate.setAdvertised(advertised);
    }

    @Override
    public String getNativeCoverageName() {
        return delegate.getNativeCoverageName();
    }

    @Override
    public void setNativeCoverageName(String nativeCoverageName) {
        delegate.setNativeCoverageName(nativeCoverageName);
    }

    @Override
    public boolean isServiceConfiguration() {
        return delegate.isServiceConfiguration();
    }

    @Override
    public void setServiceConfiguration(boolean serviceConfiguration) {
        delegate.setServiceConfiguration(serviceConfiguration);
    }

    @Override
    public List<String> getDisabledServices() {
        return delegate.getDisabledServices();
    }

    @Override
    public void setDisabledServices(List<String> disabledServices) {
        delegate.setDisabledServices(disabledServices);
    }
}
