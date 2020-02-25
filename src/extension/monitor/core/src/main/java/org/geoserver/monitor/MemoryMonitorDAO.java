/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.ows.util.OwsUtils;

public class MemoryMonitorDAO implements MonitorDAO {

    public static final String NAME = "memory";

    Queue<RequestData> live = new ConcurrentLinkedQueue<RequestData>();
    Queue<RequestData> history = new ConcurrentLinkedQueue<RequestData>();

    AtomicLong REQUEST_ID_GEN = new AtomicLong(1);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(MonitorConfig config) {}

    public RequestData init(RequestData data) {
        data.setId(REQUEST_ID_GEN.getAndIncrement());
        return data;
    }

    public void add(RequestData data) {
        live.add(data);
    }

    public void update(RequestData data) {}

    public void save(RequestData data) {
        live.remove(data);
        history.add(data);

        if (history.size() > 100) {
            history.poll();
        }
    }

    public RequestData getRequest(long id) {
        for (RequestData r : getRequests()) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    public List<RequestData> getRequests() {
        List<RequestData> requests = new LinkedList();
        requests.addAll(live);
        requests.addAll(history);
        return requests;
    }

    public List<RequestData> getRequests(Query q) {
        List<RequestData> requests = getRequests();

        List<Predicate> predicates = new ArrayList();
        if (q.getFilter() != null) {
            Filter f = q.getFilter();
            predicates.add(new PropertyCompare(f.getLeft(), f.getType(), f.getRight()));
        }
        if (q.getFromDate() != null || q.getToDate() != null) {
            predicates.add(new DateRange(q.getFromDate(), q.getToDate()));
        }

        int i = 1, count = 0;

        O:
        for (Iterator<RequestData> it = requests.iterator(); it.hasNext(); ) {
            RequestData r = it.next();
            for (Predicate p : predicates) {
                if (!p.matches(r)) {
                    it.remove();
                    continue O;
                }
            }

            if (q.getOffset() != null && q.getOffset() >= i++) {
                it.remove();
                continue;
            }
            if (q.getCount() != null && q.getCount() <= count) {
                it.remove();
                continue;
            }
            count++;
        }

        if (q.getSortBy() != null) {
            Collections.sort(requests, new Sorter(q.getSortBy(), q.getSortOrder()));
        } else if (q.getFromDate() != null || q.getToDate() != null) {
            Collections.sort(requests, new Sorter("startTime", SortOrder.DESC));
        }
        return requests;
    }

    public void getRequests(Query query, RequestDataVisitor visitor) {
        for (RequestData r : getRequests(query)) {
            visitor.visit(r);
        }
    }

    public long getCount(Query query) {
        return getRequests(query).size();
    }

    public Iterator<RequestData> getIterator(Query query) {
        return getRequests(query).iterator();
    }

    public ResourceData getLayer(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<ResourceData> getLayers() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<ResourceData> getLayers(Query query) {
        // TODO Auto-generated method stub
        return null;
    }

    public void getLayers(Query query, MonitorVisitor<ResourceData> visitor) {
        // TODO Auto-generated method stub

    }

    public List<RequestData> getOwsRequests() {
        return null;
    }

    public java.util.List<RequestData> getOwsRequests(
            String service, String operation, String version) {
        return null;
    };

    public void clear() {
        live.clear();
        history.clear();
    }

    public void dispose() {
        live.clear();
        history.clear();
        REQUEST_ID_GEN = new AtomicLong(1);
    }

    static interface Predicate {
        boolean matches(RequestData data);
    }

    static class DateRange implements Predicate {

        Date from;
        Date to;

        DateRange(Date from, Date to) {
            this.from = from;
            this.to = to;
        }

        public boolean matches(RequestData data) {
            Date time = data.getStartTime();
            if (time == null) {
                return false;
            }

            if (from != null) {
                if (time.before(from)) {
                    return false;
                }
            }

            if (to != null) {
                if (time.after(to)) {
                    return false;
                }
            }

            return true;
        }
    }

    static class PropertyCompare implements Predicate {

        Object left, right;
        Comparison compare;

        public PropertyCompare(Object left, Comparison compare, Object right) {
            this.left = left;
            this.right = right;
            this.compare = compare;
        }

        public boolean matches(RequestData data) {
            String property = null;
            Object value = null;
            if (left instanceof String && OwsUtils.has(data, (String) left)) {
                property = (String) left;
                value = right;
            } else if (right instanceof String && OwsUtils.has(data, (String) right)) {
                property = (String) right;
                value = left;
            }
            if (property == null) {
                throw new IllegalArgumentException("Could not find property");
            }

            Object o = OwsUtils.get(data, property);
            if (o == null) {
                return value == null && compare == Comparison.EQ;
            }

            if (compare == Comparison.IN) {
                if (!(value instanceof List)) {
                    throw new UnsupportedOperationException(
                            "IN comparison only supported against list values");
                }

                return ((List) value).contains(o);
            }

            if (compare == Comparison.EQ) {
                return o.equals(value);
            }
            if (compare == Comparison.NEQ) {
                return !o.equals(value);
            }

            if (o instanceof Comparable) {
                int c = ((Comparable) o).compareTo(value);
                switch (compare) {
                    case LT:
                        return c < 0;
                    case LTE:
                        return c <= 0;
                    case GT:
                        return c > 0;
                    case GTE:
                        return c >= 0;
                }
                return false;
            } else {
                throw new UnsupportedOperationException(
                        "Values of type "
                                + value.getClass().getName()
                                + " only support equality and non-equality comparison.");
            }
        }
    }

    static class Sorter implements Comparator<RequestData> {

        String property;
        SortOrder order;

        Sorter(String property, SortOrder order) {
            this.property = property;
            this.order = order;
        }

        public int compare(RequestData r1, RequestData r2) {
            int c = compareInternal(r1, r2);
            return order == SortOrder.ASC ? c : -1 * c;
        }

        public int compareInternal(RequestData r1, RequestData r2) {
            Object o1 = OwsUtils.get(r1, property);
            Object o2 = OwsUtils.get(r2, property);

            if (o1 == null && o2 != null) {
                return 1;
            }
            if (o1 != null && o2 == null) {
                return -1;
            }
            if (o1 == null && o2 == null) {
                return 0;
            }

            if (o1 instanceof Comparable) {
                return ((Comparable) o1).compareTo(o2);
            }

            return o1.toString().compareTo(o2.toString());
        }
    }
}
