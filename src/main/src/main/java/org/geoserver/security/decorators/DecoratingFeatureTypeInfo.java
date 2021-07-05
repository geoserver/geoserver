/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.measure.Measure;
import org.geotools.util.decorate.AbstractDecorator;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

/**
 * Delegates every method to the delegate feature type info. Subclasses will override selected
 * methods to perform their "decoration" job
 *
 * @author Andrea Aime
 */
public abstract class DecoratingFeatureTypeInfo extends AbstractDecorator<FeatureTypeInfo>
        implements FeatureTypeInfo {

    public DecoratingFeatureTypeInfo(FeatureTypeInfo info) {
        super(info);
    }

    @Override
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(
            ProgressListener listener, Hints hints) throws IOException {
        return delegate.getFeatureSource(listener, hints);
    }

    @Override
    public DataStoreInfo getStore() {
        return delegate.getStore();
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
    public List<AttributeTypeInfo> getAttributes() {
        return delegate.getAttributes();
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
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public List<AttributeTypeInfo> attributes() throws IOException {
        return delegate.attributes();
    }

    @Override
    public FeatureType getFeatureType() throws IOException {
        return delegate.getFeatureType();
    }

    @Override
    public Filter filter() {
        return delegate.filter();
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
    public int getMaxFeatures() {
        return delegate.getMaxFeatures();
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
    public String getNativeName() {
        return delegate.getNativeName();
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedNativeName() */
    @Override
    public Name getQualifiedNativeName() {
        return delegate.getQualifiedNativeName();
    }

    @Override
    public int getNumDecimals() {
        return delegate.getNumDecimals();
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
    public String getSRS() {
        return delegate.getSRS();
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
    public void setMaxFeatures(int maxFeatures) {
        delegate.setMaxFeatures(maxFeatures);
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
    public void setNumDecimals(int numDecimals) {
        delegate.setNumDecimals(numDecimals);
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
    public List<String> getResponseSRS() {
        return delegate.getResponseSRS();
    }

    @Override
    public boolean isOverridingServiceSRS() {
        return delegate.isOverridingServiceSRS();
    }

    @Override
    public void setOverridingServiceSRS(boolean overridingServiceSRS) {
        delegate.setOverridingServiceSRS(overridingServiceSRS);
    }

    @Override
    public boolean getSkipNumberMatched() {
        return delegate.getSkipNumberMatched();
    }

    @Override
    public void setSkipNumberMatched(boolean skipNumberMatched) {
        delegate.setSkipNumberMatched(skipNumberMatched);
    }

    @Override
    public Measure getLinearizationTolerance() {
        return delegate.getLinearizationTolerance();
    }

    @Override
    public void setLinearizationTolerance(Measure tolerance) {
        delegate.setLinearizationTolerance(tolerance);
    }

    @Override
    public boolean isCircularArcPresent() {
        return delegate.isCircularArcPresent();
    }

    @Override
    public void setCircularArcPresent(boolean enabled) {
        delegate.setCircularArcPresent(enabled);
    }

    @Override
    public String getCqlFilter() {
        return delegate.getCqlFilter();
    }

    @Override
    public void setCqlFilter(String cqlFilter) {
        delegate.setCqlFilter(cqlFilter);
    }

    @Override
    public boolean getEncodeMeasures() {
        return delegate.getEncodeMeasures();
    }

    @Override
    public void setEncodeMeasures(boolean encodeMeasures) {
        delegate.setEncodeMeasures(encodeMeasures);
    }

    @Override
    public boolean getPadWithZeros() {
        return delegate.getPadWithZeros();
    }

    @Override
    public void setPadWithZeros(boolean padWithZeros) {
        delegate.setPadWithZeros(padWithZeros);
    }

    @Override
    public boolean getForcedDecimal() {
        return delegate.getForcedDecimal();
    }

    @Override
    public void setForcedDecimal(boolean forcedDecimal) {
        delegate.setForcedDecimal(forcedDecimal);
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
