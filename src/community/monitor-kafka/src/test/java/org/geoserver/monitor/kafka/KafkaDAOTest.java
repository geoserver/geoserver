package org.geoserver.monitor.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.geoserver.monitor.Category;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataRecord;
import org.geoserver.monitor.Status;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

public class KafkaDAOTest {

    private KafkaDAO kafkaDAO;

    @Mock
    private Producer<String, RequestDataRecord> producer;

    @Mock
    private ConnectionTester connectionTester;

    @Mock
    private MonitorConfig config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.getProperties()).thenReturn(new Properties());
        kafkaDAO = new KafkaDAO();
        kafkaDAO.producer = producer;
        kafkaDAO.connectionTester = connectionTester;
        kafkaDAO.config = new KafkaMonitorConfig(config);
        when(connectionTester.testConnection()).thenReturn(true);
        when(connectionTester.testConnection(any(), any())).thenReturn(true);
    }

    @Test
    public void testGetName() {
        Assertions.assertEquals("kafka", kafkaDAO.getName());
    }

    @Test
    public void testInitConfig() {
        MonitorConfig config = mock(MonitorConfig.class);
        Properties props = new Properties();
        props.setProperty("kafka.topic", "yay-its-me");
        when(config.getProperties()).thenReturn(props);
        kafkaDAO.init(config);
        Assertions.assertNotNull(kafkaDAO.config);
        Assertions.assertEquals("yay-its-me", kafkaDAO.config.getTopic());
    }

    @Test
    public void testInit() {
        RequestData requestData = new RequestData();
        RequestData initializedData = kafkaDAO.init(requestData);
        Assertions.assertNotNull(initializedData.getId());
    }

    @Test
    public void testProducer() {
        Properties props = new Properties();
        props.setProperty("kafka.topic", "yay-its-me");
        props.setProperty("kafka.bootstrap.servers", "kafka-bootstrap-server:9092");

        when(config.getProperties()).thenReturn(props);

        // verify the producer is created only if not set (setup has set it already)
        try (MockedConstruction<KafkaProducer> mocked = mockConstruction(KafkaProducer.class, (mock, context) -> {
            // grab the constructor argument
            var param = context.arguments().get(0);

            Properties expectedProps = new Properties();
            expectedProps.put("bootstrap.servers", "kafka-bootstrap-server:9092");
            expectedProps.put("value.serializer", KafkaAvroSerializer.class);
            expectedProps.put("key.serializer", StringSerializer.class);

            // verify the constructor argument is as expected
            Assertions.assertEquals(expectedProps, param);
        })) {
            kafkaDAO.getProducer();
            kafkaDAO.getProducer();
            Assertions.assertEquals(0, mocked.constructed().size());

            kafkaDAO.producer = null;

            kafkaDAO.getProducer();
            Assertions.assertEquals(1, mocked.constructed().size());
        }
        Assertions.assertNotNull(kafkaDAO.getProducer());
    }

    @Test
    public void testAdd() {
        RequestData requestData = mock(RequestData.class);
        when(requestData.getId()).thenReturn(1L);
        // Set up other necessary mock responses for requestData's methods

        kafkaDAO.add(requestData);
        verify(producer).send(any());
    }

    @Test
    public void testUpdate() {
        RequestData requestData = mock(RequestData.class);
        when(requestData.getId()).thenReturn(1L);
        // Set up other necessary mock responses for requestData's methods

        kafkaDAO.update(requestData);
        verify(producer).send(any());
    }

    @Test
    public void testSave() {
        RequestData requestData = mock(RequestData.class);
        when(requestData.getId()).thenReturn(1L);
        // Set up other necessary mock responses for requestData's methods

        kafkaDAO.save(requestData);
        verify(producer).send(any());
    }

    @Test
    public void testNoProduceCallRequestDataNull() {
        kafkaDAO.save(null);
        verify(producer, times(0)).send(any());
    }

    @Test
    public void testMapRequestDataNonNull() {
        // Create a non-null RequestData mock
        RequestData data = mock(RequestData.class);
        when(data.getId()).thenReturn(1L);
        when(data.getStatus()).thenReturn(RequestData.Status.FINISHED);
        when(data.getCategory()).thenReturn(RequestData.Category.REST);
        when(data.getHttpMethod()).thenReturn("GET");
        when(data.getBbox()).thenReturn(new ReferencedEnvelope(10.0, 20.0, 30.0, 40.0, DefaultGeographicCRS.WGS84));
        when(data.getPath()).thenReturn("path");
        when(data.getQueryString()).thenReturn("queryString");
        when(data.getBody()).thenReturn("body".getBytes());
        when(data.getBodyContentLength()).thenReturn(4L);
        when(data.getBodyContentType()).thenReturn("text/plain");
        when(data.getStartTime()).thenReturn(new Date(1L));
        when(data.getEndTime()).thenReturn(new Date(2L));
        when(data.getRemoteAddr()).thenReturn("remote-addr");
        when(data.getHost()).thenReturn("host");
        when(data.getRemoteHost()).thenReturn("1.2.3.4");
        when(data.getRemoteUserAgent()).thenReturn("remote-user-agent");
        when(data.getRemoteUser()).thenReturn("remote-user");
        when(data.getRemoteCountry()).thenReturn("remote-country");
        when(data.getRemoteCity()).thenReturn("remote-city");
        when(data.getInternalHost()).thenReturn("internal-host");
        when(data.getService()).thenReturn("service");
        when(data.getOperation()).thenReturn("operation");
        when(data.getOwsVersion()).thenReturn("1.0.0");
        when(data.getSubOperation()).thenReturn("sub-operation");
        when(data.getResponseContentType()).thenReturn("response-content-type");
        when(data.getErrorMessage()).thenReturn("error-message");
        when(data.getResources()).thenReturn(List.of("resource1", "resource2"));
        when(data.getCacheResult()).thenReturn("cache-result");
        when(data.getMissReason()).thenReturn("miss-reason");

        // Invoke the method
        RequestDataRecord record = kafkaDAO.mapRequestData(data);

        // Validate the results
        Assertions.assertNotNull(record);
        Assertions.assertEquals(1L, record.getId());
        Assertions.assertEquals(Status.FINISHED, record.getStatus());
        Assertions.assertEquals(Category.REST, record.getCategory());
        Assertions.assertEquals("GET", record.getHttpMethod());
        Assertions.assertEquals(10.0, record.getMinx());
        Assertions.assertEquals(20.0, record.getMaxx());
        Assertions.assertEquals(30.0, record.getMiny());
        Assertions.assertEquals(40.0, record.getMaxy());
        Assertions.assertEquals("path", record.getPath());
        Assertions.assertEquals("queryString", record.getQueryString());
        Assertions.assertEquals("text/plain", record.getBodyContentType());
        Assertions.assertEquals(4, record.getBodyContentLength());
        Assertions.assertEquals(
                "body", StandardCharsets.UTF_8.decode(record.getBody()).toString());
        Assertions.assertEquals(new Date(1L).toInstant(), Instant.ofEpochMilli(record.getStartTime()));
        Assertions.assertEquals(new Date(2L).toInstant(), Instant.ofEpochMilli(record.getEndTime()));
        Assertions.assertEquals("remote-addr", record.getRemoteAddress());
        Assertions.assertEquals("host", record.getHost());
        Assertions.assertEquals("1.2.3.4", record.getRemoteHost());
        Assertions.assertEquals("remote-user-agent", record.getRemoteUserAgent());
        Assertions.assertEquals("remote-user", record.getRemoteUser());
        Assertions.assertEquals("remote-country", record.getRemoteCountry());
        Assertions.assertEquals("remote-city", record.getRemoteCity());
        Assertions.assertEquals("internal-host", record.getInternalHost());
        Assertions.assertEquals("service", record.getService());
        Assertions.assertEquals("operation", record.getOperation());
        Assertions.assertEquals("1.0.0", record.getOwsVersion());
        Assertions.assertEquals("sub-operation", record.getSubOperation());
        Assertions.assertEquals("response-content-type", record.getResponseContentType());
        Assertions.assertEquals("error-message", record.getErrorMessage());
        Assertions.assertEquals("resource1", record.getResources().get(0));
        Assertions.assertEquals("resource2", record.getResources().get(1));
        Assertions.assertEquals("cache-result", record.getCacheResult());
        Assertions.assertEquals("miss-reason", record.getMissReason());
    }

    @Test
    public void testMapRequestDataNull() {
        // Invoke the method with null
        RequestDataRecord record = kafkaDAO.mapRequestData(null);

        // Validate the results
        Assertions.assertNull(record);
    }

    @Test
    public void testMapRequestDataWithNullValues() {
        // Create a RequestData mock with null values
        RequestData data = mock(RequestData.class);
        when(data.getId()).thenReturn(1L);
        when(data.getStatus()).thenReturn(null);
        when(data.getCategory()).thenReturn(null);
        // ... Set other values as null
        when(data.getHttpMethod()).thenReturn(null);
        when(data.getBbox()).thenReturn(null);
        when(data.getQueryString()).thenReturn(null);
        when(data.getBody()).thenReturn(null);
        when(data.getBodyContentLength()).thenReturn(0L);
        when(data.getBodyContentType()).thenReturn(null);

        // Invoke the method
        RequestDataRecord record = kafkaDAO.mapRequestData(data);

        // Validate the results for handling null values
        Assertions.assertNotNull(record);
        Assertions.assertEquals(1L, record.getId());
        Assertions.assertEquals(Status.UNKNOWN_STATUS, record.getStatus());
        Assertions.assertEquals(Category.UNKNOWN_CATEGORY, record.getCategory());
        Assertions.assertEquals("", record.getHttpMethod());
        Assertions.assertEquals(0.0, record.getMinx());
        Assertions.assertEquals(0.0, record.getMiny());
        Assertions.assertEquals(0.0, record.getMaxx());
        Assertions.assertEquals(0.0, record.getMaxy());
        Assertions.assertEquals("", record.getQueryString());
        Assertions.assertEquals("", record.getBodyContentType());
        Assertions.assertEquals(0, record.getBodyContentLength());
        Assertions.assertEquals(
                "\\u00", StandardCharsets.UTF_8.decode(record.getBody()).toString());
    }

    @Test
    public void testDestroy() throws Exception {
        kafkaDAO.destroy();
        verify(producer).close(any());
        Assertions.assertNull(kafkaDAO.producer);
    }

    @Test
    public void testDispose() {
        kafkaDAO.dispose();
        verify(producer).close(any());
        Assertions.assertNull(kafkaDAO.producer);
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(producer, config);
    }
}
