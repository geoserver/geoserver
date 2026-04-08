/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.geoserver.acl.authorization.AccessRequest;

@Data
@SuppressWarnings("serial")
public class MutableAccessRequest implements Serializable {

    private String user;
    private final Set<String> roles = new HashSet<>();
    private String sourceAddress;

    private String service;
    private String request;
    private String subfield;

    private String workspace;
    private String layer;

    public void setRoles(Set<String> roles) {
        this.roles.clear();
        if (roles != null) this.roles.addAll(roles);
    }

    public AccessRequest toRequest() {
        return AccessRequest.builder()
                .layer(layer)
                .request(request)
                .roles(roles)
                .service(service)
                .sourceAddress(sourceAddress)
                .subfield(subfield)
                .user(user)
                .workspace(workspace)
                .build();
    }
}
