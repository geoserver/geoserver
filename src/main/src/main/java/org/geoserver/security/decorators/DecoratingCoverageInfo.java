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
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.api.util.ProgressListener;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.AbstractDecorator;
import org.geotools.util.factory.Hints;

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

    @Override
    public String getAbstract() {
        return delegate.getAbstract();
    }

    @Override
    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return delegate.getAdapter(adapterClass, hints);
    }

    @Override
    public List<String> getAlias() {
        return delegate.getAlias();
    }

    @Override
    public ReferencedEnvelope boundingBox() throws Exception {
        return delegate.boundingBox();
    }

    @Override
    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    @Override
    public void setCatalog(Catalog catalog) {
        delegate.setCatalog(catalog);
    }

    @Override
    public CoordinateReferenceSystem getCRS() {
        return delegate.getCRS();
    }

    @Override
    public String getDefaultInterpolationMethod() {
        return delegate.getDefaultInterpolationMethod();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public List<CoverageDimensionInfo> getDimensions() {
        return delegate.getDimensions();
    }

    @Override
    public GridGeometry getGrid() {
        return delegate.getGrid();
    }

    @Override
    public GridCoverage getGridCoverage(ProgressListener listener, Hints hints) throws IOException {
        return delegate.getGridCoverage(listener, hints);
    }

    @Override
    public GridCoverage getGridCoverage(
            ProgressListener listener, ReferencedEnvelope envelope, Hints hints)
            throws IOException {
        return delegate.getGridCoverage(listener, envelope, hints);
    }

    @Override
    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException {
        return delegate.getGridCoverageReader(listener, hints);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public List<String> getInterpolationMethods() {
        return delegate.getInterpolationMethods();
    }

    @Override
    public List<KeywordInfo> getKeywords() {
        return delegate.getKeywords();
    }

    @Override
    public List<String> keywordValues() {
        return delegate.keywordValues();
    }

    @Override
    public ReferencedEnvelope getLatLonBoundingBox() {
        return delegate.getLatLonBoundingBox();
    }

    @Override
    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public List<MetadataLinkInfo> getMetadataLinks() {
        return delegate.getMetadataLinks();
    }

    @Override
    public List<DataLinkInfo> getDataLinks() {
        return delegate.getDataLinks();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedName() */
    @Override
    public Name getQualifiedName() {
        return delegate.getQualifiedName();
    }

    @Override
    public NamespaceInfo getNamespace() {
        return delegate.getNamespace();
    }

    @Override
    public ReferencedEnvelope getNativeBoundingBox() {
        return delegate.getNativeBoundingBox();
    }

    @Override
    public CoordinateReferenceSystem getNativeCRS() {
        return delegate.getNativeCRS();
    }

    @Override
    public String getNativeFormat() {
        return delegate.getNativeFormat();
    }

    @Override
    public String getNativeName() {
        return delegate.getNativeName();
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedNativeName() */
    @Override
    public Name getQualifiedNativeName() {
        return delegate.getQualifiedNativeName();
    }

    @Override
    public Map<String, Serializable> getParameters() {
        return delegate.getParameters();
    }

    @Override
    public String prefixedName() {
        return delegate.prefixedName();
    }

    @Override
    public ProjectionPolicy getProjectionPolicy() {
        return delegate.getProjectionPolicy();
    }

    @Override
    public List<String> getRequestSRS() {
        return delegate.getRequestSRS();
    }

    @Override
    public List<String> getResponseSRS() {
        return delegate.getResponseSRS();
    }

    @Override
    public String getSRS() {
        return delegate.getSRS();
    }

    @Override
    public CoverageStoreInfo getStore() {
        return delegate.getStore();
    }

    @Override
    public List<String> getSupportedFormats() {
        return delegate.getSupportedFormats();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public void setAbstract(String _abstract) {
        delegate.setAbstract(_abstract);
    }

    @Override
    public void setDefaultInterpolationMethod(String defaultInterpolationMethod) {
        delegate.setDefaultInterpolationMethod(defaultInterpolationMethod);
    }

    @Override
    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    @Override
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public void setGrid(GridGeometry grid) {
        delegate.setGrid(grid);
    }

    @Override
    public void setLatLonBoundingBox(ReferencedEnvelope box) {
        delegate.setLatLonBoundingBox(box);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setNamespace(NamespaceInfo namespace) {
        delegate.setNamespace(namespace);
    }

    @Override
    public void setNativeBoundingBox(ReferencedEnvelope box) {
        delegate.setNativeBoundingBox(box);
    }

    @Override
    public void setNativeCRS(CoordinateReferenceSystem nativeCRS) {
        delegate.setNativeCRS(nativeCRS);
    }

    @Override
    public void setNativeFormat(String nativeFormat) {
        delegate.setNativeFormat(nativeFormat);
    }

    @Override
    public void setNativeName(String nativeName) {
        delegate.setNativeName(nativeName);
    }

    @Override
    public void setProjectionPolicy(ProjectionPolicy policy) {
        delegate.setProjectionPolicy(policy);
    }

    @Override
    public void setSRS(String srs) {
        delegate.setSRS(srs);
    }

    @Override
    public void setStore(StoreInfo store) {
        delegate.setStore(store);
    }

    @Override
    public void setTitle(String title) {
        delegate.setTitle(title);
    }

    @Override
    public InternationalString getInternationalTitle() {
        return delegate.getInternationalTitle();
    }

    @Override
    public void setInternationalTitle(InternationalString internationalTitle) {
        delegate.setInternationalTitle(internationalTitle);
    }

    @Override
    public InternationalString getInternationalAbstract() {
        return delegate.getInternationalAbstract();
    }

    @Override
    public void setInternationalAbstract(InternationalString internationalAbstract) {
        delegate.setInternationalAbstract(internationalAbstract);
    }

    @Override
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

    @Override
    public boolean isSimpleConversionEnabled() {
        return delegate.isSimpleConversionEnabled();
    }

    @Override
    public void setSimpleConversionEnabled(boolean activateComplexToSimpleOutput) {
        delegate.setSimpleConversionEnabled(activateComplexToSimpleOutput);
    }
}
