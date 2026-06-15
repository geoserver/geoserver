/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.config;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.processor.CatalogItemProcessor;
import org.geoserver.backuprestore.reader.CatalogItemReader;
import org.geoserver.backuprestore.writer.CatalogFileWriter;
import org.geoserver.backuprestore.writer.CatalogMultiResourceItemWriter;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Java replacement for the {@code backupJob} {@code <batch:job>} graph previously defined in
 * {@code applicationContext.xml}.
 *
 * <p>The job structure is reproduced exactly:
 *
 * <ul>
 *   <li>job listeners {@code backupFacade}, {@code backupJobExecutionListener},
 *       {@code genericBackupJobExecutionListener}
 *   <li>a split ({@code backupGeoServerGlobalsFlow}) over a {@link SyncTaskExecutor} (sequential, matching the XML's
 *       default executor) containing three single-step flows ({@code backupGeoServerInfos},
 *       {@code backupGeoServerSecurityManager}, {@code genericTaskletsBackup})
 *   <li>seven chunk steps ({@code backupWorkspaceInfos} &hellip; {@code backupStyleInfos}), each with a commit interval
 *       of 1000 and {@code allowStartIfComplete=true}
 *   <li>a final tasklet step {@code finalizeBackup}
 * </ul>
 *
 * <p>The readers, processors and writers were {@code scope="step"} in the XML and are reproduced here as
 * {@link StepScope}-d prototype beans. The {@code <batch:fail>/<batch:stop>/<batch:next>} transitions are reproduced
 * via the Spring Batch 6 {@link FlowBuilder} transition API.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
@Configuration
public class BackupJobConfiguration {

    private static final int COMMIT_INTERVAL = 1000;

    private static final int ITEM_COUNT_LIMIT_PER_RESOURCE = 1000;

    // -------------------------------------------------------------------------------------------------------------
    // Step-scoped chunk components (reader / processor / writer), one set per Catalog info type.
    // These reproduce the scope="step" beans nested in the XML <batch:chunk> elements.
    // -------------------------------------------------------------------------------------------------------------

    private <T> CatalogItemReader<T> reader(Class<T> clazz, Backup backupFacade) {
        return new CatalogItemReader<>(clazz, backupFacade);
    }

    private <T> CatalogItemProcessor<T> processor(Class<T> clazz, Backup backupFacade) {
        return new CatalogItemProcessor<>(clazz, backupFacade);
    }

    private <T> CatalogMultiResourceItemWriter<T> writer(
            Class<T> clazz, Backup backupFacade, String outputFilePath, String datFileName) {
        CatalogFileWriter<T> delegate = new CatalogFileWriter<>(clazz, backupFacade);
        delegate.setAppendAllowed(true);

        CatalogMultiResourceItemWriter<T> writer = new CatalogMultiResourceItemWriter<>(clazz, backupFacade);
        writer.setDelegate(delegate);
        writer.setItemCountLimitPerResource(ITEM_COUNT_LIMIT_PER_RESOURCE);
        // output.file.path is a "file:" URL string; resolve "<path>/<dat>" through Spring's resource loader exactly as
        // the former XML <property name="resource" value="#{...}/<dat>"/> did (a bare java.io.File cannot parse a URL).
        // DefaultResourceLoader returns a FileUrlResource for file: URLs, which is a WritableResource.
        writer.setResource(
                (WritableResource) new DefaultResourceLoader().getResource(outputFilePath + "/" + datFileName));
        return writer;
    }

    // ---- WorkspaceInfo --------------------------------------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogItemReader<WorkspaceInfo> backupWorkspaceReader(@Qualifier("backupFacade") Backup backupFacade) {
        return reader(WorkspaceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<WorkspaceInfo> backupWorkspaceProcessor(
            @Qualifier("backupFacade") Backup backupFacade) {
        return processor(WorkspaceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogMultiResourceItemWriter<WorkspaceInfo> backupWorkspaceWriter(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['output.file.path']}") String outputFilePath) {
        return writer(WorkspaceInfo.class, backupFacade, outputFilePath, "workspace.dat");
    }

    // ---- NamespaceInfo --------------------------------------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogItemReader<NamespaceInfo> backupNamespaceReader(@Qualifier("backupFacade") Backup backupFacade) {
        return reader(NamespaceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<NamespaceInfo> backupNamespaceProcessor(
            @Qualifier("backupFacade") Backup backupFacade) {
        return processor(NamespaceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogMultiResourceItemWriter<NamespaceInfo> backupNamespaceWriter(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['output.file.path']}") String outputFilePath) {
        return writer(NamespaceInfo.class, backupFacade, outputFilePath, "namespace.dat");
    }

    // ---- StoreInfo ------------------------------------------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogItemReader<StoreInfo> backupStoreReader(@Qualifier("backupFacade") Backup backupFacade) {
        return reader(StoreInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<StoreInfo> backupStoreProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(StoreInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogMultiResourceItemWriter<StoreInfo> backupStoreWriter(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['output.file.path']}") String outputFilePath) {
        return writer(StoreInfo.class, backupFacade, outputFilePath, "store.dat");
    }

    // ---- ResourceInfo ---------------------------------------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogItemReader<ResourceInfo> backupResourceReader(@Qualifier("backupFacade") Backup backupFacade) {
        return reader(ResourceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<ResourceInfo> backupResourceProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(ResourceInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogMultiResourceItemWriter<ResourceInfo> backupResourceWriter(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['output.file.path']}") String outputFilePath) {
        return writer(ResourceInfo.class, backupFacade, outputFilePath, "resource.dat");
    }

    // ---- LayerInfo ------------------------------------------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogItemReader<LayerInfo> backupLayerReader(@Qualifier("backupFacade") Backup backupFacade) {
        return reader(LayerInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<LayerInfo> backupLayerProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(LayerInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogMultiResourceItemWriter<LayerInfo> backupLayerWriter(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['output.file.path']}") String outputFilePath) {
        return writer(LayerInfo.class, backupFacade, outputFilePath, "layer.dat");
    }

    // ---- LayerGroupInfo -------------------------------------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogItemReader<LayerGroupInfo> backupLayerGroupReader(@Qualifier("backupFacade") Backup backupFacade) {
        return reader(LayerGroupInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<LayerGroupInfo> backupLayerGroupProcessor(
            @Qualifier("backupFacade") Backup backupFacade) {
        return processor(LayerGroupInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogMultiResourceItemWriter<LayerGroupInfo> backupLayerGroupWriter(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['output.file.path']}") String outputFilePath) {
        return writer(LayerGroupInfo.class, backupFacade, outputFilePath, "layerGroup.dat");
    }

    // ---- StyleInfo ------------------------------------------------------------------------------------------------

    @Bean
    @StepScope
    public CatalogItemReader<StyleInfo> backupStyleReader(@Qualifier("backupFacade") Backup backupFacade) {
        return reader(StyleInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogItemProcessor<StyleInfo> backupStyleProcessor(@Qualifier("backupFacade") Backup backupFacade) {
        return processor(StyleInfo.class, backupFacade);
    }

    @Bean
    @StepScope
    public CatalogMultiResourceItemWriter<StyleInfo> backupStyleWriter(
            @Qualifier("backupFacade") Backup backupFacade,
            @Value("#{jobParameters['output.file.path']}") String outputFilePath) {
        return writer(StyleInfo.class, backupFacade, outputFilePath, "style.dat");
    }

    // -------------------------------------------------------------------------------------------------------------
    // Chunk steps. Each uses a commit-interval of 1000 and allowStartIfComplete(true), matching the XML.
    // Step-scoped components are referenced through the bean methods so that Spring injects the scoped proxies.
    // -------------------------------------------------------------------------------------------------------------

    private <T> Step chunkStep(
            String name,
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<T> reader,
            ItemProcessor<T, T> processor,
            ItemWriter<T> writer) {
        return new StepBuilder(name, jobRepository)
                .<T, T>chunk(COMMIT_INTERVAL)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step backupWorkspaceInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            CatalogItemReader<WorkspaceInfo> backupWorkspaceReader,
            CatalogItemProcessor<WorkspaceInfo> backupWorkspaceProcessor,
            CatalogMultiResourceItemWriter<WorkspaceInfo> backupWorkspaceWriter) {
        return chunkStep(
                "backupWorkspaceInfos",
                jobRepository,
                transactionManager,
                backupWorkspaceReader,
                backupWorkspaceProcessor,
                backupWorkspaceWriter);
    }

    @Bean
    public Step backupNamespaceInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            CatalogItemReader<NamespaceInfo> backupNamespaceReader,
            CatalogItemProcessor<NamespaceInfo> backupNamespaceProcessor,
            CatalogMultiResourceItemWriter<NamespaceInfo> backupNamespaceWriter) {
        return chunkStep(
                "backupNamespaceInfos",
                jobRepository,
                transactionManager,
                backupNamespaceReader,
                backupNamespaceProcessor,
                backupNamespaceWriter);
    }

    @Bean
    public Step backupStoreInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            CatalogItemReader<StoreInfo> backupStoreReader,
            CatalogItemProcessor<StoreInfo> backupStoreProcessor,
            CatalogMultiResourceItemWriter<StoreInfo> backupStoreWriter) {
        return chunkStep(
                "backupStoreInfos",
                jobRepository,
                transactionManager,
                backupStoreReader,
                backupStoreProcessor,
                backupStoreWriter);
    }

    @Bean
    public Step backupResourceInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            CatalogItemReader<ResourceInfo> backupResourceReader,
            CatalogItemProcessor<ResourceInfo> backupResourceProcessor,
            CatalogMultiResourceItemWriter<ResourceInfo> backupResourceWriter) {
        return chunkStep(
                "backupResourceInfos",
                jobRepository,
                transactionManager,
                backupResourceReader,
                backupResourceProcessor,
                backupResourceWriter);
    }

    @Bean
    public Step backupLayerInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            CatalogItemReader<LayerInfo> backupLayerReader,
            CatalogItemProcessor<LayerInfo> backupLayerProcessor,
            CatalogMultiResourceItemWriter<LayerInfo> backupLayerWriter) {
        return chunkStep(
                "backupLayerInfos",
                jobRepository,
                transactionManager,
                backupLayerReader,
                backupLayerProcessor,
                backupLayerWriter);
    }

    @Bean
    public Step backupLayerGroupInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            CatalogItemReader<LayerGroupInfo> backupLayerGroupReader,
            CatalogItemProcessor<LayerGroupInfo> backupLayerGroupProcessor,
            CatalogMultiResourceItemWriter<LayerGroupInfo> backupLayerGroupWriter) {
        return chunkStep(
                "backupLayerGroupInfos",
                jobRepository,
                transactionManager,
                backupLayerGroupReader,
                backupLayerGroupProcessor,
                backupLayerGroupWriter);
    }

    @Bean
    public Step backupStyleInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            CatalogItemReader<StyleInfo> backupStyleReader,
            CatalogItemProcessor<StyleInfo> backupStyleProcessor,
            CatalogMultiResourceItemWriter<StyleInfo> backupStyleWriter) {
        return chunkStep(
                "backupStyleInfos",
                jobRepository,
                transactionManager,
                backupStyleReader,
                backupStyleProcessor,
                backupStyleWriter);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Tasklet steps used in the parallel split and as the finalize step.
    // -------------------------------------------------------------------------------------------------------------

    @Bean
    public Step backupGeoServerInfos(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("catalogBackupTasklet") Tasklet catalogBackupTasklet) {
        return new StepBuilder("backupGeoServerInfos", jobRepository)
                .tasklet(catalogBackupTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step backupGeoServerSecurityManager(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("catalogSecurityManagerTasklet") Tasklet catalogSecurityManagerTasklet) {
        return new StepBuilder("backupGeoServerSecurityManager", jobRepository)
                .tasklet(catalogSecurityManagerTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step genericTaskletsBackup(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("genericTasklet") Tasklet genericTasklet) {
        return new StepBuilder("genericTaskletsBackup", jobRepository)
                .tasklet(genericTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step finalizeBackup(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("finalizeBackupTasklet") Tasklet finalizeBackupTasklet) {
        return new StepBuilder("finalizeBackup", jobRepository)
                .tasklet(finalizeBackupTasklet, transactionManager)
                .build();
    }

    // -------------------------------------------------------------------------------------------------------------
    // Single-step flows composing the parallel split, and the split itself.
    // -------------------------------------------------------------------------------------------------------------

    private Flow singleStepFlow(String name, Step step) {
        return new FlowBuilder<Flow>(name).start(step).build();
    }

    @Bean
    public Flow backupGeoServerGlobalsFlow(
            @Qualifier("backupGeoServerInfos") Step backupGeoServerInfos,
            @Qualifier("backupGeoServerSecurityManager") Step backupGeoServerSecurityManager,
            @Qualifier("genericTaskletsBackup") Step genericTaskletsBackup) {
        Flow geoServerInfosFlow = singleStepFlow("backupGeoServerInfosFlow", backupGeoServerInfos);
        Flow geoServerSecurityManagerFlow =
                singleStepFlow("backupGeoServerSecurityManagerFlow", backupGeoServerSecurityManager);
        Flow genericTaskletsBackupFlow = singleStepFlow("genericTaskletsBackupFlow", genericTaskletsBackup);

        // The XML <batch:split> declared no task-executor, so Spring Batch ran the three flows on the default
        // SyncTaskExecutor (sequentially). Reproduce that exactly rather than introducing parallelism.
        return new FlowBuilder<Flow>("backupGeoServerGlobalsFlow")
                .split(new SyncTaskExecutor())
                .add(geoServerInfosFlow, geoServerSecurityManagerFlow, genericTaskletsBackupFlow)
                .build();
    }

    // -------------------------------------------------------------------------------------------------------------
    // The backupJob assembling the split, the seven chunk steps and the finalize step, with the
    // fail / stop(restart=next) / next(*->next) transitions reproduced from the XML.
    // -------------------------------------------------------------------------------------------------------------

    @Bean
    public Job backupJob(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("backupFacade") JobExecutionListener backupFacade,
            @Qualifier("backupJobExecutionListener") JobExecutionListener backupJobExecutionListener,
            @Qualifier("genericBackupJobExecutionListener") JobExecutionListener genericBackupJobExecutionListener,
            @Qualifier("backupGeoServerGlobalsFlow") Flow backupGeoServerGlobalsFlow,
            @Qualifier("backupWorkspaceInfos") Step backupWorkspaceInfos,
            @Qualifier("backupNamespaceInfos") Step backupNamespaceInfos,
            @Qualifier("backupStoreInfos") Step backupStoreInfos,
            @Qualifier("backupResourceInfos") Step backupResourceInfos,
            @Qualifier("backupLayerInfos") Step backupLayerInfos,
            @Qualifier("backupLayerGroupInfos") Step backupLayerGroupInfos,
            @Qualifier("backupStyleInfos") Step backupStyleInfos,
            @Qualifier("finalizeBackup") Step finalizeBackup) {

        return new JobBuilder("backupJob", jobRepository)
                .listener(backupFacade)
                .listener(backupJobExecutionListener)
                .listener(genericBackupJobExecutionListener)
                .start(backupGeoServerGlobalsFlow)
                .on("FAILED")
                .fail()
                .from(backupGeoServerGlobalsFlow)
                .on("STOPPED")
                .stopAndRestart(backupWorkspaceInfos)
                .from(backupGeoServerGlobalsFlow)
                .on("*")
                .to(backupWorkspaceInfos)
                .from(backupWorkspaceInfos)
                .on("FAILED")
                .fail()
                .from(backupWorkspaceInfos)
                .on("STOPPED")
                .stopAndRestart(backupNamespaceInfos)
                .from(backupWorkspaceInfos)
                .on("*")
                .to(backupNamespaceInfos)
                .from(backupNamespaceInfos)
                .on("FAILED")
                .fail()
                .from(backupNamespaceInfos)
                .on("STOPPED")
                .stopAndRestart(backupStoreInfos)
                .from(backupNamespaceInfos)
                .on("*")
                .to(backupStoreInfos)
                .from(backupStoreInfos)
                .on("FAILED")
                .fail()
                .from(backupStoreInfos)
                .on("STOPPED")
                .stopAndRestart(backupResourceInfos)
                .from(backupStoreInfos)
                .on("*")
                .to(backupResourceInfos)
                .from(backupResourceInfos)
                .on("FAILED")
                .fail()
                .from(backupResourceInfos)
                .on("STOPPED")
                .stopAndRestart(backupLayerInfos)
                .from(backupResourceInfos)
                .on("*")
                .to(backupLayerInfos)
                .from(backupLayerInfos)
                .on("FAILED")
                .fail()
                .from(backupLayerInfos)
                .on("STOPPED")
                .stopAndRestart(backupLayerGroupInfos)
                .from(backupLayerInfos)
                .on("*")
                .to(backupLayerGroupInfos)
                .from(backupLayerGroupInfos)
                .on("FAILED")
                .fail()
                .from(backupLayerGroupInfos)
                .on("STOPPED")
                .stopAndRestart(backupStyleInfos)
                .from(backupLayerGroupInfos)
                .on("*")
                .to(backupStyleInfos)
                .from(backupStyleInfos)
                .on("FAILED")
                .fail()
                .from(backupStyleInfos)
                .on("STOPPED")
                .stopAndRestart(finalizeBackup)
                .from(backupStyleInfos)
                .on("*")
                .to(finalizeBackup)
                .end()
                .build();
    }
}
