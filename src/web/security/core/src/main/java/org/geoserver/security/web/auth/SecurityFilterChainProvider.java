/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.springframework.util.StringUtils;

/**
 * Provider for {@link RequestFilterChain} objects
 *
 * @author Christian Mueller
 */
public class SecurityFilterChainProvider extends GeoServerDataProvider<RequestFilterChain> {

    SecurityManagerConfig config;

    public SecurityFilterChainProvider(SecurityManagerConfig config) {
        this.config = config;
    }

    private static final long serialVersionUID = 1L;

    /** name of the config */
    public static final Property<RequestFilterChain> NAME =
            new BeanProperty<RequestFilterChain>("name", "name");

    public static Property<RequestFilterChain> POSITION =
            new PropertyPlaceholder<RequestFilterChain>("position");
    public static Property<RequestFilterChain> REMOVE =
            new PropertyPlaceholder<RequestFilterChain>("remove");

    public static final Property<RequestFilterChain> DISABLED =
            new BeanProperty<RequestFilterChain>("disabled", "disabled");
    public static final Property<RequestFilterChain> ALLOWSESSIONCREATION =
            new BeanProperty<RequestFilterChain>("allowSessionCreation", "allowSessionCreation");
    public static final Property<RequestFilterChain> REQUIRESSL =
            new BeanProperty<RequestFilterChain>("requireSSL", "requireSSL");
    public static final Property<RequestFilterChain> MATCHHTTPMETHOD =
            new BeanProperty<RequestFilterChain>("matchHTTPMethod", "matchHTTPMethod");

    public static final Property<RequestFilterChain> PATTERNS =
            new AbstractProperty<RequestFilterChain>("patternString") {
                @Override
                public Object getPropertyValue(RequestFilterChain item) {
                    return StringUtils.collectionToCommaDelimitedString(item.getPatterns());
                }
            };

    public static final Property<RequestFilterChain> HTTPMETHODS =
            new AbstractProperty<RequestFilterChain>("httpMethods") {
                @Override
                public Object getPropertyValue(RequestFilterChain item) {
                    return StringUtils.collectionToCommaDelimitedString(item.getHttpMethods());
                }
            };

    public static final Property<RequestFilterChain> HASROLEFILTER =
            new AbstractProperty<RequestFilterChain>("hasRoleFilter") {
                @Override
                public Object getPropertyValue(RequestFilterChain item) {
                    return StringUtils.hasLength(item.getRoleFilterName());
                }
            };

    @Override
    protected List<Property<RequestFilterChain>> getProperties() {
        List<Property<RequestFilterChain>> result = new ArrayList<Property<RequestFilterChain>>();
        result.add(POSITION);
        result.add(NAME);
        result.add(PATTERNS);
        result.add(MATCHHTTPMETHOD);
        result.add(HTTPMETHODS);
        result.add(DISABLED);
        result.add(ALLOWSESSIONCREATION);
        result.add(REQUIRESSL);
        result.add(HASROLEFILTER);
        result.add(REMOVE);
        return result;
    }

    @Override
    protected List<RequestFilterChain> getItems() {
        return config.getFilterChain().getRequestChains();
    }
}
