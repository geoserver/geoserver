/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * PROTOTYPE: pre-flight restore validation pass.
 *
 * <p>Runs as a dedicated step after the restore catalog has been fully assembled (all catalog chunk steps plus the
 * GeoServer global and security steps) but before {@code finalizeRestore} reloads the live instance. Because the whole
 * graph is present and cross-referenced at this point, a single {@link Catalog#validate} sweep gives a trustworthy
 * verdict on whether the restored configuration is valid — unlike the per-item validation in
 * {@code CatalogItemProcessor}, which runs while the catalog is only partially assembled and was progressively defanged
 * (filter gating, id gating, log-only store helpers, extended validation disabled) to avoid false failures.
 *
 * <p>By default the pass only <b>reports</b> (logs a summary and records the findings as warnings on the execution).
 * With {@code BK_FAIL_ON_INVALID=true} it <b>aborts</b> the restore (step FAILED), so {@code finalizeRestore} — and
 * therefore the live in-memory reload — is skipped.
 *
 * <p>KNOWN LIMITATION (prototype): the catalog writer commits each item to disk as the chunk steps run (the restore
 * catalog carries {@code GeoServerConfigPersister}), so aborting here does not roll back what was already written to
 * the data directory; it only prevents the in-memory reload. A fully transactional "validate before any write" mode
 * would additionally need to defer the persister / writer until this pass succeeds (or stage into a scratch dir).
 */
public class ValidateRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    private static final Logger LOGGER = Logging.getLogger(ValidateRestoreTasklet.class);

    private boolean failOnInvalid = false;

    public ValidateRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        this.failOnInvalid =
                Boolean.parseBoolean(stepExecution.getJobParameters().getString(Backup.PARAM_FAIL_ON_INVALID, "false"));
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        // Only meaningful on the restore path: isNew() == true means getCatalog() is the assembled restore catalog.
        if (!isNew()) {
            return RepeatStatus.FINISHED;
        }

        final Catalog catalog = getCatalog();

        // The chunk pipeline disabled extended validation on the restore catalog to avoid ordering false-failures while
        // items were still being added. The graph is complete now, so re-enable it for an honest, thorough pre-flight.
        if (catalog instanceof CatalogImpl) {
            ((CatalogImpl) catalog).setExtendedValidation(true);
        }

        final List<String> problems = collectProblems(catalog);

        if (problems.isEmpty()) {
            LOGGER.info("Restore pre-flight validation: the assembled catalog is valid.");
            return RepeatStatus.FINISHED;
        }

        final String summary = "Restore pre-flight validation found "
                + problems.size()
                + " invalid catalog object(s):\n  - "
                + String.join("\n  - ", problems);
        LOGGER.warning(summary);

        // Surface the findings on the execution so they show up in the REST / web execution summary.
        if (getCurrentJobExecution() != null) {
            final List<Throwable> warnings = new ArrayList<>();
            for (String p : problems) {
                warnings.add(new CatalogException(p));
            }
            getCurrentJobExecution().addWarningExceptions(warnings);
        }

        if (failOnInvalid) {
            final CatalogException abort = new CatalogException(summary);
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addFailureExceptions(Arrays.asList(abort));
            }
            throw abort; // step FAILED -> job FAILED -> finalizeRestore (live reload) is skipped
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Validates every catalog object in the (fully-assembled) catalog and returns a human-readable description of each
     * invalidity. Package-visible and static so it can be unit-tested against a hand-built catalog without a running
     * restore job. {@code isNew=false}: the objects are already members of the catalog, so referential checks resolve
     * against the complete graph instead of false-failing on load order.
     */
    static List<String> collectProblems(Catalog catalog) {
        final List<String> problems = new ArrayList<>();
        check(catalog.getWorkspaces(), w -> catalog.validate(w, false), WorkspaceInfo::getName, "workspace", problems);
        check(
                catalog.getNamespaces(),
                n -> catalog.validate(n, false),
                NamespaceInfo::getPrefix,
                "namespace",
                problems);
        check(
                catalog.getStores(StoreInfo.class),
                s -> catalog.validate(s, false),
                StoreInfo::getName,
                "store",
                problems);
        check(
                catalog.getResources(ResourceInfo.class),
                r -> catalog.validate(r, false),
                ResourceInfo::getName,
                "resource",
                problems);
        check(catalog.getStyles(), st -> catalog.validate(st, false), StyleInfo::getName, "style", problems);
        check(catalog.getLayers(), l -> catalog.validate(l, false), LayerInfo::getName, "layer", problems);
        check(
                catalog.getLayerGroups(),
                lg -> catalog.validate(lg, false),
                LayerGroupInfo::getName,
                "layerGroup",
                problems);
        return problems;
    }

    private static <T extends CatalogInfo> void check(
            List<T> items,
            Function<T, ValidationResult> validator,
            Function<T, String> namer,
            String kind,
            List<String> problems) {
        for (T item : items) {
            try {
                ValidationResult result = validator.apply(item);
                if (result != null && !result.isValid()) {
                    problems.add(kind + " '" + namer.apply(item) + "': " + result.getErrosAsString("; "));
                }
            } catch (Exception e) {
                problems.add(kind + " '" + namer.apply(item) + "': " + e.getMessage());
            }
        }
    }
}
