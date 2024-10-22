/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.TRACE;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.PropertyFileWatcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/** Data access object for rest security configuration. */
public class WorkspaceAdminRESTAccessRuleDAO
        extends AbstractAccessRuleDAO<WorkspaceAdminRestAccessRule> implements InitializingBean {

    protected WorkspaceAdminRESTAccessRuleDAO(GeoServerDataDirectory dd) throws IOException {
        super(dd, "rest.workspaceadmin.properties");
    }

    public static WorkspaceAdminRESTAccessRuleDAO get() {
        return GeoServerExtensions.bean(WorkspaceAdminRESTAccessRuleDAO.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.rules = new ConcurrentSkipListSet<>();
        loadDefaults(0).forEach(super::addRule);
    }

    @Override
    protected void loadRules(Properties props) {
        List<WorkspaceAdminRestAccessRule> configured = loadInternal(props, 0);
        List<WorkspaceAdminRestAccessRule> defaults = loadDefaults(configured.size());
        Collection<WorkspaceAdminRestAccessRule> merged = merge(defaults, configured);
        super.clear();
        merged.forEach(super::addRule);
    }

    // externally configured ones takes priority over default ones
    private Collection<WorkspaceAdminRestAccessRule> merge(
            List<WorkspaceAdminRestAccessRule> defaults,
            List<WorkspaceAdminRestAccessRule> configured) {

        Map<String, WorkspaceAdminRestAccessRule> rules = new LinkedHashMap<>();
        configured.forEach(r -> rules.put(r.getAntPattern(), r));

        defaults.stream()
                .filter(d -> !rules.containsKey(d.getAntPattern()))
                .forEach(d -> rules.put(d.getAntPattern(), d));

        return rules.values();
    }

    @Override
    protected Properties toProperties() {
        PropertyFileWatcher.LinkedProperties props = new PropertyFileWatcher.LinkedProperties();
        for (WorkspaceAdminRestAccessRule rule : rules) {
            props.setProperty(rule.getAntPattern(), rule.methods());
        }
        return props;
    }

    private List<WorkspaceAdminRestAccessRule> loadDefaults(int basePriority) {
        Properties props = load("rest.workspaceadmin.defaults.properties");
        return loadInternal(props, basePriority);
    }

    private List<WorkspaceAdminRestAccessRule> loadInternal(Properties props, int basePriority) {
        AtomicInteger priority = new AtomicInteger(basePriority);
        return props.entrySet().stream()
                .map(
                        e -> {
                            String antPattern = (String) e.getKey();
                            Set<HttpMethod> methods = parseMethods((String) e.getValue());
                            int p = priority.getAndIncrement();
                            return new WorkspaceAdminRestAccessRule(p, antPattern, methods);
                        })
                .collect(Collectors.toList());
    }

    private PropertyFileWatcher.LinkedProperties load(String resource) {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            PropertyFileWatcher.LinkedProperties props = new PropertyFileWatcher.LinkedProperties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Set<HttpMethod> parseMethods(String values) {
        return Stream.of(values.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(StringUtils::hasText)
                .map(this::mapMethods)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private List<HttpMethod> mapMethods(String value) {
        if ("R".equalsIgnoreCase(value)) return List.of(GET, HEAD, OPTIONS, TRACE);
        if ("W".equalsIgnoreCase(value)) return List.of(POST, PUT, PATCH, DELETE);
        if ("A".equalsIgnoreCase(value))
            return Stream.of(mapMethods("R"), mapMethods("W"))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

        return List.of(HttpMethod.valueOf(value));
    }
}
