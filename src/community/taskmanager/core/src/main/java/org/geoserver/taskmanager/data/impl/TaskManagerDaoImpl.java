/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Identifiable;
import org.geoserver.taskmanager.data.LatestBatchRun;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.SoftRemove;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional("tmTransactionManager")
public class TaskManagerDaoImpl implements TaskManagerDao {

    @Autowired private SessionFactory sf;

    protected final Session getSession() {
        Session session = sf.getCurrentSession();
        session.enableFilter("activeTaskFilter");
        session.enableFilter("activeBatchFilter");
        session.enableFilter("activeElementFilter");
        session.enableFilter("activeTaskElementFilter");
        return session;
    }

    protected final Session getSessionNoFilters() {
        Session session = sf.getCurrentSession();
        session.disableFilter("activeTaskFilter");
        session.disableFilter("activeBatchFilter");
        session.disableFilter("activeElementFilter");
        session.disableFilter("activeTaskElementFilter");
        return session;
    }

    @SuppressWarnings("unchecked")
    protected <T> T saveObject(T o) {
        o = (T) getSession().merge(o);
        getSession().flush();
        return o;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Identifiable> T reload(T object) {
        return (T) getSession().get(object.getClass(), object.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Identifiable> T lockReload(T object) {
        return (T)
                getSession()
                        .get(
                                object.getClass(),
                                object.getId(),
                                new LockOptions(LockMode.PESSIMISTIC_READ).setScope(true));
    }

    @Override
    public Run save(final Run run) {
        return initInternal(saveObject(run));
    }

    @Override
    public BatchRun save(final BatchRun br) {
        return initInternal(saveObject(br));
    }

    @Override
    public Configuration save(final Configuration config) {
        if (Hibernate.isInitialized(config.getBatches())) {
            for (Batch batch : config.getBatches().values()) {
                reorder(batch);
            }
        }
        return initInternal(saveObject(config));
    }

    protected void reorder(Batch batch) {
        if (Hibernate.isInitialized(batch.getElements())) {
            int i = 0;
            for (BatchElement element : batch.getElements()) {
                if (element.isActive()) {
                    element.setIndex(i++);
                } else {
                    element.setIndex(null);
                }
            }
        }
    }

    @Override
    public Batch save(final Batch batch) {
        reorder(batch);
        return initInternal(saveObject(batch));
    }

    @Override
    public List<Batch> getAllBatches() {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<BatchImpl> root = query.from(BatchImpl.class);
        root.fetch("elements", JoinType.LEFT);
        root.join("configuration", JoinType.LEFT);
        query.select(root);
        query.where(
                cb.equal(root.get("removeStamp"), 0L),
                cb.or(
                        cb.isNull(root.get("configuration")),
                        cb.equal(root.get("configuration").get("removeStamp"), 0L)));
        query.distinct(true);
        return getSession().createQuery(query).getResultList();
    }

    @Override
    public List<Batch> getViewableBatches() {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<BatchImpl> root = query.from(BatchImpl.class);
        root.fetch("latestBatchRun", JoinType.LEFT)
                .fetch("batchRun", JoinType.LEFT)
                .fetch("runs", JoinType.LEFT);
        root.join("configuration", JoinType.LEFT);
        query.select(root);
        query.where(
                cb.equal(root.get("removeStamp"), 0L),
                cb.or(
                        cb.isNull(root.get("configuration")),
                        cb.and(
                                cb.equal(root.get("configuration").get("removeStamp"), 0L),
                                cb.equal(root.get("configuration").get("validated"), true),
                                cb.not(cb.like(root.get("name"), "@%")))));
        query.distinct(true);
        return getSession().createQuery(query).getResultList();
    }

    @Override
    public void loadLatestBatchRuns(Configuration config) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<LatestBatchRun> query = cb.createQuery(LatestBatchRun.class);
        Root<LatestBatchRunImpl> root = query.from(LatestBatchRunImpl.class);
        root.join("batch").join("configuration");
        root.fetch("batchRun");
        query.select(root);
        query.where(cb.equal(root.get("batch").get("configuration").get("id"), config.getId()));

        for (LatestBatchRun lbr : getSession().createQuery(query).getResultList()) {
            BatchImpl b = ((BatchImpl) config.getBatches().get(lbr.getBatch().getName()));
            if (b != null) {
                b.setLatestBatchRun(lbr);
            }
        }
    }

    @Override
    public List<Configuration> getConfigurations(Boolean templates) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Configuration> query = cb.createQuery(Configuration.class);
        Root<ConfigurationImpl> root = query.from(ConfigurationImpl.class);
        query.select(root);
        if (templates != null) {
            query.where(
                    cb.equal(root.get("removeStamp"), 0L),
                    cb.equal(root.get("template"), templates));
        } else {
            query.where(cb.equal(root.get("removeStamp"), 0L));
        }
        query.distinct(true);
        return getSession().createQuery(query).getResultList();
    }

    @Override
    public Configuration getConfiguration(long id) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Configuration> query = cb.createQuery(Configuration.class);
        Root<ConfigurationImpl> root = query.from(ConfigurationImpl.class);
        query.select(root);
        query.where(cb.equal(root.get("id"), id));
        try {
            return getSession().createQuery(query).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Batch getBatch(long id) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<BatchImpl> root = query.from(BatchImpl.class);
        query.select(root);
        query.where(cb.equal(root.get("id"), id));
        try {
            return getSession().createQuery(query).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Configuration getConfiguration(final String name) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Configuration> query = cb.createQuery(Configuration.class);
        Root<ConfigurationImpl> root = query.from(ConfigurationImpl.class);
        query.select(root);
        query.where(cb.equal(root.get("name"), name), cb.equal(root.get("removeStamp"), 0L));
        try {
            return getSession().createQuery(query).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<Task> getTasksAvailableForBatch(Batch batch) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<TaskImpl> root = query.from(TaskImpl.class);
        root.join("configuration");
        root.fetch("batchElements", JoinType.LEFT);
        query.select(root);

        Subquery<Number> subQuery = query.subquery(Number.class);
        Root<BatchElementImpl> subRoot = subQuery.from(BatchElementImpl.class);
        subRoot.join("batch");
        subRoot.join("task");
        subQuery.select(subRoot.get("task").get("id"));
        subQuery.where(
                cb.equal(subRoot.get("batch").get("id"), batch.getId()),
                cb.equal(subRoot.get("removeStamp"), 0L));

        query.where(
                cb.equal(root.get("removeStamp"), 0L),
                cb.equal(root.get("configuration").get("removeStamp"), 0L),
                batch.getConfiguration() == null
                        ? cb.equal(root.get("configuration").get("template"), false)
                        : cb.equal(
                                root.get("configuration").get("id"),
                                batch.getConfiguration().getId()),
                cb.not(root.get("id").in(subQuery)));

        return getSession().createQuery(query).getResultList();
    }

    @Override
    public Batch getBatch(final String fullName) {
        String[] splitName = fullName.split(Batch.FULL_NAME_DIVISOR, 2);

        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<BatchImpl> root = query.from(BatchImpl.class);
        query.select(root);
        if (splitName.length > 1) {
            query.where(
                    cb.equal(root.get("name"), splitName[1]),
                    cb.equal(root.get("configuration").get("name"), splitName[0]),
                    cb.equal(root.get("configuration").get("removeStamp"), 0L));
        } else {
            query.where(cb.equal(root.get("name"), splitName[0]));
        }
        try {
            return getSession().createQuery(query).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<Batch> findBatches(
            final String workspacePattern,
            final String configNamePattern,
            final String namePattern) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<BatchImpl> root = query.from(BatchImpl.class);

        if (configNamePattern != null) {
            root.join("configuration");
            query.where(
                    cb.equal(root.get("removeStamp"), 0L),
                    cb.equal(root.get("configuration").get("removeStamp"), 0L),
                    cb.equal(root.get("configuration").get("template"), false),
                    cb.equal(root.get("configuration").get("validated"), true),
                    cb.not(cb.like(root.get("configuration").get("name"), "@%")),
                    cb.like(root.get("configuration").get("name"), configNamePattern),
                    cb.like(root.get("name"), namePattern),
                    workspacePattern == null
                            ? cb.isNull(root.get("configuration").get("workspace"))
                            : "%".equals(workspacePattern)
                                    ? cb.isTrue(cb.literal(true))
                                    : cb.like(
                                            root.get("configuration").get("workspace"),
                                            workspacePattern));
        } else {
            query.where(
                    cb.equal(root.get("removeStamp"), 0L),
                    cb.isNull(root.get("configuration")),
                    cb.like(root.get("name"), namePattern),
                    workspacePattern == null
                            ? cb.isNull(root.get("workspace"))
                            : "%".equals(workspacePattern)
                                    ? cb.isTrue(cb.literal(true))
                                    : cb.like(root.get("workspace"), workspacePattern));
        }

        query.select(root);
        return getSession().createQuery(query).getResultList();
    }

    @Override
    public List<Batch> findInitBatches(String workspacePattern, String configNamePattern) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<BatchImpl> root = query.from(BatchImpl.class);

        if (configNamePattern != null) {
            root.join("configuration");
            query.where(
                    cb.equal(root.get("removeStamp"), 0L),
                    cb.equal(root.get("configuration").get("removeStamp"), 0L),
                    cb.equal(root.get("configuration").get("template"), false),
                    cb.equal(root.get("configuration").get("validated"), false),
                    cb.like(root.get("configuration").get("name"), configNamePattern),
                    cb.like(root.get("name"), InitConfigUtil.INIT_BATCH),
                    workspacePattern == null
                            ? cb.isNull(root.get("configuration").get("workspace"))
                            : "%".equals(workspacePattern)
                                    ? cb.isTrue(cb.literal(true))
                                    : cb.like(
                                            root.get("configuration").get("workspace"),
                                            workspacePattern));
        }

        query.select(root);
        return getSession().createQuery(query).getResultList();
    }

    @Override
    public BatchElement getBatchElement(final Batch batch, final Task task) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<BatchElement> query = cb.createQuery(BatchElement.class);
        Root<BatchElementImpl> root = query.from(BatchElementImpl.class);
        root.join("batch");
        root.join("task");
        query.select(root);
        query.where(
                cb.equal(root.get("batch").get("id"), batch.getId()),
                cb.equal(root.get("task").get("id"), task.getId()));
        try {
            return getSession().createQuery(query).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Run getCurrentRun(final Task task) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Run> query = cb.createQuery(Run.class);
        Root<RunImpl> root = query.from(RunImpl.class);
        root.join("batchElement").join("task");
        query.select(root);
        query.where(
                cb.equal(root.get("batchElement").get("task").get("id"), task.getId()),
                cb.isNull(root.get("end")));
        try {
            return getSession()
                    .createQuery(query)
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<BatchRun> getCurrentBatchRuns(final Batch batch) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<BatchRun> query = cb.createQuery(BatchRun.class);
        Root<RunImpl> root = query.from(RunImpl.class);
        Join<Object, Object> batchRun = root.join("batchRun");
        batchRun.join("batch");
        query.select(root.get("batchRun"));
        query.groupBy(batchRun.get("id"));
        query.where(
                cb.equal(root.get("batchRun").get("batch").get("id"), batch.getId()),
                root.get("status")
                        .in(Run.Status.RUNNING, Run.Status.READY_TO_COMMIT, Run.Status.COMMITTING));
        return getSession().createQuery(query).getResultList();
    }

    @Override
    public List<BatchRun> getCurrentBatchRuns() {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<BatchRun> query = cb.createQuery(BatchRun.class);
        Root<RunImpl> root = query.from(RunImpl.class);
        Join<Object, Object> batchRun = root.join("batchRun");
        batchRun.join("batch");
        query.select(root.get("batchRun"));
        query.groupBy(batchRun.get("id"));
        query.where(
                root.get("status")
                        .in(Run.Status.RUNNING, Run.Status.READY_TO_COMMIT, Run.Status.COMMITTING));
        return getSession().createQuery(query).getResultList();
    }

    @Override
    public BatchRun getBatchRunBySchedulerReference(final String schedulerReference) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<BatchRun> query = cb.createQuery(BatchRun.class);
        Root<BatchRunImpl> root = query.from(BatchRunImpl.class);
        query.select(root);
        query.where(cb.equal(root.get("schedulerReference"), schedulerReference));
        query.orderBy(cb.desc(root.get("id"))); // assuming sequential id generation
        try {
            return getSession().createQuery(query).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Run getCommittingRun(final Task task) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Run> query = cb.createQuery(Run.class);
        Root<RunImpl> root = query.from(RunImpl.class);
        root.join("batchElement").join("task");
        query.select(root);
        query.where(
                cb.equal(root.get("batchElement").get("task").get("id"), task.getId()),
                cb.isNotNull(root.get("end")),
                cb.equal(root.get("status"), Run.Status.COMMITTING));
        try {
            return getSession()
                    .createQuery(query)
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Run getLatestRun(BatchElement batchElement) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Run> query = cb.createQuery(Run.class);
        Root<RunImpl> root = query.from(RunImpl.class);
        root.join("batchElement");
        query.select(root);
        query.where(cb.equal(root.get("batchElement").get("id"), batchElement.getId()));
        query.orderBy(cb.desc(root.get("start")));
        try {
            return getSession().createQuery(query).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @Transactional(
            transactionManager = "tmTransactionManager",
            propagation = Propagation.REQUIRES_NEW)
    public Configuration copyConfiguration(String configName) {
        ConfigurationImpl clone = (ConfigurationImpl) getConfiguration(configName);
        initInternal(clone);
        getSession().evict(clone);
        clone.setId(null);
        for (Attribute att : clone.getAttributes().values()) {
            att.setConfiguration(clone);
            ((AttributeImpl) att).setId(null);
        }
        for (Task task : clone.getTasks().values()) {
            task.setConfiguration(clone);
            ((TaskImpl) task).setId(null);
            ((TaskImpl) task).setBatchElements(new ArrayList<BatchElement>());
            for (Parameter param : task.getParameters().values()) {
                param.setTask(task);
                ((ParameterImpl) param).setId(null);
            }
        }
        for (Batch batch : clone.getBatches().values()) {
            batch.setConfiguration(clone);
            ((BatchImpl) batch).setId(null);
            for (BatchElement be : batch.getElements()) {
                be.setBatch(batch);
                be.setTask(clone.getTasks().get(be.getTask().getName()));
                ((BatchElementImpl) be).setId(null);
                if (Hibernate.isInitialized(be.getRuns())) {
                    be.getRuns().clear();
                }
                be.getTask().getBatchElements().add(be);
            }
            if (Hibernate.isInitialized(batch.getBatchRuns())) {
                batch.getBatchRuns().clear();
            }
            // disable cloned batches
            if (!clone.isTemplate()) {
                batch.setEnabled(false);
            }
        }
        return clone;
    }

    @Override
    public <T extends SoftRemove> T remove(T item) {
        item.setActive(false);
        return saveObject(item);
    }

    @Override
    public void delete(Batch batch) {
        batch = (Batch) getSessionNoFilters().get(BatchImpl.class, batch.getId());
        if (batch.getConfiguration() != null) {
            batch.getConfiguration().getBatches().remove(batch.getName());
        }
        getSessionNoFilters().delete(batch);
    }

    @Override
    public void delete(Configuration config) {
        getSessionNoFilters()
                .delete(getSessionNoFilters().get(ConfigurationImpl.class, config.getId()));
    }

    @Override
    public void delete(BatchElement batchElement) {
        batchElement =
                (BatchElement) getSession().get(BatchElementImpl.class, batchElement.getId());
        batchElement.getBatch().getElements().remove(batchElement);
        getSession().delete(batchElement);
    }

    @Override
    public void delete(Task task) {
        task = (Task) getSessionNoFilters().get(TaskImpl.class, task.getId());
        task.getConfiguration().getTasks().remove(task.getName());
        getSessionNoFilters().delete(task);
    }

    /**
     * Initialize lazy collection(s) in Batch Run
     *
     * @param be the Batch Run to be initialized
     * @return return the initialized Batch
     */
    @Override
    public BatchRun init(BatchRun br) {
        return initInternal(reload(br));
    }

    /**
     * Initialize lazy collection(s) in Batch - not including run history
     *
     * @param be the Batch to be initialized
     * @return return the initialized Batch
     */
    @Override
    public Batch init(Batch b) {
        return initInternal(reload(b));
    }

    /**
     * Initialize lazy collection(s) in Batch - including run history
     *
     * @param be the Batch to be initialized
     * @return return the initialized Batch
     */
    @Override
    public Batch initHistory(Batch b) {
        b = initInternal(reload(b));
        Hibernate.initialize(b.getBatchRuns());
        return b;
    }

    /**
     * Initialize lazy collection(s) in Configuration
     *
     * @param be the Configuration to be initialized
     * @return return the initialized Batch
     */
    @Override
    public Configuration init(Configuration c) {
        return initInternal(reload(c));
    }

    protected Configuration initInternal(Configuration c) {
        Hibernate.initialize(c.getTasks());
        Hibernate.initialize(c.getAttributes());
        Hibernate.initialize(c.getBatches());
        for (Batch b : c.getBatches().values()) {
            Hibernate.initialize(b.getElements());
        }
        for (Task t : c.getTasks().values()) {
            Hibernate.initialize(t.getBatchElements());
            for (BatchElement be : t.getBatchElements()) {
                Hibernate.initialize(be.getBatch().getElements());
            }
        }
        return c;
    }

    protected Batch initInternal(Batch b) {
        Hibernate.initialize(b.getElements());
        for (BatchElement be : b.getElements()) {
            Hibernate.initialize(be.getTask().getBatchElements());
            initInternal(be.getTask().getConfiguration());
        }
        if (b.getConfiguration() != null) {
            initInternal(b.getConfiguration());
        }
        return b;
    }

    protected BatchRun initInternal(BatchRun br) {
        for (Run run : br.getRuns()) {
            initInternal(run);
        }
        return br;
    }

    protected Run initInternal(Run run) {
        Hibernate.initialize(run.getBatchElement());
        return run;
    }
}
