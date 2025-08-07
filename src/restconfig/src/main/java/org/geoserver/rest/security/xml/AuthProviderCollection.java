/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.config.SecurityAuthProviderConfig;

@XStreamAlias("authProviders")
public class AuthProviderCollection {

    private List<SecurityAuthProviderConfig> providers = new ArrayList<>();

    public AuthProviderCollection() {}

    public AuthProviderCollection(List<SecurityAuthProviderConfig> p) {
        providers = p;
    }

    public List<SecurityAuthProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(List<SecurityAuthProviderConfig> p) {
        providers = p;
    }

    public SecurityAuthProviderConfig first() {
        return providers == null || providers.isEmpty() ? null : providers.get(0);
    }
}
