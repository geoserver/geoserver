/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XStreamAlias("authProviderList")
public class AuthProviderList {
    @XStreamImplicit
    protected List<AuthProvider> providers;

    /*
     * Constructor for serialisation.
     * If the serialisation does not initialise the providers we have assigned a default to prevent an NPE. When
     * debugging if an empty list of providers is found this may be a bug in serialisation.
     */
    public AuthProviderList() {
        providers = new ArrayList<>();
    }

    public AuthProviderList(List<AuthProvider> providers) {
        this.providers = providers;
    }

    public List<AuthProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }
}
