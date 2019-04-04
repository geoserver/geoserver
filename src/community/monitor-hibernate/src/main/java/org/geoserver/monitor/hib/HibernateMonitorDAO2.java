/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geoserver.monitor.CompositeFilter;
import org.geoserver.monitor.Filter;
import org.geoserver.monitor.FilterVisitorSupport;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorConfig.Mode;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.PipeliningTaskQueue;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class HibernateMonitorDAO2 implements MonitorDAO, DisposableBean {

    public static enum Sync {
        SYNC,
        ASYNC,
        ASYNC_UPDATE;
    }

    HibernateTemplate hib;
    PipeliningTaskQueue<Thread> tasks;

    Mode mode = Mode.HISTORY;
    Sync sync = Sync.ASYNC;

    public HibernateMonitorDAO2() {
        setMode(Mode.HISTORY);
        setSync(Sync.ASYNC);
    }

    public String getName() {
        return "hibernate";
    }

    @Override
    public void init(MonitorConfig config) {
        setMode(config.getMode());
        setSync(getSync(config));
    }

    public Sync getSync(MonitorConfig config) {
        return Sync.valueOf(
                config.getProperties().getProperty("hibernate.sync", "async").toUpperCase());
    }

    public void setSync(Sync sync) {
        this.sync = sync;
        if (sync != Sync.SYNC) {
            if (tasks == null) {
                tasks = new PipeliningTaskQueue<Thread>();
                tasks.start();
            }
        } else {
            if (tasks != null) {
                dispose();
            }
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        hib = new HibernateTemplate(sessionFactory);
        hib.setFetchSize(1000);
    }

    public SessionFactory getSessionFactory() {
        return hib.getSessionFactory();
    }

    public RequestData init(final RequestData data) {
        if (mode != Mode.HISTORY) {
            if (sync == Sync.ASYNC_UPDATE) {
                // async_update means don't run the initial insert asynchronously
                new Insert(data).run();
            } else {
                run(new Insert(data));
            }
        } else {
            // don't persist yet, we persist at the very end of request
        }

        return data;
    }

    public void add(RequestData data) {
        if (sync == Sync.ASYNC_UPDATE) {
            // async_update means don't run the initial insert asynchronously
            new Insert(data).run();
        } else {
            run(new Insert(data));
        }
    }

    public void update(RequestData data) {
        save(data);
    }

    public void save(RequestData data) {
        run(new Save(data));
        //        if(data.getId() == -1) {
        //            run(new Insert(data));
        //        }
        //        else {
        //            run(new Update(data));
        //        }
    }

    public void clear() {}

    public void dispose() {
        if (tasks != null) {
            tasks.shutdown();
            tasks = null;
        }
    }

    public List<RequestData> getOwsRequests() {
        throw new UnsupportedOperationException();
    }

    public List<RequestData> getOwsRequests(String service, String operation, String version) {
        throw new UnsupportedOperationException();
    }

    public RequestData getRequest(long id) {
        return (RequestData) hib.get(RequestData.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<RequestData> getRequests() {
        return all(RequestData.class, "startTime");
    }

    @SuppressWarnings("unchecked")
    public List<RequestData> getRequests(Query q) {
        return query(q);
    }

    public void getRequests(Query q, RequestDataVisitor visitor) {
        query(q, visitor);
    }

    public long getCount(Query q) {
        q = q.clone();
        q.getAggregates().clear();
        q.getProperties().clear();
        q.getGroupBy().clear();
        q.setSortBy(null);
        q.setSortOrder(null);
        q.aggregate("count()");

        org.hibernate.Query query = toQuery(q);
        long count = ((Number) query.uniqueResult()).longValue();

        // factor in offset, count
        if (q.getOffset() != null) {
            count = Math.max(0, count - q.getOffset());
        }
        if (q.getCount() != null) {
            count = Math.min(count, q.getCount());
        }

        return count;
    }

    public Iterator<RequestData> getIterator(Query q) {
        org.hibernate.Query query = toQuery(q);
        return new RequestDataIterator(query.iterate(), q);
    }

    class RequestDataIterator implements Iterator<RequestData> {

        Iterator it;
        Query query;

        public RequestDataIterator(Iterator it, Query query) {
            this.it = it;
            this.query = query;
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public RequestData next() {
            return toRequest(it.next(), query);
        }

        public void remove() {
            // TODO Auto-generated method stub

        }
    }

    //    public ResourceData getLayer(String name) {
    //        return (ResourceData) hib.get(ResourceData.class, name);
    //    }
    //
    //    public List<ResourceData> getLayers() {
    //        return all(ResourceData.class, null);
    //    }
    //
    //    public List<ResourceData> getLayers(MonitorQuery q) {
    //        return query(q, ResourceData.class);
    //    }
    //
    //    public void getLayers(MonitorQuery q, MonitorVisitor<ResourceData> visitor) {
    //        query(q, visitor, ResourceData.class);
    //    }

    @SuppressWarnings("unchecked")
    protected List<RequestData> query(final Query q) {
        // TODO: handle the case of when the user specifies properties,
        return hib.execute(
                new HibernateCallback<List<RequestData>>() {
                    public List<RequestData> doInHibernate(Session session)
                            throws HibernateException, SQLException {
                        List objs = new ArrayList<>();
                        org.hibernate.Query query = toQuery(q, objs);

                        List list = query.list();
                        if (q.getProperties().isEmpty()) {
                            // selected whole object, just return the list directly
                            return list;
                        }

                        List<RequestData> results = new ArrayList<>(list.size());
                        Iterator it = list.iterator();
                        while (it.hasNext()) {
                            Object next = it.next();
                            results.add(toRequest(next, q));
                        }

                        return results;
                    }
                });
    }

    protected <T> void query(Query q, RequestDataVisitor visitor) {
        List<Object> objs = new ArrayList();
        org.hibernate.Query query = toQuery(q, objs);

        Iterator it = query.iterate();
        try {
            if (q.getProperties().isEmpty() && q.getAggregates().isEmpty()) {
                while (it.hasNext()) {
                    visitor.visit((RequestData) it.next());
                }
            } else {
                while (it.hasNext()) {
                    // TODO: avoid the intense object creation, and reflection... we probably
                    // just want to expose the values directly to the visitor
                    int nprops = q.getProperties().size() + q.getAggregates().size();
                    Object[] values = nprops == 1 ? new Object[] {it.next()} : (Object[]) it.next();

                    RequestData data = toRequest(values, q);

                    // aggregate properties
                    Object[] aggregates =
                            !q.getAggregates().isEmpty()
                                    ? new Object[q.getAggregates().size()]
                                    : null;
                    int off = q.getProperties().size();
                    for (int i = 0; i < q.getAggregates().size(); i++) {
                        aggregates[i] = values[off + i];
                    }

                    visitor.visit(data, aggregates);
                }
            }
        } finally {
            hib.closeIterator(it);
        }
    }

    protected RequestData toRequest(Object obj, Query q) {
        if (obj instanceof RequestData) {
            return (RequestData) obj;
        }
        Object[] values = obj instanceof Object[] ? (Object[]) obj : new Object[] {obj};
        RequestData data = null;
        try {
            data = RequestData.class.newInstance();
        } catch (Exception e) {
        }
        ;

        // regular properties
        for (int i = 0; i < q.getProperties().size(); i++) {
            String prop = q.getProperties().get(i);

            if (prop.equals("resource")) {
                // this means a joined query in which they selected an individual
                // accesses resource from the RequestData.resources collection
                data.getResources().add((String) values[i]);
            } else {
                OwsUtils.set(data, prop, values[i]);
            }
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> all(final Class<T> clazz, final String orderBy) {
        return hib.execute(
                new HibernateCallback<List<T>>() {
                    public List<T> doInHibernate(Session session)
                            throws HibernateException, SQLException {
                        StringBuffer sb = new StringBuffer();
                        sb.append("SELECT x ");
                        sb.append("FROM ").append(clazz.getSimpleName()).append(" x ");
                        if (orderBy != null) {
                            sb.append("ORDER BY x.").append(orderBy).append(" DESC");
                        }

                        org.hibernate.Query q = session.createQuery(sb.toString());
                        return q.list();
                    }
                });
    }

    protected org.hibernate.Query toQuery(Query q) {
        return toQuery(q, new ArrayList());
    }

    protected org.hibernate.Query toQuery(Query q, List<Object> objs) {
        String hql = toHQL(q, objs);

        org.hibernate.Query query = hib.getSessionFactory().getCurrentSession().createQuery(hql);
        if (q.getOffset() != null) {
            query.setFirstResult(q.getOffset().intValue());
        }

        if (q.getCount() != null) {
            query.setMaxResults(q.getCount().intValue());
        }

        for (int i = 0; i < objs.size(); i++) {
            query.setParameter(i, objs.get(i));
        }

        return query;
    }

    protected String toHQL(Query q, List<Object> objs) {
        String entity = RequestData.class.getSimpleName();
        String prefix = Character.toLowerCase(entity.charAt(0)) + "d";
        boolean join = false;

        StringBuffer sb = new StringBuffer("SELECT ");
        if (q.getProperties().isEmpty() && q.getAggregates().isEmpty()) {
            // select the whole object
            sb.append(prefix);
        } else {
            // only load the specified properties
            for (String prop : q.getProperties()) {
                prefix(prop, prefix, sb).append(", ");

                if (prop.equals("resource")) {
                    join = true;
                }
            }
            for (String agg : q.getAggregates()) {
                // handle aggregate functions
                if (agg.equals("count()")) {
                    agg = "count(" + prefix + ")";
                }
                sb.append(agg).append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

        sb.append(" FROM ").append(entity).append(" ").append(prefix);
        if (!join && q.getFilter() != null) {
            // we may need to join depending on what is found in the query
            JoinDeterminer jt = new JoinDeterminer();
            q.getFilter().accept(jt);
            join = jt.doJoin();
        }
        if (join) {
            sb.append(" LEFT JOIN ").append(prefix).append(".resources AS resource");
        }

        if (q.getFilter() != null) {
            sb.append(" WHERE ");
            q.getFilter().accept(new FilterEncoder(sb, prefix, objs));
        }

        if (q.getFromDate() != null || q.getToDate() != null) {
            if (q.getFilter() != null) {
                sb.append(" AND");
            } else {
                sb.append(" WHERE");
            }

            sb.append(" ").append(prefix).append(".startTime");
            if (q.getFromDate() != null && q.getToDate() != null) {
                sb.append(" BETWEEN ? AND ?");
                objs.add(q.getFromDate());
                objs.add(q.getToDate());
            } else if (q.getFromDate() != null) {
                sb.append(" >= ?");
                objs.add(q.getFromDate());
            } else {
                sb.append(" <= ?");
                objs.add(q.getToDate());
            }
        }

        if (!q.getGroupBy().isEmpty()) {
            sb.append(" GROUP BY ");
            for (String prop : q.getGroupBy()) {
                prefix(prop, prefix, sb).append(",");
            }
            sb.setLength(sb.length() - 1);
        }

        if (q.getSortBy() != null) {
            sb.append(" ORDER BY ");
            String sortBy = q.getSortBy();
            if (sortBy.equals("count()")) {
                sb.append("count(" + prefix + ")");
            } else {
                prefix(sortBy, prefix, sb);
            }
            sb.append(" ").append(q.getSortOrder());
        } else if (q.getFromDate() != null || q.getToDate() != null) {
            // only sort if this is not an aggregate query
            if (q.getAggregates().isEmpty()) {
                // by default sort dates descending
                sb.append(" ORDER BY ")
                        .append(prefix)
                        .append(".startTime")
                        .append(" ")
                        .append(SortOrder.DESC);
            }
        }

        return sb.toString();
    }

    StringBuffer prefix(String prop, String prefix, StringBuffer sb) {
        // TODO: hack, resource is a special property here... figure out something better
        if (!"resource".equals(prop)) {
            sb.append(prefix).append(".");
        }
        sb.append(prop);
        return sb;
    }

    static class JoinDeterminer extends FilterVisitorSupport {

        boolean join = false;

        @Override
        protected void handleFilter(Filter f) {
            if ("resource".equals(f.getLeft())) {
                join = true;
            }
        }

        public boolean doJoin() {
            return join;
        }
    }

    class FilterEncoder extends FilterVisitorSupport {

        StringBuffer sb;
        String prefix;
        List objs;

        FilterEncoder(StringBuffer sb, String prefix, List objs) {
            this.sb = sb;
            this.prefix = prefix;
            this.objs = objs;
        }

        protected void handleComposite(CompositeFilter f, String type) {
            sb.append("(");
            for (Filter g : f.getFilters()) {
                visit(g);
                sb.append(" ").append(type).append(" ");
            }
            sb.setLength(sb.length() - (type.length() + 2));
            sb.append(")");
        }

        protected void handleFilter(Filter f) {
            if (isProperty(f.getLeft())) {
                prefix((String) f.getLeft(), prefix, sb);
            } else {
                sb.append(" ?");
                objs.add(f.getLeft());
            }

            if (f.getRight() == null) {
                sb.append(" IS");
                if (f.getType() != Comparison.EQ) {
                    sb.append(" NOT");
                }
                sb.append(" NULL");
            } else {
                sb.append(" ").append(f.getType());
                if (f.getType() == Comparison.IN) {
                    if (isProperty(f.getRight())) {
                        sb.append(" elements(").append(f.getRight()).append(")");
                    } else {
                        sb.append(" (");
                        for (Object o : (List) f.getRight()) {
                            sb.append("?, ");
                            objs.add(o);
                        }
                        sb.setLength(sb.length() - 2);
                        sb.append(")");
                    }
                } else {
                    if (isProperty(f.getRight())) {
                        prefix((String) f.getRight(), prefix, sb);
                    } else {
                        sb.append(" ?");
                        objs.add(f.getRight());
                    }
                }
            }
        }

        boolean isProperty(Object obj) {
            if (obj instanceof String) {
                String s = (String) obj;
                return "resource".equals(s) || OwsUtils.has(new RequestData(), s);
            }
            return false;
        }
    }

    //    protected void mergeLayers(RequestData data, Session session) {
    //        for (int i = 0; i < data.getLayers().size(); i++) {
    //            ResourceData l = data.getLayers().get(i);
    //            l = (ResourceData) session.merge(l);
    //            data.getLayers().set(i, l);
    //        }
    //    }

    protected void run(Task task) {
        if (tasks != null) {
            tasks.execute(Thread.currentThread(), new Async(task), task.desc);
        } else {
            task.run();
        }
    }

    class Async implements Runnable {

        Task task;

        Async(Task task) {
            this.task = task;
        }

        public void run() {
            synchronized (task.data) {
                task.run();
            }
        }
    }

    abstract static class Task implements Runnable {

        RequestData data;
        String desc;

        Task(RequestData data) {
            this.data = data;
        }
    }

    class Save extends Task {
        RequestData data;

        Save(RequestData data) {
            super(data);
            this.data = data;
        }

        public void run() {
            if (data.getId() == -1) {
                new Insert(data).run();
            } else {
                new Update(data).run();
            }
        }
    }

    class Insert extends Task {

        Insert(RequestData data) {
            super(data);
            this.desc = "Insert " + data.internalid;
        }

        public void run() {
            hib.execute(
                    new HibernateCallback() {
                        public Object doInHibernate(Session session)
                                throws HibernateException, SQLException {
                            Transaction tx = session.beginTransaction();
                            data.setId((Long) session.save(data));
                            // mergeLayers(data, session);
                            session.save(data);
                            tx.commit();
                            return data;
                        }
                    });
        }
    }

    class Update extends Task {

        Update(RequestData data) {
            super(data);
            this.desc = "Update " + data.internalid;
        }

        public void run() {
            hib.execute(
                    new HibernateCallback() {
                        public Object doInHibernate(Session session)
                                throws HibernateException, SQLException {
                            try {
                                Transaction tx = session.beginTransaction();
                                // mergeLayers(data, session);
                                session.update(data);
                                tx.commit();
                            } catch (HibernateException e) {
                                if (tasks != null) tasks.print();
                                throw e;
                            }
                            return null;
                        }
                    });
        }
    }

    @Override
    public void destroy() throws Exception {
        getSessionFactory().close();
    }
}
