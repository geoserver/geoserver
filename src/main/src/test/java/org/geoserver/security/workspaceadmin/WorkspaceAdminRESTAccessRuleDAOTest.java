/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.TRACE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.http.HttpMethod;

/** Unit tests for {@link WorkspaceAdminRESTAccessRuleDAO}. */
public class WorkspaceAdminRESTAccessRuleDAOTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private WorkspaceAdminRESTAccessRuleDAO dao;

    @Before
    public void setUp() throws IOException {
        File dataDirFile = tempFolder.newFolder("dataDir");
        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(dataDirFile);
        Resource securityDir = dataDir.get("security");
        securityDir.dir().mkdir();
        dao = new WorkspaceAdminRESTAccessRuleDAO(dataDir);
    }

    @Test
    public void testCreateAndInitialize() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("rest.workspaceadmin.properties.template")) {
            properties.load(in);
        }
        assertFalse(properties.isEmpty());

        // The rules should be loaded from the template file
        List<WorkspaceAdminRestAccessRule> rules = dao.getRules();
        assertFalse(rules.isEmpty());

        Map<String, WorkspaceAdminRestAccessRule> rulesMap = rules.stream()
                .collect(Collectors.toMap(WorkspaceAdminRestAccessRule::getAntPattern, Function.identity()));
        for (String pattern : properties.stringPropertyNames()) {
            assertTrue(rulesMap.containsKey(pattern));
            String value = properties.getProperty(pattern);
            WorkspaceAdminRestAccessRule rule = rulesMap.get(pattern);

            Set<HttpMethod> expected = dao.parseMethods(value);
            assertFalse(expected.isEmpty());
            Set<HttpMethod> actual = rule.getMethods();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testLoadRules() {
        // Create a custom properties file
        Properties props = new PropertyFileWatcher.LinkedProperties();
        props.setProperty("/custom/path", "GET,POST,PUT");
        props.setProperty("/custom/path/with/wildcard/**", "rw");

        final String defaultRule = "/rest/workspaces/{workspace}/**";
        assertTrue(dao.getRules().stream()
                .map(WorkspaceAdminRestAccessRule::getAntPattern)
                .anyMatch(defaultRule::equals));
        // Call loadRules with custom properties
        dao.loadRules(props);

        // Verify custom rules are loaded
        boolean foundCustomRule = false;
        boolean foundWildcardRule = false;
        boolean foundDefaultRule = false;

        for (WorkspaceAdminRestAccessRule rule : dao.getRules()) {
            if ("/custom/path".equals(rule.getAntPattern())) {
                foundCustomRule = true;
                assertEquals(Set.of(GET, POST, PUT), rule.getMethods());
            } else if ("/custom/path/with/wildcard/**".equals(rule.getAntPattern())) {
                foundWildcardRule = true;
                assertEquals(Set.of(PATCH, HEAD, OPTIONS, POST, PUT, TRACE, DELETE, GET), rule.getMethods());
            } else if (defaultRule.equals(rule.getAntPattern())) {
                foundDefaultRule = true;
            }
        }

        assertTrue("Custom rule not found", foundCustomRule);
        assertTrue("Wildcard rule not found", foundWildcardRule);
        assertFalse("Default rules should be replaced", foundDefaultRule);
    }

    @Test
    public void testToProperties() {
        dao.afterPropertiesSet();
        // Add a custom rule
        WorkspaceAdminRestAccessRule customRule =
                new WorkspaceAdminRestAccessRule(100, "/rest/custom/test", Set.of(GET, POST));
        dao.addRule(customRule);

        // Convert to properties
        Properties props = dao.toProperties();

        // Check that properties contain all rules
        assertTrue(props.containsKey("/rest/custom/test"));
        assertEquals("GET,POST", props.getProperty("/rest/custom/test"));

        // Check for default rules too
        assertTrue(props.containsKey("/rest/workspaces/{workspace}/**"));
    }
}
