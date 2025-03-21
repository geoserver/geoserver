/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.workspaceadmin;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.TRACE;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.PropertyFileWatcher;
import org.geoserver.security.impl.AbstractAccessRuleDAO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/**
 * Data access object for workspace administrator REST API security rules.
 *
 * <p>This class is responsible for loading, managing, and providing access to the security rules that control which
 * REST API endpoints workspace administrators can access. It extends {@link AbstractAccessRuleDAO} to inherit common
 * functionality for rule management.
 *
 * <p>The rules are loaded from the "rest.workspaceadmin.properties" file in the GeoServer data directory's security
 * folder. If the file doesn't exist, it's initialized from the classpath template
 * "rest.workspaceadmin.properties.template" which contains the default workspace administrator access rules.
 *
 * <p>REST endpoints that don't match any pattern in the rest.workspaceadmin.properties file fall back to the global
 * REST security configuration (rest.properties), which typically restricts access to administrators only. The default
 * template includes patterns for workspace-specific resources that workspace administrators need to manage their
 * workspaces while restricting access to other areas of the API.
 *
 * <p>Rules are defined in properties files with the format:
 *
 * <pre>
 * /url/pattern=METHOD1,METHOD2,...
 * </pre>
 *
 * <p>Where methods can use these shorthand values:
 *
 * <ul>
 *   <li>{@code r} = Read operations ({@code GET, HEAD, OPTIONS, TRACE})
 *   <li>{@code w} = Write operations ({@code POST, PUT, PATCH, DELETE})
 *   <li>{@code rw} = All operations ({@code r + w})
 * </ul>
 *
 * <p>This class maintains rules in a sorted collection based on priority, allowing the most specific rules to be
 * evaluated first.
 *
 * @see WorkspaceAdminRestAccessRule
 * @see WorkspaceAdminRestfulDefinitionSource
 * @see WorkspaceAdminAuthorizer
 */
public class WorkspaceAdminRESTAccessRuleDAO extends AbstractAccessRuleDAO<WorkspaceAdminRestAccessRule>
        implements InitializingBean {

    private static final List<HttpMethod> READ_METHODS = List.of(GET, HEAD, OPTIONS, TRACE);
    private static final List<HttpMethod> WRITE_METHODS = List.of(POST, PUT, PATCH, DELETE);
    private static final List<HttpMethod> READ_WRITE_METHODS = List.copyOf(
            Stream.concat(READ_METHODS.stream(), WRITE_METHODS.stream()).collect(Collectors.toList()));
    /**
     * The file under {@literal security/} to load rules from. AbstractAccessRuleDAO will initialize it from the
     * classpath's rest.workspaceadmin.properties.template if the file doesn't exist.
     */
    private static final String WORKSPACEADMIN_REST_PROPERTIES = "rest.workspaceadmin.properties";

    /**
     * Creates a new DAO for workspace administrator REST access rules.
     *
     * @param dd the GeoServer data directory to read configuration from
     * @throws IOException if there is an error accessing the data directory
     */
    protected WorkspaceAdminRESTAccessRuleDAO(GeoServerDataDirectory dd) throws IOException {
        super(dd, WORKSPACEADMIN_REST_PROPERTIES);
    }

    /**
     * Force loading the rules and initializing the {@code security/rest.workspaceadmin.properties} file with the
     * default rules if it doesn't exist
     */
    @Override
    public void afterPropertiesSet() {
        checkPropertyFile(false);
    }

    /**
     * Converts the current rules to a Properties object for persistence.
     *
     * <p>Each rule is stored as a property with the Ant pattern as the key and a comma-separated list of method names
     * as the value.
     *
     * @return a Properties object containing the current rules
     */
    @Override
    protected Properties toProperties() {
        // PropertyFileWatcher.LinkedProperties maintains order
        PropertyFileWatcher.LinkedProperties props = new PropertyFileWatcher.LinkedProperties();
        for (WorkspaceAdminRestAccessRule rule : rules) {
            props.setProperty(rule.getAntPattern(), rule.methods());
        }
        return props;
    }

    /**
     * Loads rules from the provided properties.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Loads the user-configured rules from the properties
     *   <li>Merges them, with configured rules taking precedence
     *   <li>Clears the existing rules and adds the merged set
     * </ol>
     *
     * @param props the properties to load rules from
     */
    @Override
    protected void loadRules(Properties props) {
        rules = new ConcurrentSkipListSet<>(loadInternal(props));
    }

    /**
     * Creates rules from the provided properties with sequential priorities.
     *
     * <p>Each property entry is converted to a rule with:
     *
     * <ul>
     *   <li>The key as the Ant pattern
     *   <li>The value as a comma-separated list of HTTP methods or shorthand values
     *   <li>A sequential priority to maintain the declared order of the rules
     * </ul>
     *
     * @param props the properties to convert to rules
     * @return a list of rules created from the properties
     */
    private List<WorkspaceAdminRestAccessRule> loadInternal(Properties props) {
        AtomicInteger priority = new AtomicInteger(0);
        return props.entrySet().stream()
                .map(e -> {
                    String antPattern = (String) e.getKey();
                    Set<HttpMethod> methods = parseMethods((String) e.getValue());
                    int p = priority.getAndIncrement();
                    return new WorkspaceAdminRestAccessRule(p, antPattern, methods);
                })
                .collect(Collectors.toList());
    }

    /**
     * Parses a comma-separated string of HTTP methods or shorthand values.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Splits the input string by commas
     *   <li>Trims and uppercases each part
     *   <li>Filters out empty parts
     *   <li>Maps shorthand values (R, W, RW) to actual HTTP methods
     *   <li>Collects the results into a set
     * </ol>
     *
     * @param values the comma-separated string of methods
     * @return a set of HTTP methods
     */
    Set<HttpMethod> parseMethods(String values) {
        return Stream.of(values.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(StringUtils::hasText)
                .map(this::mapMethods)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Maps a method string to a list of HTTP methods.
     *
     * <p>Handles shorthand values:
     *
     * <ul>
     *   <li>R = GET, HEAD, OPTIONS, TRACE (read operations)
     *   <li>W = POST, PUT, PATCH, DELETE (write operations)
     *   <li>RW = R + W (all operations)
     * </ul>
     *
     * <p>For any other value, it assumes the string is a valid HTTP method name (like "GET" or "POST") and returns it
     * directly.
     *
     * @param value the method string to map
     * @return a list of HTTP methods
     */
    private List<HttpMethod> mapMethods(String value) {
        if ("R".equalsIgnoreCase(value)) return READ_METHODS;
        if ("W".equalsIgnoreCase(value)) return WRITE_METHODS;
        if ("RW".equalsIgnoreCase(value)) return READ_WRITE_METHODS;

        return List.of(HttpMethod.valueOf(value));
    }
}
