/* (c) 2015 - 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest.xml;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.geoserver.geofence.core.model.IPAddressRange;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.AccessType;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.LayerType;

import com.vividsolutions.jts.geom.MultiPolygon;

@XmlRootElement(name = "Rule")
public class JaxbRule {
    
    /**
     * Specification for "LIMIT" rules.
     *
     */
    public static class Limits {
        
        private MultiPolygon allowedArea;
        
        private String catalogMode;

        @XmlElement
        @XmlJavaTypeAdapter(MultiPolygonAdapter.class)
        public MultiPolygon getAllowedArea() {
            return allowedArea;
        }

        public void setAllowedArea(MultiPolygon allowedArea) {
            this.allowedArea = allowedArea;
        }

        @XmlElement
        public String getCatalogMode() {
            return catalogMode;
        }

        public void setCatalogMode(String catalogMode) {
            this.catalogMode = catalogMode;
        }
        
        public RuleLimits toRuleLimits(RuleLimits ruleLimits) {
            if (ruleLimits == null) {
                ruleLimits = new RuleLimits();
            }
            if (getAllowedArea() != null) {
                ruleLimits.setAllowedArea(getAllowedArea());
            }
            if (getCatalogMode() != null) {
                ruleLimits.setCatalogMode(CatalogMode.valueOf(getCatalogMode().toUpperCase()));
            }
            return ruleLimits;
        }
        
    }
    
    /**
     * Access specification for a Layer Attribute
     *
     */
    public static class LayerAttribute {
        private String name;
        private String dataType;
        private String accessType;
        
        public LayerAttribute() {
            
        }
        
        public LayerAttribute(org.geoserver.geofence.core.model.LayerAttribute att) {
            this.name = att.getName();
            this.dataType = att.getDatatype();
            this.accessType = att.getAccess().toString();
        }
        
        @XmlElement
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        
        @XmlElement
        public String getDataType() {
            return dataType;
        }
        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        @XmlElement
        public String getAccessType() {
            return accessType;
        }
        public void setAccessType(String accessType) {
            this.accessType = accessType;
        }
        
        public org.geoserver.geofence.core.model.LayerAttribute 
            toLayerAttribute() {
            org.geoserver.geofence.core.model.LayerAttribute att = new
                    org.geoserver.geofence.core.model.LayerAttribute();
            if (accessType != null) {
                att.setAccess(AccessType.valueOf(accessType.toUpperCase()));
            }            
            att.setDatatype(dataType);
            att.setName(name);
            return att;
        }
        
        
    }
    
    /**
     * Details for layer access.
     *
     */
    public static class LayerDetails {
        private String layerType;
        private String defaultStyle;
        private String cqlFilterRead;
        private String cqlFilterWrite;
        private MultiPolygon allowedArea;
        private String catalogMode;
        private Set<String> allowedStyles = new HashSet<String>();
        private Set<LayerAttribute> layerAttributes = new HashSet<LayerAttribute>();
        
        @XmlElement
        public String getLayerType() {
            return layerType;
        }
        public void setLayerType(String layerType) {
            this.layerType = layerType;
        }

        @XmlElement
        public String getDefaultStyle() {
            return defaultStyle;
        }
        public void setDefaultStyle(String defaultStyle) {
            this.defaultStyle = defaultStyle;
        }

        @XmlElement
        public String getCqlFilterRead() {
            return cqlFilterRead;
        }
        public void setCqlFilterRead(String cqlFilterRead) {
            this.cqlFilterRead = cqlFilterRead;
        }

        @XmlElement
        public String getCqlFilterWrite() {
            return cqlFilterWrite;
        }
        public void setCqlFilterWrite(String cqlFilterWrite) {
            this.cqlFilterWrite = cqlFilterWrite;
        }

        @XmlElement
        @XmlJavaTypeAdapter(MultiPolygonAdapter.class)
        public MultiPolygon getAllowedArea() {
            return allowedArea;
        }
        public void setAllowedArea(MultiPolygon allowedArea) {
            this.allowedArea = allowedArea;
        }

        @XmlElement
        public String getCatalogMode() {
            return catalogMode;
        }
        public void setCatalogMode(String catalogMode) {
            this.catalogMode = catalogMode;
        }

        @XmlElement(name = "allowedStyle")
        public Set<String> getAllowedStyles() {
            return allowedStyles;
        }
        public void setAllowedStyles(Set<String> allowedStyles) {
            this.allowedStyles = allowedStyles;
        }

        @XmlElement(name = "attribute")
        public Set<LayerAttribute> getAttributes() {
            return layerAttributes;
        }
        public void setAttributes(Set<LayerAttribute> layerAttributes) {
            this.layerAttributes = layerAttributes;
        }
        
        public org.geoserver.geofence.core.model.LayerDetails 
            toLayerDetails(org.geoserver.geofence.core.model.LayerDetails details) {
            if (details == null) {
                details = new org.geoserver.geofence.core.model.LayerDetails();
            }
            if (layerType != null) {
                details.setType(LayerType.valueOf(layerType.toUpperCase()));
            }
            if (allowedStyles != null) {
                details.getAllowedStyles().addAll(allowedStyles);
            }
            if (allowedArea != null) {
                details.setArea(allowedArea);
            }
            if (layerAttributes != null) {
                for (LayerAttribute att : layerAttributes) {
                    Iterator<org.geoserver.geofence.core.model.LayerAttribute> it = 
                            details.getAttributes().iterator();
                    while (it.hasNext()) {
                        if (it.next().getName().equals(att.getName())) {
                            it.remove();
                            break;
                        }
                    }
                    details.getAttributes().add(att.toLayerAttribute());
                }
            }
            if (catalogMode != null) {
                details.setCatalogMode(CatalogMode.valueOf(catalogMode.toUpperCase()));
            }
            if (cqlFilterRead != null) {
                details.setCqlFilterRead(cqlFilterRead);
            }
            if (cqlFilterWrite != null) {
                details.setCqlFilterWrite(cqlFilterWrite);
            }
            if (defaultStyle != null) {
                details.setDefaultStyle(defaultStyle);
            }
            return details;
        }
    }

    private Long id;

    private Long priority;

    private String userName;

    private String roleName;

    private String addressRange;

    private String workspace;

    private String layer;

    private String service;

    private String request;

    private String access;
    
    private Limits limits;
    
    private LayerDetails layerDetails;

    public JaxbRule() {

    }

    public JaxbRule(Rule rule) {
        id = rule.getId();
        priority = rule.getPriority();
        userName = rule.getUsername();
        roleName = rule.getRolename();
        addressRange = rule.getAddressRange() == null? null : rule.getAddressRange().getCidrSignature();
        workspace = rule.getWorkspace();
        layer = rule.getLayer();
        service = rule.getService() == null ? null : rule.getService().toUpperCase();
        request = rule.getRequest();
        access = rule.getAccess().toString();
        if (rule.getRuleLimits() != null) {
            limits = new Limits();
            limits.setAllowedArea(rule.getRuleLimits().getAllowedArea());
            limits.setCatalogMode(rule.getRuleLimits().getCatalogMode().toString());
        }
        if (rule.getLayerDetails() != null) {
            layerDetails = new LayerDetails();
            layerDetails.setAllowedArea(rule.getLayerDetails().getArea());
            layerDetails.getAllowedStyles().addAll(rule.getLayerDetails().getAllowedStyles());
            layerDetails.setCatalogMode(rule.getLayerDetails().getCatalogMode().toString());
            layerDetails.setCqlFilterRead(rule.getLayerDetails().getCqlFilterRead());
            layerDetails.setCqlFilterWrite(rule.getLayerDetails().getCqlFilterWrite());
            layerDetails.setDefaultStyle(rule.getLayerDetails().getDefaultStyle());
            layerDetails.setLayerType(rule.getLayerDetails().getType().toString());
            for (org.geoserver.geofence.core.model.LayerAttribute att : 
                rule.getLayerDetails().getAttributes()) {
                layerDetails.getAttributes().add(new LayerAttribute(att));
            }
        }
    }

    @XmlAttribute
    public Long getId() {
        return id;
    }

    @XmlElement
    public Long getPriority() {
        return priority;
    }

    public void setPriority(Long priority) {
        this.priority = priority;
    }

    @XmlElement
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @XmlElement
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getAddressRange() {
        return addressRange;
    }

    public void setAddressRange(String addressRange) {
        this.addressRange = addressRange;
    }

    @XmlElement
    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @XmlElement
    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    @XmlElement
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @XmlElement
    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    @XmlElement
    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }
    
    @XmlElement
    public Limits getLimits() {
        return limits;
    }
    
    public void setLimits(Limits limits) {
        this.limits = limits;
    }      

    public LayerDetails getLayerDetails() {
        return layerDetails;
    }

    public void setLayerDetails(LayerDetails layerDetails) {
        this.layerDetails = layerDetails;
    }

    public Rule toRule() {
        Rule rule = new Rule();
        if (getPriority() != null) {
            rule.setPriority(getPriority());
        }
        rule.setAccess(GrantType.valueOf(getAccess()));
        rule.setUsername(getUserName());
        rule.setRolename(getRoleName());
        rule.setAddressRange(getAddressRange() == null? null: new IPAddressRange(getAddressRange()));
        rule.setService(getService());
        rule.setRequest(getRequest());
        rule.setWorkspace(getWorkspace());
        rule.setLayer(getLayer());
        rule.setId(id);
        return rule;
    }

    public Rule toRule(Rule rule) {
        if (getPriority() != null) {
            rule.setPriority(getPriority());
        }
        if (getAccess() != null) {
            rule.setAccess(GrantType.valueOf(getAccess()));
        }
        if (getUserName() != null) {
            rule.setUsername(convertAny(getUserName()));
        }
        if (getRoleName() != null) {
            rule.setRolename(convertAny(getRoleName()));
        }
        if (getAddressRange() != null) {
            rule.setAddressRange(new IPAddressRange(getAddressRange()));
        }
        if (getService() != null) {
            rule.setService(convertAny(getService()));
        }
        if (getRequest() != null) {
            rule.setRequest(convertAny(getRequest()));
        }
        if (getWorkspace() != null) {
            rule.setWorkspace(convertAny(getWorkspace()));
        }
        if (getLayer() != null) {
            rule.setLayer(convertAny(getLayer()));
        }
        if (id != null) {
            rule.setId(id);
        }
        return rule;
    }
    
    protected static String convertAny(String s) {
        if ("".equals(s) || "*".equals(s))
            return null;
        else return s;
    }
}
