/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

/**
 * Configuration for security interceptor filters
 *
 * @author mcr
 */

/*
<bean id="filterSecurityRestInterceptor"
class="org.springframework.security.web.access.intercept.FilterSecurityInterceptor">
<property name="authenticationManager" ref="authenticationManager" />
<property name="accessDecisionManager">
  <bean class="org.springframework.security.access.vote.AffirmativeBased">
    <property name="allowIfAllAbstainDecisions" value="false" />
    <property name="decisionVoters">
      <list>
        <bean class="org.springframework.security.access.vote.RoleVoter" />
        <bean class="org.springframework.security.access.vote.AuthenticatedVoter" />
      </list>
    </property>
  </bean>
</property>
<property name="securityMetadataSource" ref="restFilterDefinitionMap"/>
</bean>
*/

public class SecurityInterceptorFilterConfig extends SecurityFilterConfig {

    private static final long serialVersionUID = 1L;
    private boolean allowIfAllAbstainDecisions;
    private String securityMetadataSource;

    public boolean isAllowIfAllAbstainDecisions() {
        return allowIfAllAbstainDecisions;
    }

    public void setAllowIfAllAbstainDecisions(boolean allowIfAllAbstainDecisions) {
        this.allowIfAllAbstainDecisions = allowIfAllAbstainDecisions;
    }

    public String getSecurityMetadataSource() {
        return securityMetadataSource;
    }

    public void setSecurityMetadataSource(String securityMetadataSource) {
        this.securityMetadataSource = securityMetadataSource;
    }
}
