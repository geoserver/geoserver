/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.config;

import java.io.File;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.processor.CatalogItemProcessor;
import org.geoserver.backuprestore.reader.CatalogFileReader;
import org.geoserver.backuprestore.reader.CatalogMultiResourceItemReader;
import org.geoserver.backuprestore.writer.CatalogItemWriter;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.repeat.CompletionPolicy;
import org.springframework.batch.infrastructure.repeat.policy.TimeoutTerminationPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Java replacement for the {@code restoreJob} {@code <batch:job>} graph previously defined in
 * {@code applicationContext.xml}.
 *
 * <p>The job structure is reproduced exactly:
 *
 * <ul>
 *   <li>job listeners {@code backupFacade}, {@code restoreJobExecutionListener},
 *       {@code genericRestoreJobExecutionListener}
 *   <li>seven chunk steps ({@code restoreNamespaceInfos} &hellip; {@code restoreLayerGroupInfos}) followed by four
 *       tasklet steps ({@code restoreGeoServerInfos}, {@code restoreGeoServerSecurityManager},
 *       {@code genericTaskletsRestore}, {@code finalizeRestore})
 *   <li>each chunk uses the {@code chunkTimeoutPolicy} ({@link TimeoutTerminationPolicy} of 600000 ms) completion
 *       policy
 *   <li>each chunk step and the {@code restoreGeoServerInfos} / {@code restoreGeoServerSecurityManager} tasklet steps
 *       carry the {@code restoreExecutionPromotionListener} step listener and {@code allowStartIfComplete=true}
 * </ul>
 *
 * <p>The readers, processors and writers were {@code scope="step"} in the XML and are reproduced here as
 * {@link StepScope}-d prototype beans. The {@code <batch:fail>/<batch:stop>/<batch:next>} transitions are reproduced
 * via the Spring Batch 6 flow transition API.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
@Configuration
public class RestoreJobConfiguration {

    /** 10 minutes timeout for each restore chunk (matches the {@code chunkTimeoutPolicy} bean in the XML). */
    private static final long CHUNK_TIMEOUT_MS = 600000L;

    private static final Logger LOGGER = Logging.getLogger(RestoreJobConfiguration.class);

    /**
     * Completion policy bean reproducing {@code <bean id="chunkTimeoutPolicy" ...>}. Although in the XML it was a
     * shared singleton, a {@link TimeoutTerminationPolicy} carries no mutable shared state, so a singleton bean is
     * faithful.
     */
    @Bean
    public CompletionPolicy chunkTimeoutPolicy() {
        return new TimeoutTerminationPolicy(CHUNK_TIMEOUT_MS);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Step-scoped chunk components (reader / processor / writer), one set per Catalog info type.
    // The reader delegates to a CatalogFileReader configured with strict mode and the fragment root element name(s),
    // and is fed the resources matching "<input.file.path>/<name>.dat.*".
    // -------------------------------------------------------------------------------------------------------------

    /**
     * Expand the {@code <inputFilePath>/<datFileGlob>} wildcard into a {@link Resource} array, mirroring the Spring XML
     * String-to-{@code Resource[]} conversion that backed the {@code resources} property. The job parameter holds a
     * plain filesystem path, so it is turned into a {@code file:} URL before pattern matching.
     */
    private Resource[] resolveResources(String inputFilePath, String datFileGlob) {
        // input.file.path is a "file:" URL string pointing at the extracted-archive directory. Resolve it to a real
        // directory and list the matching "<name>.dat.*" fragments directly. Expanding the wildcard through a
        // PathMatchingResourcePatternResolver is fragile across platforms: on Windows the value arrives as
        // "file://D:/..." (the drive letter is then parsed as a URL host) or with backslashes, and the Ant matcher
        // only understands forward slashes, so the pattern silently expands to ZERO resources - the restore then reads
        // nothing and, when restoring into a catalog that already holds the objects, still appears to succeed.
        String path = inputFilePath.replace('\\', '/');
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        // Drop the leading slash(es) that precede a Windows drive letter ("/D:/..", "//D:/..", "///D:/.."); POSIX
        // absolute paths ("/var/..") carry no drive letter and are left untouched.
        path = path.replaceFirst("^/+(?=[A-Za-z]:)", "");

        File dir = new File(path);
        int star = datFileGlob.indexOf('*');
        String prefix = star >= 0 ? datFileGlob.substring(0, star) : datFileGlob;
        File[] matches = dir.listFiles((d, name) -> name.startsWith(prefix));
        if (matches == null || matches.length == 0) {
            LOGGER.warning("Restore input directory '" + dir + "' contained no files matching '" + datFileGlob + "'.");
            return new Resource[0];
        }
        Resource[] resources = new Resource[matches.length];
        for (int i = 0; i < matches.length; i++) {
            resources[i] = new FileSystemResource(matches[i]);
        }
        return resources;
    }

    private <T> CatalogMultiResourceItemReader<T> readerWithSingleFragment(
            Class<T> clazz,
            Backup backupFacade,
            boolean strict,
            String fragmentRootElementName,
            String inputFilePath,
            String datFileGlob) {
        CatalogFileReader<T> delegate = new CatalogFileReader<>(clazz, backupFacade);
        delegate.setStrict(strict);
        delegate.setFragmentRootElementName(fragmentRootElementName);

        CatalogMultiResourceItemReader<T> reader = new CatalogMultiResourceItemReader<>(clazz, backupFacade);
        reader.setDelegate(delegate);
        reader.setResources(resolveResources(inputFilePath, datFileGlob));
        return reader;
    }

    private <T> CatalogMultiResourceItemReader<T> readerWithFragmentList(
            Class<T> clazz,
            Backup backupFacade,
            boolean strict,
            String[] fragmentRootElementNames,
            String inputFilePath,
            String datFileGlob) {
        CatalogFileReader<T> delegate = new CatalogFileReader<>(clazz, backupFacade);
        delegate.setStrict(strict);
        delegate.setFragmentRootElementNames(fragmentRootElementNames);

        CatalogMultiResourceItemReader<T> reader = new CatalogMultiResourceItemReader<>(clazz, backupFacade);
        reader.setDelegate(delegate);
        reader.setResources(resolveResources(inputFilePath, datFileGlob));
        return reader;
    }

    private <T> CatalogItemProcessor<T> processor(Class<T> clazz, Backup backupFacade) {
        return new CatalogItemProcessor<>(clazz, backupFacade);
    }

    private <T> CatalogItemWriter<T> writer(Class<T> clazz, Backup backupFacade) {
        return new CatalogItemWriter<>(clazz, backupFacade);
    }

    // ---- NamespaceInfo (fragment "namespace", strict=false) -------------------------------------------------------

    @Bean
    @StepScope
    public CatalogMultiResourceItemReader<NamespaceInfo> restoreNamespaceReader(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['input.file.path']}") String inputFilePath) {
        return readerWithSingleFragment(
                NamespaceInfo.class, backupFacade, false, "namespace", inputFilePath, "namespace.dat.*");
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<NamespaceInfo> restoreNamespaceProcessor(
            @Qualifier("backupFacade") Backup backupFacade) {
        return processor(NamespaceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemWriter<NamespaceInfo> restoreNamespaceWriter(@Qualifier("backupFacade") Backup backupFacade) {
        return writer(NamespaceInfo.class, backupFacade);
    }

    // ---- WorkspaceInfo (fragment "workspace", strict=true) --------------------------------------------------------

    @Bean
    @StepScope
    public CatalogMultiResourceItemReader<WorkspaceInfo> restoreWorkspaceReader(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['input.file.path']}") String inputFilePath) {
        return readerWithSingleFragment(
                WorkspaceInfo.class, backupFacade, true, "workspace", inputFilePath, "workspace.dat.*");
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<WorkspaceInfo> restoreWorkspaceProcessor(
            @Qualifier("backupFacade") Backup backupFacade) {
        return processor(WorkspaceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemWriter<WorkspaceInfo> restoreWorkspaceWriter(@Qualifier("backupFacade") Backup backupFacade) {
        return writer(WorkspaceInfo.class, backupFacade);
    }

    // ---- StoreInfo (fragments dataStore/coverageStore/wmsStore/wmtsStore, strict=true) ----------------------------

    @Bean
    @StepScope
    public CatalogMultiResourceItemReader<StoreInfo> restoreStoreReader(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['input.file.path']}") String inputFilePath) {
        return readerWithFragmentList(
                StoreInfo.class,
                backupFacade,
                true,
                new String[] {"dataStore", "coverageStore", "wmsStore", "wmtsStore"},
                inputFilePath,
                "store.dat.*");
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<StoreInfo> restoreStoreProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(StoreInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemWriter<StoreInfo> restoreStoreWriter(@Qualifier("backupFacade") Backup backupFacade) {
        return writer(StoreInfo.class, backupFacade);
    }

    // ---- ResourceInfo (fragments featureType/coverage/wmsLayer/wmtsLayer, strict=true) ----------------------------

    @Bean
    @StepScope
    public CatalogMultiResourceItemReader<ResourceInfo> restoreResourceReader(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['input.file.path']}") String inputFilePath) {
        return readerWithFragmentList(
                ResourceInfo.class,
                backupFacade,
                true,
                new String[] {"featureType", "coverage", "wmsLayer", "wmtsLayer"},
                inputFilePath,
                "resource.dat.*");
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<ResourceInfo> restoreResourceProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(ResourceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemWriter<ResourceInfo> restoreResourceWriter(@Qualifier("backupFacade") Backup backupFacade) {
        return writer(ResourceInfo.class, backupFacade);
    }

    // ---- StyleInfo (fragment "style", strict=true) ----------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogMultiResourceItemReader<StyleInfo> restoreStyleReader(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['input.file.path']}") String inputFilePath) {
        // XML used the plural setter with a single value ("style"); reproduced as a single-element list.
        return readerWithFragmentList(
                StyleInfo.class, backupFacade, true, new String[] {"style"}, inputFilePath, "style.dat.*");
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<StyleInfo> restoreStyleProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(StyleInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemWriter<StyleInfo> restoreStyleWriter(@Qualifier("backupFacade") Backup backupFacade) {
        return writer(StyleInfo.class, backupFacade);
    }

    // ---- LayerInfo (fragment "layer", strict=true) ----------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogMultiResourceItemReader<LayerInfo> restoreLayerReader(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['input.file.path']}") String inputFilePath) {
        return readerWithFragmentList(
                LayerInfo.class, backupFacade, true, new String[] {"layer"}, inputFilePath, "layer.dat.*");
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<LayerInfo> restoreLayerProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(LayerInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemWriter<LayerInfo> restoreLayerWriter(@Qualifier("backupFacade") Backup backupFacade) {
        return writer(LayerInfo.class, backupFacade);
    }

    // ---- LayerGroupInfo (fragment "layerGroup", strict=true) ------------------------------------------------------

    @Bean
    @StepScope
    public CatalogMultiResourceItemReader<LayerGroupInfo> restoreLayerGroupReader(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['input.file.path']}") String inputFilePath) {
        return readerWithFragmentList(
                LayerGroupInfo.class,
                backupFacade,
                true,
                new String[] {"layerGroup"},
                inputFilePath,
                "layerGroup.dat.*");
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<LayerGroupInfo> restoreLayerGroupProcessor(
            @Qualifier("backupFacade") Backup backupFacade) {
        return processor(LayerGroupInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemWriter<LayerGroupInfo> restoreLayerGroupWriter(@Qualifier("backupFacade") Backup backupFacade) {
        return writer(LayerGroupInfo.class, backupFacade);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Chunk steps. Each uses the chunkTimeoutPolicy completion policy, allowStartIfComplete(true) and the
    // restoreExecutionPromotionListener step listener, matching the XML.
    // -------------------------------------------------------------------------------------------------------------

    // chunk(CompletionPolicy, txMgr) keeps the restore's TimeoutTerminationPolicy (one atomic chunk with a 10-minute
    // safety timeout). The new ChunkOrientedStepBuilder.chunk(int) is fixed-size only, so there is no
    // behaviour-preserving migration; suppress until the restore chunking strategy is revisited.
    @SuppressWarnings("removal")
    private <T> Step chunkStep(
            String name,
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CompletionPolicy chunkTimeoutPolicy,
            ItemReader<T> reader,
            ItemProcessor<T, T> processor,
            ItemWriter<T> writer,
            StepExecutionListener promotionListener) {
        return new StepBuilder(name, jobRepository)
                .<T, T>chunk(chunkTimeoutPolicy, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(promotionListener)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step restoreNamespaceInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("chunkTimeoutPolicy") CompletionPolicy chunkTimeoutPolicy,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener,
            CatalogMultiResourceItemReader<NamespaceInfo> restoreNamespaceReader,
            CatalogItemProcessor<NamespaceInfo> restoreNamespaceProcessor,
            CatalogItemWriter<NamespaceInfo> restoreNamespaceWriter) {
        return chunkStep(
                "restoreNamespaceInfos",
                jobRepository,
                transactionManager,
                chunkTimeoutPolicy,
                restoreNamespaceReader,
                restoreNamespaceProcessor,
                restoreNamespaceWriter,
                restoreExecutionPromotionListener);
    }

    @Bean
    public Step restoreWorkspaceInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("chunkTimeoutPolicy") CompletionPolicy chunkTimeoutPolicy,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener,
            CatalogMultiResourceItemReader<WorkspaceInfo> restoreWorkspaceReader,
            CatalogItemProcessor<WorkspaceInfo> restoreWorkspaceProcessor,
            CatalogItemWriter<WorkspaceInfo> restoreWorkspaceWriter) {
        return chunkStep(
                "restoreWorkspaceInfos",
                jobRepository,
                transactionManager,
                chunkTimeoutPolicy,
                restoreWorkspaceReader,
                restoreWorkspaceProcessor,
                restoreWorkspaceWriter,
                restoreExecutionPromotionListener);
    }

    @Bean
    public Step restoreStoreInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("chunkTimeoutPolicy") CompletionPolicy chunkTimeoutPolicy,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener,
            CatalogMultiResourceItemReader<StoreInfo> restoreStoreReader,
            CatalogItemProcessor<StoreInfo> restoreStoreProcessor,
            CatalogItemWriter<StoreInfo> restoreStoreWriter) {
        return chunkStep(
                "restoreStoreInfos",
                jobRepository,
                transactionManager,
                chunkTimeoutPolicy,
                restoreStoreReader,
                restoreStoreProcessor,
                restoreStoreWriter,
                restoreExecutionPromotionListener);
    }

    @Bean
    public Step restoreResourceInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("chunkTimeoutPolicy") CompletionPolicy chunkTimeoutPolicy,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener,
            CatalogMultiResourceItemReader<ResourceInfo> restoreResourceReader,
            CatalogItemProcessor<ResourceInfo> restoreResourceProcessor,
            CatalogItemWriter<ResourceInfo> restoreResourceWriter) {
        return chunkStep(
                "restoreResourceInfos",
                jobRepository,
                transactionManager,
                chunkTimeoutPolicy,
                restoreResourceReader,
                restoreResourceProcessor,
                restoreResourceWriter,
                restoreExecutionPromotionListener);
    }

    @Bean
    public Step restoreStyleInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("chunkTimeoutPolicy") CompletionPolicy chunkTimeoutPolicy,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener,
            CatalogMultiResourceItemReader<StyleInfo> restoreStyleReader,
            CatalogItemProcessor<StyleInfo> restoreStyleProcessor,
            CatalogItemWriter<StyleInfo> restoreStyleWriter) {
        return chunkStep(
                "restoreStyleInfos",
                jobRepository,
                transactionManager,
                chunkTimeoutPolicy,
                restoreStyleReader,
                restoreStyleProcessor,
                restoreStyleWriter,
                restoreExecutionPromotionListener);
    }

    @Bean
    public Step restoreLayerInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("chunkTimeoutPolicy") CompletionPolicy chunkTimeoutPolicy,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener,
            CatalogMultiResourceItemReader<LayerInfo> restoreLayerReader,
            CatalogItemProcessor<LayerInfo> restoreLayerProcessor,
            CatalogItemWriter<LayerInfo> restoreLayerWriter) {
        return chunkStep(
                "restoreLayerInfos",
                jobRepository,
                transactionManager,
                chunkTimeoutPolicy,
                restoreLayerReader,
                restoreLayerProcessor,
                restoreLayerWriter,
                restoreExecutionPromotionListener);
    }

    @Bean
    public Step restoreLayerGroupInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("chunkTimeoutPolicy") CompletionPolicy chunkTimeoutPolicy,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener,
            CatalogMultiResourceItemReader<LayerGroupInfo> restoreLayerGroupReader,
            CatalogItemProcessor<LayerGroupInfo> restoreLayerGroupProcessor,
            CatalogItemWriter<LayerGroupInfo> restoreLayerGroupWriter) {
        return chunkStep(
                "restoreLayerGroupInfos",
                jobRepository,
                transactionManager,
                chunkTimeoutPolicy,
                restoreLayerGroupReader,
                restoreLayerGroupProcessor,
                restoreLayerGroupWriter,
                restoreExecutionPromotionListener);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Tasklet steps. restoreGeoServerInfos / restoreGeoServerSecurityManager carry the promotion listener and
    // allowStartIfComplete(true); genericTaskletsRestore allows start-if-complete; finalizeRestore is terminal.
    // -------------------------------------------------------------------------------------------------------------

    @Bean
    public Step restoreGeoServerInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("catalogBackupTasklet") Tasklet catalogBackupTasklet,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener) {
        return new StepBuilder("restoreGeoServerInfos", jobRepository)
                .tasklet(catalogBackupTasklet, transactionManager)
                .listener(restoreExecutionPromotionListener)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step restoreGeoServerSecurityManager(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("catalogSecurityManagerTasklet") Tasklet catalogSecurityManagerTasklet,
            @Qualifier("restoreExecutionPromotionListener") StepExecutionListener restoreExecutionPromotionListener) {
        return new StepBuilder("restoreGeoServerSecurityManager", jobRepository)
                .tasklet(catalogSecurityManagerTasklet, transactionManager)
                .listener(restoreExecutionPromotionListener)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step genericTaskletsRestore(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("genericTasklet") Tasklet genericTasklet) {
        return new StepBuilder("genericTaskletsRestore", jobRepository)
                .tasklet(genericTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step finalizeRestore(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("finalizeRestoreTasklet") Tasklet finalizeRestoreTasklet) {
        return new StepBuilder("finalizeRestore", jobRepository)
                .tasklet(finalizeRestoreTasklet, transactionManager)
                .build();
    }

    // -------------------------------------------------------------------------------------------------------------
    // The restoreJob assembling the eleven steps with the fail / stop(restart=next) / next(*->next) transitions
    // reproduced from the XML. finalizeRestore is terminal (no outgoing transition).
    // -------------------------------------------------------------------------------------------------------------

    @Bean
    public Job restoreJob(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("backupFacade") JobExecutionListener backupFacade,
            @Qualifier("restoreJobExecutionListener") JobExecutionListener restoreJobExecutionListener,
            @Qualifier("genericRestoreJobExecutionListener") JobExecutionListener genericRestoreJobExecutionListener,
            @Qualifier("restoreNamespaceInfos") Step restoreNamespaceInfos,
            @Qualifier("restoreWorkspaceInfos") Step restoreWorkspaceInfos,
            @Qualifier("restoreStoreInfos") Step restoreStoreInfos,
            @Qualifier("restoreResourceInfos") Step restoreResourceInfos,
            @Qualifier("restoreStyleInfos") Step restoreStyleInfos,
            @Qualifier("restoreLayerInfos") Step restoreLayerInfos,
            @Qualifier("restoreLayerGroupInfos") Step restoreLayerGroupInfos,
            @Qualifier("restoreGeoServerInfos") Step restoreGeoServerInfos,
            @Qualifier("restoreGeoServerSecurityManager") Step restoreGeoServerSecurityManager,
            @Qualifier("genericTaskletsRestore") Step genericTaskletsRestore,
            @Qualifier("finalizeRestore") Step finalizeRestore) {

        return new JobBuilder("restoreJob", jobRepository)
                .listener(backupFacade)
                .listener(restoreJobExecutionListener)
                .listener(genericRestoreJobExecutionListener)
                .start(restoreNamespaceInfos)
                .on("FAILED")
                .fail()
                .from(restoreNamespaceInfos)
                .on("STOPPED")
                .stopAndRestart(restoreWorkspaceInfos)
                .from(restoreNamespaceInfos)
                .on("*")
                .to(restoreWorkspaceInfos)
                .from(restoreWorkspaceInfos)
                .on("FAILED")
                .fail()
                .from(restoreWorkspaceInfos)
                .on("STOPPED")
                .stopAndRestart(restoreStoreInfos)
                .from(restoreWorkspaceInfos)
                .on("*")
                .to(restoreStoreInfos)
                .from(restoreStoreInfos)
                .on("FAILED")
                .fail()
                .from(restoreStoreInfos)
                .on("STOPPED")
                .stopAndRestart(restoreResourceInfos)
                .from(restoreStoreInfos)
                .on("*")
                .to(restoreResourceInfos)
                .from(restoreResourceInfos)
                .on("FAILED")
                .fail()
                .from(restoreResourceInfos)
                .on("STOPPED")
                .stopAndRestart(restoreStyleInfos)
                .from(restoreResourceInfos)
                .on("*")
                .to(restoreStyleInfos)
                .from(restoreStyleInfos)
                .on("FAILED")
                .fail()
                .from(restoreStyleInfos)
                .on("STOPPED")
                .stopAndRestart(restoreLayerInfos)
                .from(restoreStyleInfos)
                .on("*")
                .to(restoreLayerInfos)
                .from(restoreLayerInfos)
                .on("FAILED")
                .fail()
                .from(restoreLayerInfos)
                .on("STOPPED")
                .stopAndRestart(restoreLayerGroupInfos)
                .from(restoreLayerInfos)
                .on("*")
                .to(restoreLayerGroupInfos)
                .from(restoreLayerGroupInfos)
                .on("FAILED")
                .fail()
                .from(restoreLayerGroupInfos)
                .on("STOPPED")
                .stopAndRestart(restoreGeoServerInfos)
                .from(restoreLayerGroupInfos)
                .on("*")
                .to(restoreGeoServerInfos)
                .from(restoreGeoServerInfos)
                .on("FAILED")
                .fail()
                .from(restoreGeoServerInfos)
                .on("STOPPED")
                .stopAndRestart(restoreGeoServerSecurityManager)
                .from(restoreGeoServerInfos)
                .on("*")
                .to(restoreGeoServerSecurityManager)
                .from(restoreGeoServerSecurityManager)
                .on("FAILED")
                .fail()
                .from(restoreGeoServerSecurityManager)
                .on("STOPPED")
                .stopAndRestart(genericTaskletsRestore)
                .from(restoreGeoServerSecurityManager)
                .on("*")
                .to(genericTaskletsRestore)
                .from(genericTaskletsRestore)
                .on("FAILED")
                .fail()
                .from(genericTaskletsRestore)
                .on("STOPPED")
                .stopAndRestart(finalizeRestore)
                .from(genericTaskletsRestore)
                .on("*")
                .to(finalizeRestore)
                .end()
                .build();
    }
}
