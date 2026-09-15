/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.util.AntPathMatcher;

/**
 * A single REST access rule for workspace administrators: an Ant-style URL pattern, the set of HTTP methods it allows,
 * and a priority determining evaluation order (lower values first).
 *
 * <p>Rules are loaded by {@link WorkspaceAdminRESTAccessRuleDAO} from {@code security/rest.workspaceadmin.properties}.
 * Implements {@link ConfigAttribute} to integrate with Spring Security's authorization framework.
 */
@SuppressWarnings({"serial", "deprecation"})
public class WorkspaceAdminRestAccessRule implements ConfigAttribute, Comparable<WorkspaceAdminRestAccessRule> {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private String antPattern;

    private Set<HttpMethod> methods;

    private int priority;

    public WorkspaceAdminRestAccessRule(int priority, String antPattern, Set<HttpMethod> methods) {
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

    /** Returns true if the method is allowed and the URI, ignoring any query string, matches the Ant pattern. */
    public boolean matches(String uri, HttpMethod method) {
        int i;
        if ((i = uri.indexOf('?')) > -1) {
            uri = uri.substring(0, i);
        }
        return methods.contains(method) && PATH_MATCHER.match(antPattern, uri);
    }

    /** Returns this rule as {@code antPattern=METHOD1,METHOD2,...} with the methods sorted alphabetically. */
    @Override
    public String getAttribute() {
        return String.format("%s=%s", antPattern, methods());
    }

    /** Returns the allowed HTTP method names, comma-separated and sorted alphabetically. */
    public String methods() {
        return methods.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(","));
    }

    /** Orders by priority, breaking ties by pattern and methods so sorted sets never drop a distinct rule. */
    @Override
    public int compareTo(WorkspaceAdminRestAccessRule o) {
        int c = Integer.compare(priority, o.priority);
        if (c == 0) {
            c = antPattern.compareTo(o.antPattern);
        }
        if (c == 0) {
            c = methods().compareTo(o.methods());
        }
        return c;
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, antPattern, methods);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WorkspaceAdminRestAccessRule other) {
            return priority == other.priority
                    && Objects.equals(antPattern, other.antPattern)
                    && Objects.equals(methods, other.methods);
        }
        return false;
    }

    @Override
    public String toString() {
        return getAttribute();
    }
}
