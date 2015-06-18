/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.GrantType;

@XmlRootElement(name = "Rule")
public class JaxbRule {

    private Long id;

    private Long priority;

    private String userName;

    private String roleName;

    private String workspace;

    private String layer;

    private String service;

    private String request;

    private String access;

    public JaxbRule() {

    }

    public JaxbRule(Rule rule) {
        id = rule.getId();
        priority = rule.getPriority();
        userName = rule.getUsername();
        roleName = rule.getRolename();
        workspace = rule.getWorkspace();
        layer = rule.getLayer();
        service = rule.getService() == null ? null : rule.getService().toUpperCase();
        request = rule.getRequest();
        access = rule.getAccess().toString();
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

    public Rule toRule() {
        Rule rule = new Rule();
        if (getPriority() != null) {
            rule.setPriority(getPriority());
        }
        rule.setAccess(GrantType.valueOf(getAccess()));
        rule.setUsername(getUserName());
        rule.setRolename(getRoleName());
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
