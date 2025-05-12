/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Abstract base class for all security filter configurations.
 *
 * @author mcr
 */
@XmlSeeAlso({
    AnonymousAuthenticationFilterConfig.class,
    BasicAuthenticationFilterConfig.class,
    CredentialsFromRequestHeaderFilterConfig.class,
    DigestAuthenticationFilterConfig.class,
    ExceptionTranslationFilterConfig.class,
    LogoutFilterConfig.class,

    // PreAuthenticatedUserNameFilterConfig types
    //        J2eeAuthenticationBaseFilterConfig.class,
    //        RequestHeaderAuthenticationFilterConfig.class,

    RememberMeAuthenticationFilterConfig.class,
    RoleFilterConfig.class,
    SecurityContextPersistenceFilterConfig.class,
    SecurityInterceptorFilterConfig.class,
    SSLFilterConfig.class,
    UsernamePasswordAuthenticationFilterConfig.class
})
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnonymousAuthenticationFilterConfig.class),
    @JsonSubTypes.Type(value = BasicAuthenticationFilterConfig.class),
    @JsonSubTypes.Type(value = CredentialsFromRequestHeaderFilterConfig.class),
    @JsonSubTypes.Type(value = DigestAuthenticationFilterConfig.class),
    @JsonSubTypes.Type(value = ExceptionTranslationFilterConfig.class),
    @JsonSubTypes.Type(value = LogoutFilterConfig.class),

    // PreAuthenticatedUserNameFilterConfig types
    @JsonSubTypes.Type(value = J2eeAuthenticationBaseFilterConfig.class),
    @JsonSubTypes.Type(value = RequestHeaderAuthenticationFilterConfig.class),
    @JsonSubTypes.Type(value = RememberMeAuthenticationFilterConfig.class),
    @JsonSubTypes.Type(value = RoleFilterConfig.class),
    @JsonSubTypes.Type(value = SecurityContextPersistenceFilterConfig.class),
    @JsonSubTypes.Type(value = SecurityInterceptorFilterConfig.class),
    @JsonSubTypes.Type(value = SSLFilterConfig.class),
    @JsonSubTypes.Type(value = UsernamePasswordAuthenticationFilterConfig.class)
})
// Add all concrete types here
public abstract class SecurityFilterConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;

    /**
     * Determines if the filter provides an {@link AuthenticationEntryPoint}.
     *
     * <p>If <code>true</code>, the corresponding {@link GeoServerSecurityFilter} class must return non-null from the
     * method {@link GeoServerSecurityFilter#getAuthenticationEntryPoint()}.
     *
     * @return true if the corresponding filter provides an {@link AuthenticationEntryPoint} object.
     */
    public boolean providesAuthenticationEntryPoint() {
        return false;
    }
}
