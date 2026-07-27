/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.junit.Test;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Deterministic coverage for the <em>abort</em> branch of the pre-flight restore validation step
 * ({@link ValidateRestoreTasklet#doExecute}). The sibling {@code ValidateRestoreTaskletTest} only covers the static
 * {@code collectProblems} sweep; this one drives the tasklet body itself to prove the {@code BK_FAIL_ON_INVALID}
 * contract:
 *
 * <ul>
 *   <li>invalid catalog + {@code BK_FAIL_ON_INVALID=true} -&gt; the step throws (job FAILED, so the live reload that
 *       {@code finalizeRestore} performs is skipped);
 *   <li>invalid catalog + {@code BK_FAIL_ON_INVALID=false} -&gt; the step only reports and FINISHES (default mode);
 *   <li>valid catalog + {@code BK_FAIL_ON_INVALID=true} -&gt; nothing to abort on, the step FINISHES.
 * </ul>
 *
 * <p>It needs neither a Spring context nor a running batch job. A tiny test subclass overrides {@code getCatalog()} and
 * {@code isNew()} so the body runs against a hand-built {@link CatalogImpl}; {@code failOnInvalid} is driven through
 * the normal {@code initialize()} seam with a mock {@link StepExecution} that only has to answer
 * {@code getJobParameters()}. The (otherwise unused) {@code Backup} collaborator is a nice mock, and the three
 * {@code doExecute} arguments are all unused by the body, so they are passed as {@code null}.
 * {@code getCurrentJobExecution()} stays {@code null}; every use of it in the tasklet is null-guarded, so the abort
 * still surfaces as the thrown exception rather than an NPE.
 */
public class ValidateRestoreTaskletAbortTest {

    /** A {@link ValidateRestoreTasklet} whose catalog and restore-path flag are injected for unit testing. */
    private static final class TestableValidateRestoreTasklet extends ValidateRestoreTasklet {
        private final Catalog injectedCatalog;

        TestableValidateRestoreTasklet(Backup backupFacade, Catalog injectedCatalog, boolean failOnInvalid) {
            super(backupFacade);
            this.injectedCatalog = injectedCatalog;
            // isNew()==true selects the restore path; drive failOnInvalid through the normal initialize() seam
            setNew(true);
            initialize(stepExecutionWith(failOnInvalid));
        }

        @Override
        public Catalog getCatalog() {
            // bypass the base getCatalog() (which authenticates via the real Backup) and serve the test catalog
            return injectedCatalog;
        }
    }

    /** A mock {@link StepExecution} that answers only the {@code getJobParameters()} the tasklet's initialize reads. */
    private static StepExecution stepExecutionWith(boolean failOnInvalid) {
        StepExecution stepExecution = createNiceMock(StepExecution.class);
        expect(stepExecution.getJobParameters())
                .andReturn(new JobParametersBuilder()
                        .addString(Backup.PARAM_FAIL_ON_INVALID, Boolean.toString(failOnInvalid))
                        .toJobParameters())
                .anyTimes();
        replay(stepExecution);
        return stepExecution;
    }

    @Test
    public void invalidCatalogAbortsWhenFailOnInvalidTrue() {
        Catalog catalog = invalidCatalog();
        TestableValidateRestoreTasklet tasklet =
                new TestableValidateRestoreTasklet(createNiceMock(Backup.class), catalog, true);

        CatalogException thrown = assertThrows(
                "BK_FAIL_ON_INVALID=true must abort the step when the assembled catalog is invalid",
                CatalogException.class,
                () -> tasklet.doExecute(null, null, null));
        // the abort message is the pre-flight summary, which must name the invalid object kind
        assertTrue(
                "abort message should describe the invalid catalog object, got: " + thrown.getMessage(),
                thrown.getMessage().contains("style"));
    }

    @Test
    public void invalidCatalogOnlyReportsWhenFailOnInvalidFalse() throws Exception {
        Catalog catalog = invalidCatalog();
        TestableValidateRestoreTasklet tasklet =
                new TestableValidateRestoreTasklet(createNiceMock(Backup.class), catalog, false);

        // default mode: an invalid catalog is reported (logged / recorded as warnings) but does NOT abort the restore
        RepeatStatus status = tasklet.doExecute(null, null, null);
        assertEquals(RepeatStatus.FINISHED, status);
    }

    @Test
    public void validCatalogFinishesEvenWhenFailOnInvalidTrue() throws Exception {
        Catalog catalog = new CatalogImpl();
        addValidContent(catalog);
        TestableValidateRestoreTasklet tasklet =
                new TestableValidateRestoreTasklet(createNiceMock(Backup.class), catalog, true);

        // nothing invalid to trip on, so even in fail-on-invalid mode the step finishes normally
        RepeatStatus status = tasklet.doExecute(null, null, null);
        assertEquals(RepeatStatus.FINISHED, status);
    }

    /** A catalog with a single deliberately invalid object: a style whose mandatory name has been nulled. */
    private static Catalog invalidCatalog() {
        Catalog catalog = new CatalogImpl();
        addValidContent(catalog);
        StyleInfo style = catalog.getStyleByName("good-style");
        // unwrap the ModificationProxy and corrupt the stored object so the validation sweep sees an invalid style
        ModificationProxy.unwrap(style).setName(null);
        return catalog;
    }

    private static void addValidContent(Catalog catalog) {
        CatalogFactory factory = catalog.getFactory();

        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName("good-ws");
        catalog.add(ws);

        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix("good-ws");
        ns.setURI("http://good");
        catalog.add(ns);

        StyleInfo style = factory.createStyle();
        style.setName("good-style");
        style.setFilename("good-style.sld");
        catalog.add(style);
    }
}
