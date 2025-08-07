/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("authProviders")
public class AuthProviderCollection {

    private List<AuthProvider> authProviders = new ArrayList<>();

    public AuthProviderCollection() {}

    public AuthProviderCollection(List<AuthProvider> providers) {
        this.authProviders = providers;
    }

    public List<AuthProvider> getAuthProviders() {
        return authProviders;
    }

    public void setAuthProviders(List<AuthProvider> authProviders) {
        this.authProviders = authProviders;
    }
}
