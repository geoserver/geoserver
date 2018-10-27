/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.Wps10Factory;
import org.geoserver.wps.ProcessStatusStore;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.transform.Definition;
import org.geotools.data.transform.TransformFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.wps.WPS;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.xml.sax.SAXException;

/**
 * A class that stores WPS session statuses in JDBC datastores.
 *
 * @author Ian Turton
 */
public class JDBCStatusStore implements ProcessStatusStore {

    private static final String STACKTRACESEPERATOR = "/";

    static final Logger LOGGER = Logging.getLogger(JDBCStatusStore.class);

    static final Wps10Factory WPSFACTORY = Wps10Factory.eINSTANCE;

    static final Ows11Factory OWSFACTORY = Ows11Factory.eINSTANCE;

    static final String STACK_TRACE = "stackTrace";

    static final String EXCEPTION_MESSAGE = "exceptionMessage";

    static final String EXCEPTION_CLASS = "exceptionClass";

    static final String ASYNC = "async";

    static final String USER_NAME = "userName";

    static final String TASK = "task";

    static final String SIMPLE_PROCESS_NAME = "processName";

    static final String PROPERTIES = "properties";

    static final String PROGRESS = "progress";

    static final String PROCESS_NAME_URI = "processNameURI";

    static final String PROCESS_NAME = "processNameImpl";

    static final String PHASE = "phase";

    static final String NODE_ID = "node";

    static final String COMPLETION = "completionTime";

    static final String LASTUPDATE = "lastUpdated";

    static final String CREATION = "creationTime";

    static final String PROCESS_ID = "processId";

    static final String STATUS = "status";

    static final String EXECUTION_ID = "exceptionId";

    private static final String REQUEST = "request";

    DataStore statuses;

    SimpleFeatureType schema;

    String actualStatusName;

    List<Definition> mappingDefinitions;

    public JDBCStatusStore(JDBCStatusStoreLoader loader) {
        this(loader.getStore());
    }

    public JDBCStatusStore(DataStore store) {
        if (store == null) {
            throw new RuntimeException(
                    "Attempted to create a JDBCStatusStore with a null datastore");
        }
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add(PROCESS_ID, String.class);
        tb.add(CREATION, Timestamp.class);
        tb.add(LASTUPDATE, Timestamp.class);
        tb.add(COMPLETION, Timestamp.class);
        tb.add(NODE_ID, String.class);
        tb.add(PHASE, String.class);
        tb.add(PROCESS_NAME, String.class);
        tb.add(PROCESS_NAME_URI, String.class);
        tb.add(PROGRESS, Float.class);
        tb.add(REQUEST, byte[].class);
        tb.add(PROPERTIES, String.class);
        tb.add(SIMPLE_PROCESS_NAME, String.class);
        tb.add(TASK, String.class);
        tb.add(USER_NAME, String.class);
        tb.add(ASYNC, String.class);
        tb.add(EXCEPTION_CLASS, String.class);
        tb.add(EXCEPTION_MESSAGE, String.class);
        tb.add(STACK_TRACE, byte[].class);
        tb.setName(STATUS);
        schema = tb.buildFeatureType();

        statuses = store;
        try {
            SimpleFeatureType storeSchema = lookupStatusSchema();

            if (storeSchema == null) {
                LOGGER.fine("creating new DB table for statuses");
                statuses.createSchema(schema);
                storeSchema = lookupStatusSchema();
            }

            // do we need any mapping?
            actualStatusName = storeSchema.getTypeName();
            mappingDefinitions = buildDefinitions(storeSchema, schema);
        } catch (IOException e) {
            throw new WPSException("Failed to setup the underlying store", e);
        }
    }

    private List<Definition> buildDefinitions(
            SimpleFeatureType actual, SimpleFeatureType expected) {
        List<Definition> definitions = new ArrayList<>();
        boolean mappingRequired = false;
        if (!actual.getTypeName().equals(expected.getTypeName())) {
            mappingRequired = true;
        }

        List<AttributeDescriptor> expectedDescriptors = expected.getAttributeDescriptors();
        List<AttributeDescriptor> actualDescriptors = actual.getAttributeDescriptors();
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        for (int i = 0; i < expected.getAttributeCount(); i++) {
            AttributeDescriptor expectedDescriptor = expectedDescriptors.get(i);
            AttributeDescriptor actualDescriptor = actualDescriptors.get(i);
            String expectedName = expectedDescriptor.getLocalName();
            String actualName = actualDescriptor.getLocalName();
            if (!expectedName.equals(actualName)) {
                mappingRequired = true;
            }
            Class<?> expectedType = expectedDescriptor.getType().getBinding();
            if (!expectedType.isAssignableFrom(actualDescriptor.getType().getBinding())) {
                mappingRequired = true;
            }
            definitions.add(new Definition(expectedName, ff.property(actualName), expectedType));
        }

        if (mappingRequired) {
            return definitions;
        } else {
            return null;
        }
    }

    private SimpleFeatureType lookupStatusSchema() throws IOException {
        String[] typeNames = statuses.getTypeNames();
        for (String typeName : typeNames) {
            if (typeName.equalsIgnoreCase(STATUS)) {
                return statuses.getSchema(typeName);
            }
        }
        return null;
    }

    private SimpleFeatureStore getStatusFeatureStore() throws IOException {
        SimpleFeatureSource source = statuses.getFeatureSource(actualStatusName);
        if (mappingDefinitions != null) {
            source = TransformFactory.transform(source, new NameImpl(STATUS), mappingDefinitions);
        }

        return (SimpleFeatureStore) source;
    }

    @Override
    public void save(ExecutionStatus status) {
        DefaultTransaction transaction = new DefaultTransaction("create");
        boolean committed = false;
        try {
            SimpleFeatureStore store = getStatusFeatureStore();
            store.setTransaction(transaction);
            SimpleFeature feature = statusToFeature(status);
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection =
                    DataUtilities.collection(feature);
            // if the feature exists delete it
            Filter filter = ECQL.toFilter(PROCESS_ID + " = '" + status.getExecutionId() + "'");
            store.removeFeatures(filter);
            store.addFeatures(featureCollection);
            transaction.commit();
            committed = true;
        } catch (Exception e) {
            throw new WPSException("Failure saving status " + status, e);
        } finally {
            closeTransaction(transaction, committed);
        }
    }

    @Override
    public ExecutionStatus get(String executionId) {
        LOGGER.fine("getting status " + executionId);
        try {
            SimpleFeatureSource source = getStatusFeatureStore();

            Filter filter = ECQL.toFilter(PROCESS_ID + " = '" + executionId + "'");
            SimpleFeatureCollection features = source.getFeatures(filter);
            SimpleFeature f = DataUtilities.first(features);
            ExecutionStatus stat = featureToStatus(f);
            return stat;
        } catch (IOException | CQLException e) {
            throw new WPSException("Failed to get execution status " + executionId, e);
        }
    }

    @Override
    public ExecutionStatus remove(String executionId) {
        LOGGER.fine("removing status " + executionId);
        DefaultTransaction transaction = new DefaultTransaction("create");
        boolean committed = false;
        try {
            SimpleFeatureStore store = getStatusFeatureStore();

            Filter filter = ECQL.toFilter(PROCESS_ID + " = '" + executionId + "'");
            store.setTransaction(transaction);
            SimpleFeatureCollection features = store.getFeatures(filter);
            SimpleFeature f = DataUtilities.first(features);
            ExecutionStatus stat = featureToStatus(f);
            store.removeFeatures(filter);
            transaction.commit();
            committed = true;
            return stat;
        } catch (Exception e) {
            throw new WPSException("Failure to remove status by id: " + executionId, e);
        } finally {
            closeTransaction(transaction, committed);
        }
    }

    private void closeTransaction(DefaultTransaction transaction, boolean committed) {
        if (!committed) {
            try {
                transaction.rollback();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failure to roll back transaction", e);
            }
        }
        transaction.close();
    }

    @Override
    public int remove(Filter filter) {
        LOGGER.fine("removing statuses matching " + filter);
        int ret = 0;
        DefaultTransaction transaction = new DefaultTransaction("create");
        boolean committed = false;
        try {
            SimpleFeatureStore store = getStatusFeatureStore();
            SimpleFeatureCollection features = store.getFeatures(filter);
            ret = features.size();
            if (ret == 0) {
                return ret;
            }
            store.setTransaction(transaction);
            store.removeFeatures(filter);
            transaction.commit();
            committed = true;
        } catch (Exception e) {
            throw new WPSException("Failure to remove status by filter: " + filter, e);
        } finally {
            closeTransaction(transaction, committed);
        }

        return ret;
    }

    @Override
    public List<ExecutionStatus> list(Query query) {
        LOGGER.fine("listing statuses matching " + query);
        try {
            ArrayList<ExecutionStatus> ret = new ArrayList<>();
            SimpleFeatureStore source = getStatusFeatureStore();
            LOGGER.fine("requesting " + query);

            SimpleFeatureCollection features = source.getFeatures(query);
            try (SimpleFeatureIterator itr = features.features()) {
                while (itr.hasNext()) {
                    SimpleFeature f = itr.next();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("adding " + f);
                    }
                    ret.add(featureToStatus(f));
                }
                return ret;
            }
        } catch (IOException e) {
            throw new WPSException("Failed to list statuses by query " + query, e);
        }
    }

    protected SimpleFeature statusToFeature(ExecutionStatus status) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
        builder.set(PROCESS_ID, status.getExecutionId());
        builder.set(CREATION, status.getCreationTime());
        builder.set(LASTUPDATE, status.getLastUpdated());
        builder.set(COMPLETION, status.getCompletionTime());
        builder.set(NODE_ID, status.getNodeId());
        builder.set(PHASE, status.getPhase());
        Name processName = status.getProcessName();
        builder.set(PROCESS_NAME, processName.getLocalPart());
        builder.set(PROCESS_NAME_URI, processName.getNamespaceURI());

        builder.set(PROGRESS, status.getProgress());
        ExecuteType request = status.getRequest();
        if (request != null) {
            builder.set(REQUEST, serializeRequest(request));
        }
        builder.set(SIMPLE_PROCESS_NAME, status.getSimpleProcessName());
        builder.set(TASK, status.getTask());
        builder.set(USER_NAME, status.getUserName());
        builder.set(ASYNC, status.isAsynchronous());
        Throwable exception = status.getException();
        if (exception != null) {
            builder.set(EXCEPTION_CLASS, exception.getClass().getName());
            builder.set(EXCEPTION_MESSAGE, exception.getMessage());
            StackTraceElement[] stackTrace = exception.getStackTrace();
            StringBuffer buf = new StringBuffer();
            for (StackTraceElement el : stackTrace) {
                buf.append(el.getClassName()).append(STACKTRACESEPERATOR);
                buf.append(el.getFileName()).append(STACKTRACESEPERATOR);
                buf.append(el.getMethodName()).append(STACKTRACESEPERATOR);
                buf.append(el.getLineNumber());
                buf.append("\n");
            }

            builder.set(STACK_TRACE, buf.toString().getBytes(Charset.forName("UTF-8")));
        }
        SimpleFeature feature = builder.buildFeature(null);
        return feature;
    }

    private byte[] serializeRequest(ExecuteType request) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder e = new Encoder(new WPSConfiguration());
        e.setIndenting(true);
        try {
            e.encode(request, WPS.Execute, out);
        } catch (IOException ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem encountered encoding WPS Request, moving on without it",
                    ex);
        }

        return out.toByteArray();
    }

    protected ExecutionStatus featureToStatus(SimpleFeature f) {
        HashMap<String, Object> attrs = new HashMap<>();
        if (f == null) {
            return null;
        }
        for (Property p : f.getProperties()) {
            //
            if (p.getValue() != null) {
                attrs.put(p.getName().toString(), p.getValue());
            }
        }
        Name processName = new NameImpl((String) attrs.get(PROCESS_NAME));
        String executionId = (String) attrs.get(PROCESS_ID);
        boolean asynchronous = Converters.convert(attrs.get(ASYNC), Boolean.class);

        ExecutionStatus status = new ExecutionStatus(processName, executionId, asynchronous);
        if (attrs.containsKey(REQUEST)) {
            ExecuteType request = buildRequest(attrs);

            status.setRequest(request);
        }
        String phase = (String) attrs.get(PHASE);
        ProcessState state = ProcessState.valueOf(phase);
        status.setPhase(state);

        status.setProgress((float) attrs.get(PROGRESS));
        status.setTask((String) attrs.get(TASK));
        status.setUserName((String) attrs.get(USER_NAME));
        if (attrs.containsKey(EXCEPTION_MESSAGE)) {
            status.setException(buildException(attrs, status));
        }
        // set the time stamps last as other items may set them for us but
        // we know best here!
        status.setCreationTime((Date) attrs.get(CREATION));
        if (attrs.containsKey(COMPLETION)) {
            status.setCompletionTime((Date) attrs.get(COMPLETION));
        }
        if (attrs.containsKey(LASTUPDATE)) {
            status.setLastUpdated((Date) attrs.get(LASTUPDATE));
        }

        return status;
    }

    private ExecuteType buildRequest(HashMap<String, Object> attrs) {
        byte[] req = (byte[]) attrs.get(REQUEST);
        Parser parser = new Parser(new WPSConfiguration());
        ExecuteType request = null;
        try {
            request = (ExecuteType) parser.parse(new ByteArrayInputStream(req));
        } catch (IOException | SAXException | ParserConfigurationException e) {
            LOGGER.log(Level.WARNING, "Problem building WPS request for status", e);
        }

        return request;
    }

    private Exception buildException(HashMap<String, Object> attrs, ExecutionStatus status) {
        String message = (String) attrs.get(EXCEPTION_MESSAGE);
        Exception exc = new Exception(message);
        // see if we can rebuild the exception
        try {
            Constructor<?> con =
                    this.getClass()
                            .getClassLoader()
                            .loadClass((String) attrs.get(EXCEPTION_CLASS))
                            .getConstructor(String.class);
            exc = (Exception) con.newInstance(message);

        } catch (InstantiationException
                | IllegalAccessException
                | ClassNotFoundException
                | NoSuchMethodException
                | SecurityException
                | IllegalArgumentException
                | InvocationTargetException e) {
            // too bad, I don't care
            LOGGER.log(Level.FINE, "Couldn't reinstaniate Exception for WPS status", e);
        }

        byte[] r = (byte[]) attrs.get(STACK_TRACE);
        ArrayList<StackTraceElement> trace = new ArrayList<>();

        for (String line : new String(r, Charset.forName("UTF-8")).split("\n")) {
            String[] parts = line.split(STACKTRACESEPERATOR);
            String declaringClass = parts[0];
            String fileName = parts[1];
            String methodName = parts[2];
            int lineNumber = Integer.parseInt(parts[3]);
            StackTraceElement t =
                    new StackTraceElement(declaringClass, methodName, fileName, lineNumber);

            trace.add(t);
        }
        exc.setStackTrace(trace.toArray(new StackTraceElement[] {}));
        return exc;
    }

    @Override
    public boolean supportsPredicate() {
        return false;
    }

    @Override
    public boolean supportsPaging() {
        return true;
    }
}
