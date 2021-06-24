/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.util.InternationalString;

/**
 * Delegates every method to the wrapped {@link LayerInfo}. Subclasses will override selected
 * methods to perform their "decoration" job
 *
 * @author Andrea Aime
 */
public class DecoratingLayerInfo extends AbstractDecorator<LayerInfo> implements LayerInfo {

    public DecoratingLayerInfo(LayerInfo delegate) {
        super(delegate);
    }

    @Override
    public StyleInfo getDefaultStyle() {
        return delegate.getDefaultStyle();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public LegendInfo getLegend() {
        return delegate.getLegend();
    }

    @Override
    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String prefixedName() {
        return delegate.prefixedName();
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public ResourceInfo getResource() {
        return delegate.getResource();
    }

    @Override
    public Set<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    @Override
    public PublishedType getType() {
        return delegate.getType();
    }

    @Override
    public AttributionInfo getAttribution() {
        return delegate.getAttribution();
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
    public void setDefaultStyle(StyleInfo defaultStyle) {
        delegate.setDefaultStyle(defaultStyle);
    }

    @Override
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public void setLegend(LegendInfo legend) {
        delegate.setLegend(legend);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setPath(String path) {
        delegate.setPath(path);
    }

    @Override
    public void setResource(ResourceInfo resource) {
        delegate.setResource(resource);
    }

    @Override
    public void setType(PublishedType type) {
        delegate.setType(type);
    }

    @Override
    public void setAttribution(AttributionInfo attr) {
        delegate.setAttribution(attr);
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(delegate)
                .append(']')
                .toString();
    }

    @Override
    public void setQueryable(boolean _queryableEnabled) {
        delegate.setQueryable(_queryableEnabled);
    }

    @Override
    public boolean isQueryable() {
        return delegate.isQueryable();
    }

    @Override
    public void setOpaque(boolean _opaqueEnabled) {
        delegate.setOpaque(_opaqueEnabled);
    }

    @Override
    public boolean isOpaque() {
        return delegate.isOpaque();
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
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return delegate.getAuthorityURLs();
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return delegate.getIdentifiers();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public void setTitle(String title) {
        delegate.setTitle(title);
    }

    @Override
    public String getAbstract() {
        return delegate.getAbstract();
    }

    @Override
    public void setAbstract(String abstractTxt) {
        delegate.setAbstract(abstractTxt);
    }

    @Override
    public WMSInterpolation getDefaultWMSInterpolationMethod() {
        return delegate.getDefaultWMSInterpolationMethod();
    }

    @Override
    public void setDefaultWMSInterpolationMethod(WMSInterpolation interpolationMethod) {
        delegate.setDefaultWMSInterpolationMethod(interpolationMethod);
    }

    @Override
    public Date getDateModified() {
        return delegate.getDateModified();
    }

    @Override
    public Date getDateCreated() {
        return delegate.getDateCreated();
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        delegate.setDateCreated(dateCreated);
    }

    @Override
    public void setDateModified(Date dateModified) {
        delegate.setDateModified(dateModified);
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
