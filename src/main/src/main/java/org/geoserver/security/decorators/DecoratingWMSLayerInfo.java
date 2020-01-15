/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.styling.Style;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Delegates every method to the delegate wms layer info. Subclasses will override selected methods
 * to perform their "decoration" job
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DecoratingWMSLayerInfo extends AbstractDecorator<WMSLayerInfo>
        implements WMSLayerInfo {

    public DecoratingWMSLayerInfo(WMSLayerInfo delegate) {
        super(delegate);
    }

    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    public ReferencedEnvelope boundingBox() throws Exception {
        return delegate.boundingBox();
    }

    public boolean enabled() {
        return delegate.enabled();
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

    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    public CoordinateReferenceSystem getCRS() {
        return delegate.getCRS();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public String getId() {
        return delegate.getId();
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

    public NamespaceInfo getNamespace() {
        return delegate.getNamespace();
    }

    public ReferencedEnvelope getNativeBoundingBox() {
        return delegate.getNativeBoundingBox();
    }

    public CoordinateReferenceSystem getNativeCRS() {
        return delegate.getNativeCRS();
    }

    public String getNativeName() {
        return delegate.getNativeName();
    }

    public String prefixedName() {
        return delegate.prefixedName();
    }

    public ProjectionPolicy getProjectionPolicy() {
        return delegate.getProjectionPolicy();
    }

    public Name getQualifiedName() {
        return delegate.getQualifiedName();
    }

    public Name getQualifiedNativeName() {
        return delegate.getQualifiedNativeName();
    }

    public String getSRS() {
        return delegate.getSRS();
    }

    public WMSStoreInfo getStore() {
        return delegate.getStore();
    }

    public String getTitle() {
        return delegate.getTitle();
    }

    public Layer getWMSLayer(ProgressListener listener) throws IOException {
        return delegate.getWMSLayer(listener);
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public void setAbstract(String abstract1) {
        delegate.setAbstract(abstract1);
    }

    public void setCatalog(Catalog catalog) {
        delegate.setCatalog(catalog);
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
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
    public List<String> remoteStyles() { //
        return delegate.remoteStyles();
    }

    @Override
    public String getForcedRemoteStyle() {

        return delegate.getForcedRemoteStyle();
    }

    @Override
    public void setForcedRemoteStyle(String forcedRemoteStyle) {
        delegate.setForcedRemoteStyle(forcedRemoteStyle);
    }

    @Override
    public List<String> availableFormats() {
        return delegate.availableFormats();
    }

    @Override
    public Optional<Style> findRemoteStyleByName(String name) {
        return delegate.findRemoteStyleByName(name);
    }

    @Override
    public boolean isSelectedRemoteStyles(String name) {
        return delegate.isSelectedRemoteStyles(name);
    }

    @Override
    public Set<StyleInfo> getRemoteStyleInfos() {
        return delegate.getRemoteStyleInfos();
    }

    @Override
    public List<String> getSelectedRemoteFormats() {
        return delegate.getSelectedRemoteFormats();
    }

    @Override
    public void setSelectedRemoteFormats(List<String> selectedRemoteFormats) {
        delegate.setSelectedRemoteFormats(selectedRemoteFormats);
    }

    @Override
    public List<String> getSelectedRemoteStyles() {
        return delegate.getSelectedRemoteStyles();
    }

    @Override
    public void setSelectedRemoteStyles(List<String> selectedRemoteStyles) {
        delegate.setSelectedRemoteStyles(selectedRemoteStyles);
    }

    @Override
    public boolean isFormatValid(String format) {
        return delegate.isFormatValid(format);
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public String getPreferredFormat() {
        return delegate.getPreferredFormat();
    }

    @Override
    public void setPreferredFormat(String prefferedFormat) {
        delegate.setPreferredFormat(prefferedFormat);
    }

    @Override
    public Set<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    @Override
    public StyleInfo getDefaultStyle() {
        return delegate.getDefaultStyle();
    }

    @Override
    public Double getMinScale() {
        return delegate.getMinScale();
    }

    @Override
    public void setMinScale(Double minScale) {
        delegate.setMinScale(minScale);
    }

    @Override
    public Double getMaxScale() {
        return delegate.getMaxScale();
    }

    @Override
    public void setMaxScale(Double maxScale) {
        delegate.setMaxScale(maxScale);
    }

    @Override
    public List<StyleInfo> getAllAvailableRemoteStyles() {
        return delegate.getAllAvailableRemoteStyles();
    }

    @Override
    public boolean isMetadataBBoxRespected() {
        return delegate.isMetadataBBoxRespected();
    }

    @Override
    public void setMetadataBBoxRespected(boolean metadataBBoxRespected) {
        delegate.setMetadataBBoxRespected(metadataBBoxRespected);
    }
}
