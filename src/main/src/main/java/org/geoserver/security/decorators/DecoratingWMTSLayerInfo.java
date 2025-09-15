/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.api.util.ProgressListener;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.util.decorate.AbstractDecorator;

/**
 * Delegates every method to the delegate wmts layer info.
 *
 * <p>Subclasses will override selected methods to perform their "decoration" job
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class DecoratingWMTSLayerInfo extends AbstractDecorator<WMTSLayerInfo> implements WMTSLayerInfo {

    public DecoratingWMTSLayerInfo(WMTSLayerInfo delegate) {
        super(delegate);
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public ReferencedEnvelope boundingBox() throws Exception {
        return delegate.boundingBox();
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
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
    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    @Override
    public CoordinateReferenceSystem getCRS() {
        return delegate.getCRS();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public String getId() {
        return delegate.getId();
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
    public String getNativeName() {
        return delegate.getNativeName();
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
    public Name getQualifiedName() {
        return delegate.getQualifiedName();
    }

    @Override
    public Name getQualifiedNativeName() {
        return delegate.getQualifiedNativeName();
    }

    @Override
    public String getSRS() {
        return delegate.getSRS();
    }

    @Override
    public WMTSStoreInfo getStore() {
        return delegate.getStore();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public WMTSLayer getWMTSLayer(ProgressListener listener) throws IOException {
        return delegate.getWMTSLayer(listener);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void setAbstract(String abstract1) {
        delegate.setAbstract(abstract1);
    }

    @Override
    public void setCatalog(Catalog catalog) {
        delegate.setCatalog(catalog);
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
    public boolean isAdvertised() {
        return delegate.isAdvertised();
    }

    @Override
    public void setAdvertised(boolean advertised) {
        delegate.setAdvertised(advertised);
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
}
