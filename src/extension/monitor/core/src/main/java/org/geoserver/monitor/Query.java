/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Query implements Serializable {

    public enum SortOrder {
        ASC,
        DESC
    }

    public enum Comparison {
        EQ {
            @Override
            public String toString() {
                return "=";
            }
        },

        NEQ {
            @Override
            public String toString() {
                return "!=";
            }
        },

        LT {
            @Override
            public String toString() {
                return "<";
            }
        },

        LTE {
            @Override
            public String toString() {
                return "<=";
            }
        },

        GT {
            @Override
            public String toString() {
                return ">";
            }
        },

        GTE {
            @Override
            public String toString() {
                return ">=";
            }
        },
        IN {
            @Override
            public String toString() {
                return "IN";
            }
        }
    }

    List<String> properties = new ArrayList();

    String sortBy;
    SortOrder sortOrder;

    Date fromDate;
    Date toDate;

    Long offset;
    Long count;

    Filter filter;

    List<String> aggregates = new ArrayList();
    List<String> groupBy = new ArrayList();

    public Query properties(String... props) {
        for (String p : props) {
            properties.add(p);
        }
        return this;
    }

    public Query sort(String property, SortOrder order) {
        sortBy = property;
        sortOrder = order;
        return this;
    }

    public Query filter(Object left, Object right, Comparison type) {
        return filter(new Filter(left, right, type));
    }

    public Query filter(Filter filter) {
        return and(filter);
    }

    public Query and(Object left, Object right, Comparison type) {
        return and(new Filter(left, right, type));
    }

    public Query and(Filter f) {
        if (filter == null) {
            filter = f;
        } else if (filter instanceof And) {
            ((And) filter).getFilters().add(f);
        } else {
            filter = new And(filter, f);
        }
        return this;
    }

    public Query or(Object left, Object right, Comparison type) {
        return or(new Filter(left, right, type));
    }

    public Query or(Filter f) {
        if (filter == null) {
            filter = f;
        } else if (filter instanceof Or) {
            ((Or) filter).getFilters().add(f);
        } else {
            filter = new Or(filter, f);
        }
        return this;
    }

    public Query between(Date from, Date to) {
        fromDate = from;
        toDate = to;
        return this;
    }

    public Query page(Long offset, Long count) {
        this.offset = offset;
        this.count = count;
        return this;
    }

    public Query aggregate(String... aggregates) {
        this.aggregates.addAll(Arrays.asList(aggregates));
        return this;
    }

    public Query group(String... properties) {
        this.groupBy.addAll(Arrays.asList(properties));
        return this;
    }

    public List<String> getProperties() {
        return properties;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<String> getAggregates() {
        return aggregates;
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    @Override
    public Query clone() {
        Query clone = new Query();
        clone.properties.addAll(properties);
        clone.aggregates.addAll(aggregates);
        clone.groupBy.addAll(groupBy);
        clone.count = count;
        clone.offset = offset;
        clone.fromDate = fromDate;
        clone.toDate = toDate;
        clone.sortBy = sortBy;
        clone.sortOrder = sortOrder;
        clone.filter = filter;
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    SELECT: ").append(properties).append("\n");
        sb.append(" AGGREGATE: ").append(aggregates).append("\n");
        sb.append("    FILTER: ").append(filter).append("\n");
        sb.append("   BETWEEN: ").append(fromDate).append(", ").append(toDate).append("\n");
        sb.append("    OFFSET: ").append(offset).append(" LIMIT:").append(count).append("\n");
        sb.append("   SORT BY: ").append(sortBy).append(", ").append(sortOrder).append("\n");
        sb.append("  GROUP BY: ").append(groupBy);
        return sb.toString();
    }
}
