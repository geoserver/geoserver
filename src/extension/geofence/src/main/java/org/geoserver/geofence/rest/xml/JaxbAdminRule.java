/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.geofence.core.model.AdminRule;
import org.geoserver.geofence.core.model.IPAddressRange;
import org.geoserver.geofence.core.model.enums.AdminGrantType;

@XmlRootElement(name = "AdminRule")
public class JaxbAdminRule {

    private Long id;

    private Long priority;

    private String userName;

    private String roleName;

    private String addressRange;

    private String workspace;

    private String access;

    public JaxbAdminRule() {}

    public JaxbAdminRule(AdminRule rule) {
        id = rule.getId();
        priority = rule.getPriority();
        userName = rule.getUsername();
        roleName = rule.getRolename();
        addressRange =
                rule.getAddressRange() == null ? null : rule.getAddressRange().getCidrSignature();
        workspace = rule.getWorkspace();
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
    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public AdminRule toRule() {
        AdminRule rule = new AdminRule();
        if (getPriority() != null) {
            rule.setPriority(getPriority());
        }
        rule.setAccess(AdminGrantType.valueOf(getAccess()));
        rule.setUsername(getUserName());
        rule.setRolename(getRoleName());
        rule.setAddressRange(
                getAddressRange() == null ? null : new IPAddressRange(getAddressRange()));
        rule.setWorkspace(getWorkspace());
        rule.setId(id);
        return rule;
    }

    public AdminRule toRule(AdminRule rule) {
        if (getPriority() != null) {
            rule.setPriority(getPriority());
        }
        if (getAccess() != null) {
            rule.setAccess(AdminGrantType.valueOf(getAccess()));
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
        if (getWorkspace() != null) {
            rule.setWorkspace(convertAny(getWorkspace()));
        }
        if (id != null) {
            rule.setId(id);
        }
        return rule;
    }

    protected static String convertAny(String s) {
        if ("".equals(s) || "*".equals(s)) return null;
        else return s;
    }
}
