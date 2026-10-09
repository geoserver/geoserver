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
import org.geoserver.security.impl.AbstractAccessRuleDAO;
import org.geoserver.util.LinkedProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/**
 * Loads and manages the REST API access rules for workspace administrators.
 *
 * <p>Rules are read from {@code security/rest.workspaceadmin.properties} in the data directory, initialized from the
 * classpath template {@code rest.workspaceadmin.properties.template} when the file doesn't exist. Each entry has the
 * form {@code /url/pattern=METHOD1,METHOD2,...}, where a method is an HTTP method name or one of the shorthands
 * {@code r} (GET, HEAD, OPTIONS, TRACE), {@code w} (POST, PUT, PATCH, DELETE), or {@code rw} (both).
 *
 * <p>Rules are evaluated in declaration order. Requests matching no rule fall back to the global REST security
 * configuration ({@code rest.properties}), which typically restricts access to full administrators.
 *
 * @see WorkspaceAdminRestAccessRule
 * @see WorkspaceAdminAuthorizer
 */
public class WorkspaceAdminRESTAccessRuleDAO extends AbstractAccessRuleDAO<WorkspaceAdminRestAccessRule>
        implements InitializingBean {

    private static final List<HttpMethod> READ_METHODS = List.of(GET, HEAD, OPTIONS, TRACE);
    private static final List<HttpMethod> WRITE_METHODS = List.of(POST, PUT, PATCH, DELETE);
    private static final List<HttpMethod> READ_WRITE_METHODS = List.copyOf(
            Stream.concat(READ_METHODS.stream(), WRITE_METHODS.stream()).collect(Collectors.toList()));
    /** The file under {@literal security/} to load rules from, initialized from the classpath template if missing. */
    private static final String WORKSPACEADMIN_REST_PROPERTIES = "rest.workspaceadmin.properties";

    public WorkspaceAdminRESTAccessRuleDAO(GeoServerDataDirectory dd) throws IOException {
        super(dd, WORKSPACEADMIN_REST_PROPERTIES);
    }

    /** Forces loading the rules at startup, creating the properties file from the template if it doesn't exist. */
    @Override
    public void afterPropertiesSet() {
        checkPropertyFile(false);
    }

    @Override
    protected Properties toProperties() {
        // LinkedProperties used to maintain order
        LinkedProperties props = new LinkedProperties();
        for (WorkspaceAdminRestAccessRule rule : rules) {
            props.setProperty(rule.getAntPattern(), rule.methods());
        }
        return props;
    }

    @Override
    protected void loadRules(Properties props) {
        rules = new ConcurrentSkipListSet<>(loadInternal(props));
    }

    /** Assigns sequential priorities to the rules to preserve their declaration order. */
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

    /** Parses a comma-separated list of HTTP method names or the {@code r}/{@code w}/{@code rw} shorthands. */
    Set<HttpMethod> parseMethods(String values) {
        return Stream.of(values.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(StringUtils::hasText)
                .map(this::mapMethods)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private List<HttpMethod> mapMethods(String value) {
        if ("R".equalsIgnoreCase(value)) return READ_METHODS;
        if ("W".equalsIgnoreCase(value)) return WRITE_METHODS;
        if ("RW".equalsIgnoreCase(value)) return READ_WRITE_METHODS;

        return List.of(HttpMethod.valueOf(value));
    }
}
