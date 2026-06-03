/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.config;

import javax.sql.DataSource;
import org.geoserver.backuprestore.TolerantMapJobRegistry;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistrySmartInitializingSingleton;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Java replacement for {@code backup-batch-context.xml}: the in-memory Spring Batch 6 infrastructure.
 *
 * <p>This reproduces, bean-for-bean and name-for-name, the batch infrastructure that previously lived in
 * {@code backup-batch-context.xml}. See that file's header comment for the rationale behind using an embedded H2
 * database (a queryable, JVM-lifetime job repository) rather than a {@code ResourcelessJobRepository}.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
@Configuration
public class BatchInfrastructureConfiguration {

    /**
     * Registers the Spring Batch {@code step} bean scope. The {@code <batch:job>} XML namespace used to register this
     * automatically; with the job graph now defined in Java — and without {@code @EnableBatchProcessing}, whose
     * auto-configured job repository would clash with the custom embedded-H2 one below — it must be declared explicitly
     * so the {@code @StepScope} reader/processor/writer beans resolve. Must be {@code static} (it is a
     * {@code BeanFactoryPostProcessor}).
     */
    @Bean
    public static org.springframework.batch.core.scope.StepScope stepScope() {
        return new org.springframework.batch.core.scope.StepScope();
    }

    /** Registers the Spring Batch {@code job} bean scope (see {@link #stepScope()}). */
    @Bean
    public static org.springframework.batch.core.scope.JobScope jobScope() {
        return new org.springframework.batch.core.scope.JobScope();
    }

    /**
     * In-memory batch metadata store: an embedded H2 database seeded with the standard Spring Batch schema. A unique
     * database name is generated on every start-up so the metadata never outlives the JVM and never touches the data
     * directory.
     */
    @Bean
    public DataSource backupJobRepositoryDataSource() {
        // A uniquely-named in-memory H2, kept alive for the JVM lifetime with DB_CLOSE_DELAY=-1, then seeded with the
        // standard Batch schema. Spring's EmbeddedDatabaseBuilder doesn't expose DB_CLOSE_DELAY and a bare in-memory H2
        // drops its WHOLE schema when the last connection closes during an idle gap between jobs - which intermittently
        // failed later jobs (and hung tests) with "Table BATCH_JOB_EXECUTION not found (this database is empty)".
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:backupJobRepository-"
                        + java.util.UUID.randomUUID()
                        + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "sa",
                "");
        dataSource.setDriverClassName("org.h2.Driver");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));
        DatabasePopulatorUtils.execute(populator, dataSource);
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("backupJobRepositoryDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    public JobRepository jobRepository(
            @Qualifier("backupJobRepositoryDataSource") DataSource dataSource,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager)
            throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        // H2 does not support SERIALIZABLE create semantics well; READ_COMMITTED is sufficient here
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * {@link TolerantMapJobRegistry} (a {@code MapJobRegistry} that ignores duplicate registrations) is used instead of
     * the stock registry: in the full GeoServer assembly the backup/restore jobs are presented to the registry twice
     * and the strict registry would abort start-up with a {@code DuplicateJobException}.
     */
    @Bean
    public JobRegistry jobRegistry() {
        return new TolerantMapJobRegistry();
    }

    /**
     * Auto-registers the {@code Job} beans by name into the job registry (replaces
     * {@code JobRegistryBeanPostProcessor}).
     */
    @Bean
    public JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton(
            @Qualifier("jobRegistry") JobRegistry jobRegistry) {
        JobRegistrySmartInitializingSingleton singleton = new JobRegistrySmartInitializingSingleton();
        singleton.setJobRegistry(jobRegistry);
        return singleton;
    }

    /** Asynchronous job launcher ({@code TaskExecutorJobLauncher} replaces the removed {@code SimpleJobLauncher}). */
    @Bean
    public JobLauncher jobLauncherAsync(@Qualifier("jobRepository") JobRepository jobRepository) throws Exception {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(10);

        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(taskExecutor);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /** Job operator for monitoring / stop / restart / abandon of running executions. */
    @Bean
    public JobOperator jobOperator(
            @Qualifier("jobRepository") JobRepository jobRepository,
            @Qualifier("jobRegistry") JobRegistry jobRegistry,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager)
            throws Exception {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(10);

        JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
        factory.setJobRepository(jobRepository);
        factory.setJobRegistry(jobRegistry);
        factory.setTransactionManager(transactionManager);
        factory.setTaskExecutor(taskExecutor);
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
