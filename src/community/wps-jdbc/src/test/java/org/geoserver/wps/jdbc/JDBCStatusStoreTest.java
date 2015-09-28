package org.geoserver.wps.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.geoserver.wps.AbstractProcessStoreTest;
import org.geoserver.wps.ProcessStatusStore;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataInputsType1;
import net.opengis.wps10.DataType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.Wps10Factory;

/**
 * Tests the JDBC based process status store with a single instance
 * 
 * @author Ian Turton
 */
public class JDBCStatusStoreTest extends AbstractProcessStoreTest {

    private static final String fixtureID = "wpstest";

    private DataStore datastore;

    JDBCStatusStore statusStore;
    @Before
    public void checkOnLine() {
        Assume.assumeNotNull(GSFixtureUtilitiesDelegate.loadFixture(fixtureID));
    }
    @Override
    protected ProcessStatusStore buildStore() {
        
        Properties props = GSFixtureUtilitiesDelegate.loadFixture(fixtureID);
        
        Map<Object, Object> params = props;
        // work round PostFilter issue?
        params.put(PostgisNGDataStoreFactory.ENCODE_FUNCTIONS.key, true);
        try {
            datastore = DataStoreFinder.getDataStore(params);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        statusStore = new JDBCStatusStore(datastore);
        if(statusStore == null) {
            throw new RuntimeException("failed to create StatusStore with \n"+props);
        }
        return statusStore;
    }

    
    @After
    public void shutdown() {
        // clean up the DB
        if(statusStore!=null)
            statusStore.remove(Filter.INCLUDE);
        if(datastore != null)
            datastore.dispose();
    }

    @Test
    public void testStackTrace() {
        ExecutionStatus s = new ExecutionStatus(new NameImpl("tracetest"), "ian", false);
        s.setException(new IllegalArgumentException("a test exception"));
        store.save(s);
        ExecutionStatus status = store.get(s.getExecutionId());
        assertEquals(s, status);
        assertEquals(s.getException().getMessage(), status.getException().getMessage());

        StackTraceElement[] expStackTrace = s.getException().getStackTrace();
        StackTraceElement[] obsStackTrace = status.getException().getStackTrace();
        assertEquals(expStackTrace.length, obsStackTrace.length);
        for (int i = 0; i < obsStackTrace.length; i++) {
            assertEquals(expStackTrace[i], obsStackTrace[i]);
        }
        store.remove(s.getExecutionId());
    }
    
    @Test
    public void testRequest() {
        Wps10Factory f = Wps10Factory.eINSTANCE;
        ExecuteType ex = f.createExecuteType();

        CodeType id = Ows11Factory.eINSTANCE.createCodeType();
        ex.setIdentifier(id);
        id.setValue("foo");

        DataInputsType1 inputs = f.createDataInputsType1();
        ex.setDataInputs(inputs);

        InputType in = f.createInputType();
        inputs.getInput().add(in);

        DataType data = f.createDataType();
        in.setData(data);

        ComplexDataType cd = f.createComplexDataType();
        data.setComplexData(cd);
        ExecutionStatus s = new ExecutionStatus(new NameImpl("requesttest"), "ian", false);
        s.setRequest(ex);
        store.save(s);
        ExecutionStatus status = store.get(s.getExecutionId());
        assertEquals(s, status);
        ExecuteType obs = status.getRequest();
        ExecuteType expected = s.getRequest();
        assertEquals(expected.getBaseUrl(), obs.getBaseUrl());
        assertEquals(expected.getIdentifier().getValue(), obs.getIdentifier().getValue());
    }
}
