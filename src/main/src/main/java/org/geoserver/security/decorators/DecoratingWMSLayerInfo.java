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
import org.opengis.util.InternationalString;
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
    public WMSStoreInfo getStore() {
        return delegate.getStore();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public Layer getWMSLayer(ProgressListener listener) throws IOException {
        return delegate.getWMSLayer(listener);
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
