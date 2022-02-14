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
        return saveObject(run);
    }

    @Override
    public BatchRun save(final BatchRun br) {
        return saveObject(br);
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

        /* Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .createAlias("configuration", "configuration", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN)
                        .setFetchMode("elements", FetchMode.JOIN)
                        .add(Restrictions.eq("removeStamp", 0L))
                        .add(
                                Restrictions.or(
                                        Restrictions.isNull("configuration"),
                                        Restrictions.eq("configuration.removeStamp", 0L)));
        return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();*/
    }

    private void loadLatestBatchRuns() {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<BatchRunImpl> query = cb.createQuery(BatchRunImpl.class);
        Root<BatchRunImpl> root = query.from(BatchRunImpl.class);
        root.join("batch").join("configuration", JoinType.LEFT);
        query.select(root);

        Subquery<Number> subQuery = query.subquery(Number.class);
        Root<RunImpl> subRoot = subQuery.from(RunImpl.class);
        subRoot.join("batchRun").join("batch");
        subQuery.select(
                cb.max(subRoot.get("batchRun").get("id"))); // assuming sequential id generation
        subQuery.where(
                cb.equal(
                        subRoot.get("batchRun").get("batch").get("id"),
                        root.get("batch").get("id")));

        query.where(
                cb.equal(root.get("batch").get("removeStamp"), 0L),
                cb.or(
                        cb.isNull(root.get("batch").get("configuration")),
                        cb.and(
                                cb.equal(
                                        root.get("batch").get("configuration").get("removeStamp"),
                                        0L),
                                cb.equal(
                                        root.get("batch").get("configuration").get("validated"),
                                        true),
                                cb.not(cb.like(root.get("batch").get("name"), "@%")))),
                cb.equal(root.get("id"), subQuery));
        query.distinct(true);

        /*Criteria criteria =
                getSession()
                        .createCriteria(BatchRunImpl.class, "outerBr")
                        .createAlias("outerBr.batch", "outerBatch")
                        .createAlias(
                                "outerBatch.configuration",
                                "configuration",
                                org.hibernate.sql.JoinType.LEFT_OUTER_JOIN)
                        .add(Restrictions.eq("outerBatch.removeStamp", 0L));
        criteria.add(
                Restrictions.or(
                        Restrictions.isNull("outerBatch.configuration"),
                        Restrictions.and(
                                Restrictions.and(
                                        Restrictions.eq("configuration.removeStamp", 0L),
                                        Restrictions.eq("configuration.validated", true)),
                                Restrictions.not(Restrictions.like("outerBatch.name", "@%")))));
        criteria.add(
                Subqueries.propertyEq(
                        "outerBr.id",
                        DetachedCriteria.forClass(RunImpl.class)
                                .createAlias("batchRun", "innerBr")
                                .createAlias("innerBr.batch", "innerBatch")
                                .add(Restrictions.eqProperty("innerBatch.id", "outerBatch.id"))
                                .setProjection(Projections.max("innerBr.id"))));

        for (BatchRunImpl br :
                (List<BatchRunImpl>)
                        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list()) {*/

        for (BatchRunImpl br : getSession().createQuery(query).getResultList()) {
            ((BatchImpl) br.getBatch()).setLatestBatchRun(br);
        }
    }

    @Override
    public List<Batch> getViewableBatches() {
        loadLatestBatchRuns();

        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<BatchImpl> root = query.from(BatchImpl.class);
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

        /* return getSession()
        .createCriteria(BatchImpl.class)
        .createAlias("configuration", "configuration", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN)
        .add(Restrictions.eq("removeStamp", 0L))
        .add(
                Restrictions.or(
                        Restrictions.isNull("configuration"),
                        Restrictions.and(
                                Restrictions.and(
                                        Restrictions.eq("configuration.removeStamp", 0L),
                                        Restrictions.eq("configuration.validated", true)),
                                Restrictions.not(Restrictions.like("name", "@%")))))
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .list(); */
    }

    @Override
    public void loadLatestBatchRuns(Configuration config) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<BatchRunImpl> query = cb.createQuery(BatchRunImpl.class);
        Root<BatchRunImpl> root = query.from(BatchRunImpl.class);
        root.join("batch").join("configuration", JoinType.LEFT);
        query.select(root);

        Subquery<Number> subQuery = query.subquery(Number.class);
        Root<RunImpl> subRoot = subQuery.from(RunImpl.class);
        subRoot.join("batchRun").join("batch");
        subQuery.select(
                cb.max(subRoot.get("batchRun").get("id"))); // assuming sequential id generation
        subQuery.where(
                cb.equal(
                        subRoot.get("batchRun").get("batch").get("id"),
                        root.get("batch").get("id")));

        query.where(
                cb.equal(root.get("batch").get("configuration").get("id"), config.getId()),
                cb.equal(root.get("id"), subQuery));
        query.distinct(true);

        /*Criteria criteria =
                getSession()
                        .createCriteria(BatchRunImpl.class, "outerBr")
                        .createAlias("outerBr.batch", "outerBatch")
                        .createAlias(
                                "outerBatch.configuration",
                                "configuration",
                                org.hibernate.sql.JoinType.LEFT_OUTER_JOIN)
                        .add(Restrictions.eq("outerBatch.removeStamp", 0L));
        criteria.add(Restrictions.eq("configuration.id", config.getId()));
        criteria.add(
                Subqueries.propertyEq(
                        "outerBr.id",
                        DetachedCriteria.forClass(RunImpl.class)
                                .createAlias("batchRun", "innerBr")
                                .createAlias("innerBr.batch", "innerBatch")
                                .add(Restrictions.eqProperty("innerBatch.id", "outerBatch.id"))
                                .setProjection(Projections.max("innerBr.id"))));

        for (BatchRunImpl br :
                (List<BatchRunImpl>)
                        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list()) {*/

        for (BatchRunImpl br : getSession().createQuery(query).getResultList()) {
            BatchImpl b = ((BatchImpl) config.getBatches().get(br.getBatch().getName()));
            if (b != null) {
                b.setLatestBatchRun(br);
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

        /*Criteria criteria =
                getSession()
                        .createCriteria(ConfigurationImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));
        if (templates != null) {
            criteria.add(Restrictions.eq("template", templates));
        }
        return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();*/
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

        /*return (Configuration)
        getSession()
                .createCriteria(ConfigurationImpl.class)
                .add(Restrictions.idEq(id))
                .uniqueResult();*/
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

        /* return (Batch)
        getSession()
                .createCriteria(BatchImpl.class)
                .add(Restrictions.idEq(id))
                .uniqueResult(); */
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

        /*return (Configuration)
        getSession()
                .createCriteria(ConfigurationImpl.class)
                .add(Restrictions.eq("removeStamp", 0L))
                .add(Restrictions.eq("name", name))
                .uniqueResult();*/
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

        /*DetachedCriteria alreadyInBatch =
                DetachedCriteria.forClass(BatchElementImpl.class)
                        .createAlias("batch", "batch")
                        .createAlias("task", "task")
                        .add(Restrictions.eq("batch.id", batch.getId()))
                        .add(Restrictions.eq("removeStamp", 0L))
                        .setProjection(Projections.property("task.id"));
        Criteria criteria =
                getSession()
                        .createCriteria(TaskImpl.class)
                        .createAlias("configuration", "configuration")
                        .createAlias("batchElements", "batchElements", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN)
                        .add(Restrictions.eq("removeStamp", 0L))
                        .add(Restrictions.eq("configuration.removeStamp", 0L))
                        .add(Subqueries.propertyNotIn("id", alreadyInBatch));

        if (batch.getConfiguration() == null) {
            criteria.add(Restrictions.eq("configuration.template", false));
        } else {
            criteria.add(Restrictions.eq("configuration.id", batch.getConfiguration().getId()));
        }

        return (List<Task>) criteria.list();*/
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

        /* Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));
        if (splitName.length > 1) {
            criteria.createAlias("configuration", "configuration")
                    .add(Restrictions.eq("configuration.name", splitName[0]))
                    .add(Restrictions.eq("name", splitName[1]))
                    .add(Restrictions.eq("configuration.removeStamp", 0L));
        } else {
            criteria.add(Restrictions.isNull("configuration"))
                    .add(Restrictions.eq("name", splitName[0]));
        }

        return (Batch) criteria.uniqueResult();*/
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

        /*Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));

        if (configNamePattern != null) {
            criteria.createAlias("configuration", "configuration")
                    .add(Restrictions.like("configuration.name", configNamePattern))
                    .add(Restrictions.like("name", namePattern))
                    .add(Restrictions.eq("configuration.removeStamp", 0L))
                    .add(Restrictions.eq("configuration.template", false))
                    .add(Restrictions.eq("configuration.validated", true))
                    .add(Restrictions.not(Restrictions.like("name", "@%")));
            if (workspacePattern == null) {
                criteria.add(Restrictions.isNull("configuration.workspace"));
            } else if (!"%".equals(workspacePattern)) {
                criteria.add(Restrictions.like("configuration.workspace", workspacePattern));
            }
        } else {
            criteria.add(Restrictions.isNull("configuration"))
                    .add(Restrictions.like("name", namePattern));
            if (workspacePattern == null) {
                criteria.add(Restrictions.isNull("workspace"));
            } else if (!"%".equals(workspacePattern)) {
                criteria.add(Restrictions.like("workspace", workspacePattern));
            }
        }

        return (List<Batch>) criteria.list();*/
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

        /*
        Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));

        criteria.createAlias("configuration", "configuration")
                .add(Restrictions.like("configuration.name", configNamePattern))
                .add(Restrictions.like("name", InitConfigUtil.INIT_BATCH))
                .add(Restrictions.eq("configuration.removeStamp", 0L))
                .add(Restrictions.eq("configuration.template", false))
                .add(Restrictions.eq("configuration.validated", false));
        if (workspacePattern == null) {
            criteria.add(Restrictions.isNull("configuration.workspace"));
        } else if (!"%".equals(workspacePattern)) {
            criteria.add(Restrictions.like("configuration.workspace", workspacePattern));
        }

        return (List<Batch>) criteria.list();*/
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

        /*return (BatchElement)
        getSession()
                .createCriteria(BatchElementImpl.class)
                .createAlias("batch", "batch")
                .createAlias("task", "task")
                .add(Restrictions.eq("batch.id", batch.getId()))
                .add(Restrictions.eq("task.id", task.getId()))
                .uniqueResult(); */
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

        /* return (Run)
        (getSession()
                        .createCriteria(RunImpl.class)
                        .setLockMode(LockMode.PESSIMISTIC_READ)
                        .createAlias("batchElement", "batchElement")
                        .createAlias("batchElement.task", "task")
                        .add(Restrictions.eq("task.id", task.getId()))
                        .add(Restrictions.isNull("end")))
                .uniqueResult(); */
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

        /*return (List<BatchRun>)
        (getSession()
                .createCriteria(RunImpl.class)
                .createAlias("batchRun", "batchRun")
                .createAlias("batchRun.batch", "batch")
                .add(Restrictions.eq("batch.id", batch.getId()))
                .add(
                        Restrictions.in(
                                "status",
                                (Object[])
                                        new Run.Status[] {
                                            Run.Status.RUNNING,
                                            Run.Status.READY_TO_COMMIT,
                                            Run.Status.COMMITTING
                                        }))
                .setProjection(Projections.groupProperty("batchRun"))
                .list());*/
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

        /* return (List<BatchRun>)
        (getSession()
                .createCriteria(RunImpl.class)
                .createAlias("batchRun", "batchRun")
                .createAlias("batchRun.batch", "batch")
                .add(
                        Restrictions.in(
                                "status",
                                (Object[])
                                        new Run.Status[] {
                                            Run.Status.RUNNING,
                                            Run.Status.READY_TO_COMMIT,
                                            Run.Status.COMMITTING
                                        }))
                .setProjection(Projections.groupProperty("batchRun"))
                .list()); */
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

        /*return (BatchRun)
        (getSession()
                .createCriteria(BatchRunImpl.class)
                .add(Restrictions.eq("schedulerReference", schedulerReference))
                .addOrder(Order.desc("id")) // assuming sequential id generation
                .setMaxResults(1)
                .uniqueResult());*/
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

        /* return (Run)
        (getSession()
                        .createCriteria(RunImpl.class)
                        .setLockMode(LockMode.PESSIMISTIC_READ)
                        .createAlias("batchElement", "batchElement")
                        .createAlias("batchElement.task", "task")
                        .add(Restrictions.eq("task.id", task.getId()))
                        .add(Restrictions.isNotNull("end"))
                        .add(Restrictions.eq("status", Run.Status.COMMITTING)))
                .uniqueResult(); */
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

        /* return (Run)
        (getSession()
                        .createCriteria(RunImpl.class)
                        .createAlias("batchElement", "batchElement")
                        .add(Restrictions.eq("batchElement.id", batchElement.getId()))
                        .addOrder(Order.desc("start")))
                .setMaxResults(1)
                .uniqueResult(); */
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
}
