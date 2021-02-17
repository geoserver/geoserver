/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataInputsType1;
import net.opengis.wps10.DataType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.Wps10Factory;
import org.geoserver.wps.AbstractProcessStoreTest;
import org.geoserver.wps.ProcessStatusStore;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * Tests the JDBC based process status store with a single instance
 *
 * @author Ian Turton
 */
public abstract class AbstractJDBCStatusStoreTest extends AbstractProcessStoreTest {

    private DataStore datastore;

    JDBCStatusStore statusStore;

    String fixtureId;

    abstract String getFixtureId();

    protected Properties getFixture() {
        Properties properties = GSFixtureUtilitiesDelegate.loadFixture(getFixtureId());
        Assume.assumeNotNull(properties);
        return properties;
    }

    @After
    public void cleanup() {
        if (datastore != null) {
            datastore.dispose();
        }
    }

    @Override
    protected ProcessStatusStore buildStore() throws IOException {
        setupDataStore();
        if (Arrays.asList(datastore.getTypeNames()).contains(getStatusTable())) {
            datastore.removeSchema(getStatusTable());
        }
        statusStore = new JDBCStatusStore(datastore);
        return statusStore;
    }

    protected String getStatusTable() {
        return JDBCStatusStore.STATUS;
    }

    protected void setupDataStore() {
        Properties props = getFixture();

        try {
            datastore = DataStoreFinder.getDataStore(DataUtilities.toConnectionParameters(props));
        } catch (IOException e) {
        }

        if (datastore == null) {
            throw new RuntimeException("failed to create dataStore with \n " + props);
        }
    }

    @After
    public void shutdown() {
        // clean up the DB
        if (statusStore != null) statusStore.remove(Filter.INCLUDE);
        if (datastore != null) datastore.dispose();
    }

    @Test
    public void testStackTrace() {
        ExecutionStatus s = new ExecutionStatus(new NameImpl("tracetest"), "ian", false);
        IllegalArgumentException exception = new IllegalArgumentException("a test exception");
        exception.fillInStackTrace();
        s.setException(exception);
        store.save(s);
        ExecutionStatus status = store.get(s.getExecutionId());
        assertEquals(s, status);
        assertEquals(s.getException().getMessage(), status.getException().getMessage());

        StackTraceElement[] expStackTrace = s.getException().getStackTrace();
        StackTraceElement[] obsStackTrace = status.getException().getStackTrace();
        assertEquals(expStackTrace.length, obsStackTrace.length);
        // under latest Java 11 the two traces are not identical, relaxed testing
        assertEquals(expStackTrace[0].toString(), obsStackTrace[0].toString());
        store.remove(s.getExecutionId());
    }

    @Test
    @SuppressWarnings("unchecked") // EMF models without generics
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
