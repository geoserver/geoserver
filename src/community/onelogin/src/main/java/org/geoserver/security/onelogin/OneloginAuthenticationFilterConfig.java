/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.onelogin;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;

/**
 * Configuration for OneLogin authentication
 * 
 * @author Xandros
 */

public class OneloginAuthenticationFilterConfig extends PreAuthenticatedUserNameFilterConfig {

    private static final long serialVersionUID = 1199751476823173800L;

    private String entityId;

    private String metadataURL;

    private Boolean wantAssertionSigned = false;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMetadataURL() {
        return metadataURL;
    }

    public void setMetadataURL(String metadataURL) {
        this.metadataURL = metadataURL;
    }

    public Boolean getWantAssertionSigned() {
        return wantAssertionSigned;
    }

    public void setWantAssertionSigned(Boolean wantAssertionSigned) {
        this.wantAssertionSigned = wantAssertionSigned;
    }

}
