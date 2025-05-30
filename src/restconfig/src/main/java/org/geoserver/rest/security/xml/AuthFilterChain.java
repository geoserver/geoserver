/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import static java.util.stream.Collectors.toSet;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.CannotMakeChain;
import org.geoserver.security.HTTPMethod;
import org.geoserver.security.RequestFilterChain;

@XStreamAlias("filterChain")
public class AuthFilterChain {
    private String name;
    private String className;
    private List<String> patterns;
    private List<String> filters;
    private boolean disabled;
    private boolean allowSessionCreation;
    private boolean requireSSL;
    private boolean matchHTTPMethod;
    private Set<String> httpMethods;
    private String roleFilterName;

    private int position;

    public AuthFilterChain() {}

    public AuthFilterChain(RequestFilterChain requestFilterChain) {
        this.name = requestFilterChain.getName();
        this.className = requestFilterChain.getClass().getName();
        this.patterns = requestFilterChain.getPatterns();
        this.filters = requestFilterChain.getFilterNames();
        this.disabled = requestFilterChain.isDisabled();
        this.allowSessionCreation = requestFilterChain.isAllowSessionCreation();
        this.requireSSL = requestFilterChain.isRequireSSL();
        this.matchHTTPMethod = requestFilterChain.isMatchHTTPMethod();
        this.httpMethods = requestFilterChain.getHttpMethods().stream()
                .map(HTTPMethod::name)
                .collect(toSet());
        this.roleFilterName = requestFilterChain.getRoleFilterName();
    }

    public RequestFilterChain toRequestFilterChain() {
        RequestFilterChain filterChain = createInstance(patterns);
        filterChain.setName(name);
        filterChain.setPatterns(patterns);
        filterChain.setFilterNames(filters);
        filterChain.setDisabled(disabled);
        filterChain.setAllowSessionCreation(allowSessionCreation);
        filterChain.setRequireSSL(requireSSL);
        filterChain.setMatchHTTPMethod(matchHTTPMethod);
        if (httpMethods != null) {
            filterChain.setHttpMethods(
                    httpMethods.stream().map(HTTPMethod::valueOf).collect(toSet()));
        }
        filterChain.setRoleFilterName(this.roleFilterName);
        return filterChain;
    }

    private RequestFilterChain createInstance(List<String> patterns) {
        try {
            Class<?> clazz = Class.forName(className);
            Optional<Constructor<?>> possibleConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                    .filter(matchesStringArrayConstructor())
                    .findFirst();
            if (possibleConstructor.isPresent()) {
                return (RequestFilterChain)
                        possibleConstructor.get().newInstance(new Object[] {patterns.toArray(new String[0])});
            }
            throw new CannotMakeChain(
                    className,
                    new InstantiationException("Cannot find a constructor with a single String[] parameter"));
        } catch (ReflectiveOperationException e) {
            throw new CannotMakeChain(className, e);
        }
    }

    private static Predicate<Constructor<?>> matchesStringArrayConstructor() {
        return c -> {
            Class<?>[] parameterTypes = c.getParameterTypes();
            return parameterTypes.length == 1
                    && parameterTypes[0].isArray()
                    && parameterTypes[0].getComponentType() == String.class;
        };
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isAllowSessionCreation() {
        return allowSessionCreation;
    }

    public void setAllowSessionCreation(boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }

    public boolean isRequireSSL() {
        return requireSSL;
    }

    public void setRequireSSL(boolean requireSSL) {
        this.requireSSL = requireSSL;
    }

    public boolean isMatchHTTPMethod() {
        return matchHTTPMethod;
    }

    public void setMatchHTTPMethod(boolean matchHTTPMethod) {
        this.matchHTTPMethod = matchHTTPMethod;
    }

    public Set<String> getHttpMethods() {
        return httpMethods;
    }

    public void setHttpMethods(Set<String> httpMethods) {
        this.httpMethods = httpMethods;
    }

    public String getRoleFilterName() {
        return roleFilterName;
    }

    public void setRoleFilterName(String roleFilterName) {
        this.roleFilterName = roleFilterName;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
