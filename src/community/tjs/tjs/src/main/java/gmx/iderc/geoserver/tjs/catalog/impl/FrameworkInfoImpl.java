/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogVisitor;
import org.geoserver.catalog.*;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class FrameworkInfoImpl extends TJSCatalogObjectImpl implements FrameworkInfo, Serializable {

    String featureTypeId;
    String uri;
    Date referenceDate;
    int version;
    String documentation;
    String organization;
    String frameworkKey;
    String frameworkKeyTitle;
    String relatedWMS;
    String wmsLayerId;

    transient private FeatureTypeInfo featureType;
    transient private LayerInfo wmsLayer;

    public FrameworkInfoImpl(TJSCatalog catalog) {
        super(catalog);
    }

    @Override
    public void loadDefault() {
        setId(TJSCatalogFactoryImpl.getIdForObject(this));
        setName("Default Framework Info");
        setDescription("Default Framework for testing propose.");
//        setWorkspace(getDefaultWorkspace());
        DatasetInfoImpl dsi = new DatasetInfoImpl(getCatalog());
        dsi.loadDefault();
    }

    //TODO: sobreescribir aqui no hace falta?
    // Alvaro Javier Fuentes Suarez, 11:30 p.m. 1/8/13
    @Override
    public void accept(TJSCatalogVisitor visitor) {
        visitor.visit((FrameworkInfo) this);
    }

    public String getUri() {
        return uri;
    }

    public FeatureTypeInfo getFeatureType() {
        if (featureType != null) {
            return featureType;
        }
        Catalog gsCatalog = getCatalog().getGeoserverCatalog();
        if ((gsCatalog != null) && (featureTypeId != null)) {
            featureType = gsCatalog.getFeatureType(featureTypeId);
            return featureType;
        } else {
            return null;
        }
    }

    private void updateUri() {
        if (name != null) {
            this.uri = getFeatureType().getNamespace().getURI().concat("/").concat(name);
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        updateUri();
    }

    public void setFeatureType(FeatureTypeInfo featureType) {
        this.featureType = featureType;
        this.featureTypeId = featureType.getId();
        updateUri();
    }

    public Date getRefererenceDate() {
        return referenceDate;
    }

    public void setRefererenceDate(Date date) {
        this.referenceDate = date;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String uri) {
        this.documentation = uri;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    private List<AttributeTypeInfo> getAttributes(FeatureTypeInfo featureTypeInfo) {
        try {
            Catalog geocatalog = getCatalog().getGeoserverCatalog();
            ResourcePool resourcePool = geocatalog.getResourcePool();
            return resourcePool.getAttributes(featureTypeInfo);
        } catch (Exception ex) {
            Logger.getLogger(FrameworkInfoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return featureTypeInfo.getAttributes();
    }

    private AttributeTypeInfo getAttributeByName(String attName) {
        FeatureTypeInfo fti = getFeatureType();
        if (fti != null) {
            for (AttributeTypeInfo att : getAttributes(fti)) {
                if (att.getName().equals(attName)) {
                    if (att.getLength() == null){
                        att.setLength(new Integer(0));
                    }
                    return att;
                }
            }
        }
        return null;
    }

    public AttributeTypeInfo getFrameworkKey() {
        return getAttributeByName(frameworkKey);
    }

    public void setFrameworkKey(AttributeTypeInfo frameworkKey) {
        this.frameworkKey = frameworkKey.getName();
    }

    public AttributeTypeInfo getFrameworkKeyTitle() {
        return getAttributeByName(frameworkKeyTitle);
    }

    public void setFrameworkKeyTitle(AttributeTypeInfo frameworkKeyTitle) {
        this.frameworkKeyTitle = frameworkKeyTitle.getName();
    }

    public ReferencedEnvelope getBoundingCoordinates() {
        try {
            return getFeatureType().boundingBox();
        } catch (Exception ex) {
            Logger.getLogger(FrameworkInfoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public LayerInfo getAssociatedWMS() {
        if (wmsLayer != null) {
            return wmsLayer;
        }
        if (wmsLayerId == null) {
            return null;
        }
        Catalog geocatalog = getCatalog().getGeoserverCatalog();
        wmsLayer = (LayerInfo) geocatalog.getLayer(wmsLayerId);
        return wmsLayer;
    }

    public void setAssociatedWMS(LayerInfo wmsLayer) {
        this.wmsLayer = wmsLayer;
        this.wmsLayerId = wmsLayer.getId();
    }

}
