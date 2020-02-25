/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Base class for status store tests
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractProcessStoreTest {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    protected ProcessStatusStore store;

    protected ExecutionStatus s1;

    protected ExecutionStatus s2;

    protected ExecutionStatus s3;

    protected ExecutionStatus s4;

    @Before
    public void setup() throws IOException {
        // prepare a few statues
        this.store = buildStore();

        s1 = new ExecutionStatus(new NameImpl("test1"), "abcde1", false);
        s2 = new ExecutionStatus(new NameImpl("test2"), "abcde2", false);
        s2.setException(new Exception());
        s3 = new ExecutionStatus(new NameImpl("test3"), "abcde3", false);
        s3.setPhase(ProcessState.RUNNING);
        s3.setProgress(50f);
        s3.setTask("Having fun");
        s4 = new ExecutionStatus(new NameImpl("test3"), "abcde4", false);
        s4.setPhase(ProcessState.RUNNING);
        s4.setProgress(70f);
        s4.setTask("Fun is almost over");

        fillStore();
    }

    /** Builds the status store for this test */
    protected abstract ProcessStatusStore buildStore() throws IOException;

    /** Puts all the test statuses in the store */
    protected void fillStore() {
        store.save(s1);
        store.save(s2);
        store.save(s3);
        store.save(s4);
    }

    @Test
    public void testFilter() throws CQLException {
        // simple filters
        checkFiltered(store, query("processName = 'test1'"), s1);
        checkFiltered(store, query("phase = 'RUNNING'"), s3, s4);
        checkFiltered(store, query("progress > 30"), s3, s4);
        // force a post filter
        checkFiltered(store, query("strToLowerCase(phase) = 'running'"), s3, s4);
        checkFiltered(store, query("strToLowerCase(phase) = 'running' AND progress > 30"), s3, s4);
    }

    @Test
    public void testPaging() throws CQLException {
        // simple filters with some paging, sometimes odd
        checkFiltered(store, query("processName = 'test1'", 0, 1), s1);
        checkFiltered(store, query("processName = 'test1'", 1, 1));
        checkFiltered(store, query("phase = 'RUNNING'", 0, 1, asc("progress")), s3);
        checkFiltered(store, query("phase = 'RUNNING'", 1, 1, asc("progress")), s4);
        checkFiltered(store, query("phase = 'RUNNING'", 0, 1, desc("progress")), s4);
        checkFiltered(store, query("phase = 'RUNNING'", 1, 1, desc("progress")), s3);

        // force a post filter
        String lowercaseRunning = "strToLowerCase(phase) = 'running'";
        checkFiltered(store, query(lowercaseRunning), s3, s4);
        checkFiltered(store, query(lowercaseRunning, 0, 1, asc("progress")), s3);
        checkFiltered(store, query(lowercaseRunning, 1, 1, asc("progress")), s4);
        checkFiltered(store, query(lowercaseRunning, 0, 1, desc("progress")), s4);
        checkFiltered(store, query(lowercaseRunning, 1, 1, desc("progress")), s3);

        // force a mix of pre and post filter
        String lowercaseRunningProgress = "strToLowerCase(phase) = 'running' AND progress > 30";
        checkFiltered(store, query(lowercaseRunningProgress), s3, s4);
        checkFiltered(store, query(lowercaseRunningProgress), s3, s4);
        checkFiltered(store, query(lowercaseRunningProgress, 0, 1, asc("progress")), s3);
        checkFiltered(store, query(lowercaseRunningProgress, 1, 1, asc("progress")), s4);
        checkFiltered(store, query(lowercaseRunningProgress, 0, 1, desc("progress")), s4);
        checkFiltered(store, query(lowercaseRunningProgress, 1, 1, desc("progress")), s3);
    }

    private SortBy asc(String propertyName) {
        return FF.sort(propertyName, SortOrder.ASCENDING);
    }

    private SortBy desc(String propertyName) {
        return FF.sort(propertyName, SortOrder.DESCENDING);
    }

    protected void checkFiltered(
            ProcessStatusStore store, Query query, ExecutionStatus... statuses) {
        List<ExecutionStatus> filtered = store.list(query);
        checkContains(filtered, statuses);
    }

    private Query query(String cql) throws CQLException {
        return query(cql, 0, Integer.MAX_VALUE);
    }

    private Query query(String cql, int startIndex, int maxFeatures, SortBy... sortBy)
            throws CQLException {
        Filter filter = ECQL.toFilter(cql);
        Query query = new Query(null, filter);
        query.setStartIndex(startIndex);
        query.setMaxFeatures(maxFeatures);
        query.setSortBy(sortBy);
        return query;
    }

    private void checkContains(List<ExecutionStatus> filtered, ExecutionStatus... statuses) {
        assertEquals(statuses.length, filtered.size());
        for (ExecutionStatus status : statuses) {
            assertTrue(filtered.contains(status));
        }
    }

    @Test
    public void testDelete() throws CQLException {
        assertEquals(1, store.remove(CQL.toFilter("processName = 'test1'")));
        checkContains(store.list(Query.ALL), s2, s3, s4);
        assertEquals(2, store.remove(CQL.toFilter("progress > 30")));
        checkContains(store.list(Query.ALL), s2);
        assertEquals(1, store.remove(CQL.toFilter("phase = 'FAILED'")));
        checkContains(store.list(Query.ALL));
    }

    @Test
    public void testIsolated() {
        store.remove(Filter.INCLUDE);
        ExecutionStatus status = new ExecutionStatus(new NameImpl("test"), "abcde", false);
        store.save(status);
        List<ExecutionStatus> statuses = store.list(Query.ALL);
        assertEquals(1, statuses.size());
        assertEquals("incorrect status", status, statuses.get(0));
        assertNotSame(status, statuses.get(0));
    }
}
