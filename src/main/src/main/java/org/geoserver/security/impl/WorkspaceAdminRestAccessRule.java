/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.util.AntPathMatcher;

@SuppressWarnings("serial")
public class WorkspaceAdminRestAccessRule
        implements ConfigAttribute, Comparable<WorkspaceAdminRestAccessRule> {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private String antPattern;

    private Set<HttpMethod> methods;

    private int priority;

    WorkspaceAdminRestAccessRule(int priority, String antPattern, Set<HttpMethod> methods) {
        this.priority = priority;
        this.antPattern = antPattern;
        this.methods = Set.copyOf(methods);
    }

    public String getAntPattern() {
        return antPattern;
    }

    public Set<HttpMethod> getMethods() {
        return methods;
    }

    public boolean matches(String uri, HttpMethod method) {
        return methods.contains(method) && PATH_MATCHER.match(antPattern, uri);
    }

    @Override
    public String getAttribute() {
        return String.format("%s=%s", antPattern, methods());
    }

    public String methods() {
        return methods.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(","));
    }

    @Override
    public int compareTo(WorkspaceAdminRestAccessRule o) {
        return Integer.compare(priority, o.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(antPattern, methods);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WorkspaceAdminRestAccessRule) {
            WorkspaceAdminRestAccessRule other = (WorkspaceAdminRestAccessRule) obj;
            return Objects.equals(antPattern, other.antPattern)
                    && Objects.equals(methods, other.methods);
        }
        return false;
    }

    @Override
    public String toString() {
        return getAttribute();
    }
}
