/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.util.LoggerRule;
import org.geotools.util.logging.Logging;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/** Integration test that demonstrates rule validation is deferred until the catalog is reloaded. */
public class ValidateRulesAfterCatalogLoadTest {

    @Rule
    public LoggerRule log = new LoggerRule(Logging.getLogger(DataAccessRuleDAO.class), Level.ALL);

    @Test
    public void testValidationDeferredUntilCatalogReloaded() throws Exception {
        Catalog rawCatalog = mock(Catalog.class);

        // ensure the catalog looks empty at first
        when(rawCatalog.getWorkspaces()).thenReturn(Collections.emptyList());
        when(rawCatalog.getWorkspaceByName(any(String.class))).thenReturn(null);

        // capture the registered CatalogListener
        AtomicReference<CatalogListener> listenerRef = new AtomicReference<>();
        doAnswer(invocation -> {
                    CatalogListener l = (CatalogListener) invocation.getArguments()[0];
                    listenerRef.set(l);
                    return null;
                })
                .when(rawCatalog)
                .addListener(any(CatalogListener.class));

        // build large properties set simulating thousands of layers
        Properties props = new Properties();
        final int RULES = 2000; // large number to stress the parsing path
        for (int i = 0; i < RULES; i++) {
            props.put("ws" + i + ".layer" + i + ".r", "ROLE_X");
        }

        // construct the DAO (this will parse rules but should not emit WARNINGs yet)
        MemoryDataAccessRuleDAO dao = new MemoryDataAccessRuleDAO(rawCatalog, props);

        // ensure no WARNING logged during parsing (spurious startup warnings suppressed)
        boolean sawWarningAtParse = log.records().stream()
                .anyMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue()
                        && r.getMessage().contains("Namespace/Workspace"));
        Assert.assertFalse("No WARNING should be logged during parse (validation deferred)", sawWarningAtParse);

        // now ensure the catalog still reports missing workspaces (simulate missing resources scenario)
        when(rawCatalog.getWorkspaceByName(any(String.class))).thenReturn(null);

        // trigger the catalog reload event which should cause validation to run and warnings to be emitted
        CatalogListener l = listenerRef.get();
        Assert.assertNotNull("CatalogListener should have been registered", l);
        l.reloaded();

        // now a WARNING should be logged for at least one of the rules
        log.assertLogged(new BaseMatcher<LogRecord>() {
            @Override
            public boolean matches(Object item) {
                if (!(item instanceof LogRecord)) return false;
                LogRecord r = (LogRecord) item;
                return r.getLevel().intValue() >= Level.WARNING.intValue()
                        && r.getMessage().contains("Namespace/Workspace");
            }

            @Override
            public void describeTo(Description description) {}
        });
    }

    @Test
    public void testNoWarningWhenWorkspaceAddedBeforeReload() throws Exception {
        Catalog rawCatalog = mock(Catalog.class);

        // catalog appears empty at parse time
        when(rawCatalog.getWorkspaces()).thenReturn(Collections.emptyList());
        when(rawCatalog.getWorkspaceByName(any(String.class))).thenReturn(null);

        // capture the registered CatalogListener
        AtomicReference<CatalogListener> listenerRef = new AtomicReference<>();
        doAnswer(invocation -> {
                    CatalogListener l = (CatalogListener) invocation.getArguments()[0];
                    listenerRef.set(l);
                    return null;
                })
                .when(rawCatalog)
                .addListener(any(CatalogListener.class));

        // large properties set
        Properties props = new Properties();
        final int RULES = 2000;
        for (int i = 0; i < RULES; i++) {
            props.put("ws" + i + ".layer" + i + ".r", "ROLE_X");
        }

        // construct DAO (parsing occurs, no warnings expected)
        MemoryDataAccessRuleDAO dao = new MemoryDataAccessRuleDAO(rawCatalog, props);
        boolean sawWarningAtParse = log.records().stream()
                .anyMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue()
                        && r.getMessage().contains("Namespace/Workspace"));
        Assert.assertFalse("No WARNING should be logged during parse (validation deferred)", sawWarningAtParse);

        // now simulate that workspaces were added before the catalog reload
        when(rawCatalog.getWorkspaceByName(any(String.class)))
                .thenReturn(mock(org.geoserver.catalog.WorkspaceInfo.class));
        when(rawCatalog.getWorkspaces())
                .thenReturn(Collections.singletonList(mock(org.geoserver.catalog.WorkspaceInfo.class)));

        // trigger reload and ensure NO WARNINGs are logged as the workspaces exist
        CatalogListener l = listenerRef.get();
        Assert.assertNotNull("CatalogListener should have been registered", l);
        l.reloaded();

        // assert no Namespace/Workspace WARNING was emitted after validation
        boolean sawWarningAfterReload = log.records().stream()
                .anyMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue()
                        && r.getMessage().contains("Namespace/Workspace"));
        Assert.assertFalse(
                "No WARNING should be logged after reload when workspaces are present", sawWarningAfterReload);
    }
}
