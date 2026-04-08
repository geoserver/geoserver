/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.config;

import java.io.Serializable;
import lombok.Getter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.plugin.accessmanager.AuthorizationServiceConfig;
import org.geoserver.acl.plugin.web.support.ApplicationContextSupport;
import org.geoserver.web.wicket.model.ExtPropertyModel;

@SuppressWarnings("serial")
public class ACLServiceConfigPageModel implements Serializable {

    private @Getter CompoundPropertyModel<AuthorizationServiceConfig> configModel;

    private @Getter ExtPropertyModel<String> serviceUrl;

    private @Getter IModel<Boolean> allowRemoteAndInlineLayers;
    private @Getter IModel<Boolean> grantWriteToWorkspacesToAuthenticatedUsers;

    private @Getter IModel<Boolean> useRolesToFilter;

    public static ACLServiceConfigPageModel newInstance() {
        return new ACLServiceConfigPageModel();
    }

    ACLServiceConfigPageModel() {
        AuthorizationServiceConfig config = ApplicationContextSupport.getBeanOfType(AuthorizationServiceConfig.class);
        configModel = new CompoundPropertyModel<>(config);
        serviceUrl = new ExtPropertyModel<>(configModel, "serviceUrl");
        allowRemoteAndInlineLayers = new PropertyModel<>(configModel, "allowRemoteAndInlineLayers");
        grantWriteToWorkspacesToAuthenticatedUsers =
                new PropertyModel<>(configModel, "grantWriteToWorkspacesToAuthenticatedUsers");
        useRolesToFilter = new PropertyModel<>(configModel, "useRolesToFilter");
    }

    /** @return {@code true} if the ACL service runs in-process, {@code false} if it hits a remote service */
    public boolean isInternal() {
        return false;
    }

    public void testConnection() throws Exception {
        AuthorizationServiceConfig newConfig = configModel.getObject();
        testConfig(newConfig);
    }

    public void testConfig(AuthorizationServiceConfig config) {
        AuthorizationService service = ApplicationContextSupport.getBeanOfType(AuthorizationService.class);
        service.getMatchingRules(AccessRequest.builder().build());
    }
}
