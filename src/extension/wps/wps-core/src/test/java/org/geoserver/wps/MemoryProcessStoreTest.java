package org.geoserver.wps;

import static org.junit.Assert.*;

import java.util.List;

import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geotools.data.Query;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

public class MemoryProcessStoreTest {

    private MemoryProcessStatusStore store;

    private ExecutionStatus s1;

    private ExecutionStatus s2;

    private ExecutionStatus s3;

    private ExecutionStatus s4;

    @Before
    public void setup() {
        // prepare a few statues
        store = new MemoryProcessStatusStore();
        
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

    private void fillStore() {
        store.save(s1);
        store.save(s2);
        store.save(s3);
        store.save(s4);
    }

    @Test
    public void testFilter() throws CQLException {

        checkFiltered(store, CQL.toFilter("processName = 'test1'"), s1);
        checkFiltered(store, CQL.toFilter("phase = 'RUNNING'"), s3, s4);
        checkFiltered(store, CQL.toFilter("progress > 30"), s3, s4);
    }

    private void checkFiltered(MemoryProcessStatusStore store, Filter filter,
            ExecutionStatus... statuses) {
        List<ExecutionStatus> filtered = store.list(new Query(null, filter));
        checkContains(filtered, statuses);
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
        MemoryProcessStatusStore store = new MemoryProcessStatusStore();
        ExecutionStatus status = new ExecutionStatus(new NameImpl("test"), "abcde", false);
        store.save(status);
        List<ExecutionStatus> statuses = store.list(Query.ALL);
        assertEquals(1, statuses.size());
        assertEquals(status, statuses.get(0));
        assertNotSame(status, statuses.get(0));
    }

}
