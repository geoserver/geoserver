/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.Serial;
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

    @Serial
    private static final long serialVersionUID = 1L;

    /** name of the config */
    public static final Property<RequestFilterChain> NAME = new BeanProperty<>("name", "name");

    public static Property<RequestFilterChain> POSITION = new PropertyPlaceholder<>("position");
    public static Property<RequestFilterChain> REMOVE = new PropertyPlaceholder<>("remove");

    public static final Property<RequestFilterChain> DISABLED = new BeanProperty<>("disabled", "disabled");
    public static final Property<RequestFilterChain> ALLOWSESSIONCREATION =
            new BeanProperty<>("allowSessionCreation", "allowSessionCreation");
    public static final Property<RequestFilterChain> REQUIRESSL = new BeanProperty<>("requireSSL", "requireSSL");
    public static final Property<RequestFilterChain> MATCHHTTPMETHOD =
            new BeanProperty<>("matchHTTPMethod", "matchHTTPMethod");

    public static final Property<RequestFilterChain> PATTERNS = new AbstractProperty<>("patternString") {
        @Override
        public Object getPropertyValue(RequestFilterChain item) {
            return StringUtils.collectionToCommaDelimitedString(item.getPatterns());
        }
    };

    public static final Property<RequestFilterChain> HTTPMETHODS = new AbstractProperty<>("httpMethods") {
        @Override
        public Object getPropertyValue(RequestFilterChain item) {
            return StringUtils.collectionToCommaDelimitedString(item.getHttpMethods());
        }
    };

    public static final Property<RequestFilterChain> HASROLEFILTER = new AbstractProperty<>("hasRoleFilter") {
        @Override
        public Object getPropertyValue(RequestFilterChain item) {
            return StringUtils.hasLength(item.getRoleFilterName());
        }
    };

    @Override
    protected List<Property<RequestFilterChain>> getProperties() {
        List<Property<RequestFilterChain>> result = new ArrayList<>();
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
