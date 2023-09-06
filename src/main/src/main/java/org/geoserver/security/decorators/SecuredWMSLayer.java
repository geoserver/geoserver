/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.GeneralBounds;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wms.xml.Dimension;
import org.geotools.ows.wms.xml.Extent;

/**
 * A {@link Layer} wrapper carrying around the wrapper policy so that {@link SecuredWebMapServer}
 * can apply it while performing the requests
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredWMSLayer extends Layer {
    Layer delegate;

    WrapperPolicy policy;

    public SecuredWMSLayer(Layer delegate, WrapperPolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    public WrapperPolicy getPolicy() {
        return policy;
    }

    @Override
    public boolean isQueryable() {
        if (policy.getLimits() instanceof WMSAccessLimits) {
            WMSAccessLimits wl = (WMSAccessLimits) policy.getLimits();
            if (!wl.isAllowFeatureInfo()) {
                return false;
            }
        }
        return delegate.isQueryable();
    }

    @Override
    public String toString() {
        return "SecuredLayer - " + delegate.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
        result = prime * result + ((policy == null) ? 0 : policy.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SecuredWMSLayer other = (SecuredWMSLayer) obj;
        if (delegate == null) {
            if (other.delegate != null) return false;
        } else if (!delegate.equals(other.delegate)) return false;
        if (policy == null) {
            if (other.policy != null) return false;
        } else if (!policy.equals(other.policy)) return false;
        return true;
    }

    // --------------------------------------------------------------------------------------
    // Purely delegated methods
    // --------------------------------------------------------------------------------------

    @Override
    public void addChildren(Layer child) {
        delegate.addChildren(child);
    }

    @Override
    public void clearCache() {
        delegate.clearCache();
    }

    @Override
    public int compareTo(Layer layer) {
        return delegate.compareTo(layer);
    }

    @Override
    public String get_abstract() {
        return delegate.get_abstract();
    }

    @Override
    public Map<String, CRSEnvelope> getBoundingBoxes() {
        return delegate.getBoundingBoxes();
    }

    @Override
    public Layer[] getChildren() {
        return delegate.getChildren();
    }

    @Override
    public Dimension getDimension(String name) {
        return delegate.getDimension(name);
    }

    @Override
    public Map<String, Dimension> getDimensions() {
        return delegate.getDimensions();
    }

    @Override
    public GeneralBounds getEnvelope(CoordinateReferenceSystem crs) {
        return delegate.getEnvelope(crs);
    }

    @Override
    public Extent getExtent(String name) {
        return delegate.getExtent(name);
    }

    @Override
    public Map<String, Extent> getExtents() {
        return delegate.getExtents();
    }

    @Override
    public String[] getKeywords() {
        return delegate.getKeywords();
    }

    @Override
    public CRSEnvelope getLatLonBoundingBox() {
        return delegate.getLatLonBoundingBox();
    }

    @Override
    public List<CRSEnvelope> getLayerBoundingBoxes() {
        return delegate.getLayerBoundingBoxes();
    }

    @Override
    public List<Layer> getLayerChildren() {
        return delegate.getLayerChildren();
    }

    @Override
    public List<Dimension> getLayerDimensions() {
        return delegate.getLayerDimensions();
    }

    @Override
    public List<Extent> getLayerExtents() {
        return delegate.getLayerExtents();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Layer getParent() {
        return delegate.getParent();
    }

    @Override
    public double getScaleDenominatorMax() {
        return delegate.getScaleDenominatorMax();
    }

    @Override
    public double getScaleDenominatorMin() {
        return delegate.getScaleDenominatorMin();
    }

    public double getScaleHintMax() {
        return delegate.getScaleDenominatorMax();
    }

    public double getScaleHintMin() {
        return delegate.getScaleDenominatorMin();
    }

    @Override
    public Set<String> getSrs() {
        return delegate.getSrs();
    }

    @Override
    public List<StyleImpl> getStyles() {
        return delegate.getStyles();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public void set_abstract(String abstract1) {
        delegate.set_abstract(abstract1);
    }

    @Override
    public void setBoundingBoxes(CRSEnvelope boundingBox) {
        delegate.setBoundingBoxes(boundingBox);
    }

    @Override
    public void setBoundingBoxes(Map<String, CRSEnvelope> boundingBoxes) {
        delegate.setBoundingBoxes(boundingBoxes);
    }

    @Override
    public void setChildren(Layer[] childrenArray) {
        delegate.setChildren(childrenArray);
    }

    @Override
    public void setDimensions(Collection<Dimension> dimensionList) {
        delegate.setDimensions(dimensionList);
    }

    @Override
    public void setDimensions(Dimension dimension) {
        delegate.setDimensions(dimension);
    }

    @Override
    public void setDimensions(Map<String, Dimension> dimensionMap) {
        delegate.setDimensions(dimensionMap);
    }

    @Override
    public void setExtents(Collection<Extent> extentList) {
        delegate.setExtents(extentList);
    }

    @Override
    public void setExtents(Extent extent) {
        delegate.setExtents(extent);
    }

    @Override
    public void setExtents(Map<String, Extent> extentMap) {
        delegate.setExtents(extentMap);
    }

    @Override
    public void setKeywords(String[] keywords) {
        delegate.setKeywords(keywords);
    }

    @Override
    public void setLatLonBoundingBox(CRSEnvelope latLonBoundingBox) {
        delegate.setLatLonBoundingBox(latLonBoundingBox);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setParent(Layer parentLayer) {
        delegate.setParent(parentLayer);
    }

    @Override
    public void setQueryable(boolean queryable) {
        delegate.setQueryable(queryable);
    }

    @Override
    public void setScaleDenominatorMax(double scaleDenominatorMax) {
        delegate.setScaleDenominatorMax(scaleDenominatorMax);
    }

    @Override
    public void setScaleDenominatorMin(double scaleDenominatorMin) {
        delegate.setScaleDenominatorMin(scaleDenominatorMin);
    }

    public void setScaleHintMax(double scaleHintMax) {
        delegate.setScaleDenominatorMax(scaleHintMax);
    }

    public void setScaleHintMin(double scaleHintMin) {
        delegate.setScaleDenominatorMin(scaleHintMin);
    }

    @Override
    public void setSrs(Set<String> srs) {
        delegate.setSrs(srs);
    }

    @Override
    public void setStyles(List<StyleImpl> styles) {
        delegate.setStyles(styles);
    }

    @Override
    public void setTitle(String title) {
        delegate.setTitle(title);
    }
}
