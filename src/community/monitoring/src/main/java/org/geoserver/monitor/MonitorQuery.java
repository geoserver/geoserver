package org.geoserver.monitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MonitorQuery {

    public enum SortOrder { ASC, DESC }
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
    
    //TODO: come up with a better way to store query predicates, perhaps 
    // use geotools filter interfaces?
    String filterProperty;
    Object filterValue;
    Comparison filterCompare;
    
    public MonitorQuery properties(String... props) {
        for (String p : props) {
            properties.add(p);
        }
        return this;
    }
    
    public MonitorQuery sort(String property, SortOrder order) {
        sortBy = property;
        sortOrder = order;
        return this;
    }
    
    public MonitorQuery filter(String property, Object value, Comparison compare) {
        filterProperty = property;
        filterValue = value;
        filterCompare = compare;
        return this;
    }
    
    public MonitorQuery between(Date from, Date to) {
        fromDate = from;
        toDate = to;
        return this;
    }
    
    public MonitorQuery page(Long offset, Long count) {
        this.offset = offset;
        this.count = count;
        return this;
    }
    
    public List<String> getProperties() {
        return properties;
    }
    
    public Date getFromDate() {
        return fromDate;
    }
    
    public Date getToDate() {
        return toDate;
    }
    
    public String getFilterProperty() {
        return filterProperty;
    }
    
    public Object getFilterValue() {
        return filterValue;
    }
    
    public Comparison getFilterCompare() {
        return filterCompare;
    }
    
    public Long getOffset() {
        return offset;
    }
    
    public Long getCount() {
        return count;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public SortOrder getSortOrder() {
        return sortOrder;
    }
    
}
