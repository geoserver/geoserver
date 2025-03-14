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
 * Represents a REST API access rule for workspace administrators.
 *
 * <p>This class defines URL patterns and HTTP methods that workspace administrators are allowed to access. Each rule
 * consists of:
 *
 * <ul>
 *   <li>An Ant-style URL pattern (e.g., "/rest/workspaces/{workspace}/**")
 *   <li>A set of permitted HTTP methods (e.g., GET, POST, PUT, etc.)
 *   <li>A priority value used for rule ordering
 * </ul>
 *
 * <p>Rules are defined in the security/rest.workspaceadmin.properties file and loaded by the
 * {@link WorkspaceAdminRESTAccessRuleDAO}. The file is initially populated from a template with default rules that
 * allow workspace administrators to manage resources within their assigned workspaces.
 *
 * <p>Rules have the format:
 *
 * <pre>
 * /url/pattern=METHOD1,METHOD2,...
 * </pre>
 *
 * <p>Where methods can use shorthand values:
 *
 * <ul>
 *   <li>r = Read operations (GET, HEAD, OPTIONS, TRACE)
 *   <li>w = Write operations (POST, PUT, PATCH, DELETE)
 *   <li>rw = All operations (read + write)
 * </ul>
 *
 * <p>When evaluating access, rules with lower priority values (higher priority) are evaluated before rules with higher
 * values.
 *
 * <p>The class implements {@link ConfigAttribute} to integrate with Spring Security's authorization framework and
 * {@link Comparable} to allow sorting rules by priority.
 *
 * <p>Examples of rule patterns:
 *
 * <pre>
 * /rest/workspaces/{workspace}/**   - All URLs under a specific workspace
 * /rest/namespaces/{workspace}/**    - All namespace URLs for a specific workspace
 * /rest/layers/{workspace}:*         - Layer resources for a specific workspace
 * /rest/resource/workspaces          - The workspaces resource collection
 * </pre>
 *
 * @see WorkspaceAdminRESTAccessRuleDAO
 * @see WorkspaceAdminAuthorizationManager
 * @see WorkspaceAdminRestfulDefinitionSource
 */
@SuppressWarnings("serial")
public class WorkspaceAdminRestAccessRule implements ConfigAttribute, Comparable<WorkspaceAdminRestAccessRule> {

    /** The path matcher used to compare request URLs against the rule's Ant pattern. */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * The Ant-style pattern defining which URLs this rule applies to. May contain wildcards and path variables like
     * {workspace}.
     */
    private String antPattern;

    /** The set of HTTP methods (GET, POST, PUT, etc.) allowed by this rule. */
    private Set<HttpMethod> methods;

    /**
     * The priority of this rule, used for ordering when multiple rules might apply. Lower values indicate higher
     * priority.
     */
    private int priority;

    /**
     * Creates a new REST access rule for workspace administrators.
     *
     * @param priority the rule priority (lower values are evaluated first)
     * @param antPattern the Ant-style URL pattern this rule applies to
     * @param methods the set of HTTP methods allowed by this rule
     */
    WorkspaceAdminRestAccessRule(int priority, String antPattern, Set<HttpMethod> methods) {
        this.priority = priority;
        this.antPattern = antPattern;
        this.methods = Set.copyOf(methods);
    }

    /**
     * Returns the Ant-style URL pattern for this rule.
     *
     * @return the URL pattern
     */
    public String getAntPattern() {
        return antPattern;
    }

    /**
     * Returns the set of HTTP methods allowed by this rule.
     *
     * @return the allowed HTTP methods
     */
    public Set<HttpMethod> getMethods() {
        return methods;
    }

    /**
     * Determines if this rule matches the given URI and HTTP method.
     *
     * <p>A match occurs when:
     *
     * <ol>
     *   <li>The HTTP method is contained in this rule's allowed methods
     *   <li>The URI matches this rule's Ant pattern according to Spring's AntPathMatcher
     * </ol>
     *
     * @param uri the URI to check against this rule's pattern
     * @param method the HTTP method to check against this rule's allowed methods
     * @return true if both the URI and method match this rule, false otherwise
     */
    public boolean matches(String uri, HttpMethod method) {
        int i;
        if ((i = uri.indexOf('?')) > -1) {
            uri = uri.substring(0, i);
        }
        return methods.contains(method) && PATH_MATCHER.match(antPattern, uri);
    }

    /**
     * Returns a string representation of this rule as required by the ConfigAttribute interface.
     *
     * <p>The format is "antPattern=METHOD1,METHOD2,..." where the methods are sorted alphabetically.
     *
     * @return a string representation of this rule
     */
    @Override
    public String getAttribute() {
        return String.format("%s=%s", antPattern, methods());
    }

    /**
     * Returns a comma-separated string of HTTP method names in alphabetical order.
     *
     * @return a string representation of the allowed HTTP methods
     */
    public String methods() {
        return methods.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(","));
    }

    /**
     * Compares this rule to another based on priority.
     *
     * <p>Rules with lower priority values are "less than" rules with higher values, meaning they will appear earlier in
     * sorted collections.
     *
     * @param o the rule to compare to
     * @return negative if this rule has higher priority, positive if lower, 0 if equal
     */
    @Override
    public int compareTo(WorkspaceAdminRestAccessRule o) {
        return Integer.compare(priority, o.priority);
    }

    /**
     * Generates a hash code based on the antPattern and methods.
     *
     * <p>Note that priority is deliberately excluded from the hash code calculation as it affects ordering but not the
     * rule's semantic identity.
     *
     * @return a hash code for this rule
     */
    @Override
    public int hashCode() {
        return Objects.hash(antPattern, methods);
    }

    /**
     * Determines if this rule is equal to another object.
     *
     * <p>Rules are considered equal if they have the same antPattern and methods. The priority is deliberately excluded
     * from equality checks as it affects ordering but not the rule's semantic identity.
     *
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WorkspaceAdminRestAccessRule) {
            WorkspaceAdminRestAccessRule other = (WorkspaceAdminRestAccessRule) obj;
            return Objects.equals(antPattern, other.antPattern) && Objects.equals(methods, other.methods);
        }
        return false;
    }

    /**
     * Returns a string representation of this rule, equivalent to getAttribute().
     *
     * @return a string representation of this rule
     */
    @Override
    public String toString() {
        return getAttribute();
    }
}
