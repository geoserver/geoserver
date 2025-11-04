/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.monitor.kafka;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.geoserver.monitor.Category;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataRecord;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.Status;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

public class KafkaDAO implements MonitorDAO, DisposableBean {

    public static final String NAME = "kafka";

    KafkaMonitorConfig config;

    ConnectionTester connectionTester;

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    AtomicLong REQUEST_ID_GEN = new AtomicLong(1);

    Producer<String, RequestDataRecord> producer;

    public Producer<String, RequestDataRecord> getProducer() {
        if (producer == null && config.isEnabled()) {
            LOGGER.info("using kafka topic: " + config.getTopic());
            if (connectionTester.testConnection(config.getKafkaProperties(), config.getTopic())) {
                producer = new KafkaProducer<>(config.getKafkaProperties());
            }
        }
        return producer;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(MonitorConfig config) {
        this.config = new KafkaMonitorConfig(config);
        this.connectionTester = new ConnectionTester(this.config);
    }

    @Override
    public RequestData init(RequestData data) {
        data.setId(REQUEST_ID_GEN.getAndIncrement());
        return data;
    }

    @Override
    public void add(RequestData data) {
        produces(mapRequestData(data));
    }

    private void produces(RequestDataRecord requestDataRecord) {
        if (requestDataRecord == null) {
            return;
        }
        Producer<String, RequestDataRecord> producer = getProducer();
        if (producer == null) {
            return;
        }
        producer.send(new ProducerRecord<>(config.getTopic(), null, requestDataRecord));
    }

    @Override
    public void update(RequestData data) {
        produces(mapRequestData(data));
    }

    @Override
    public void save(RequestData data) {
        produces(mapRequestData(data));
    }

    @Override
    public RequestData getRequest(long id) {
        return null;
    }

    @Override
    public List<RequestData> getRequests() {
        return null;
    }

    @Override
    public List<RequestData> getRequests(Query query) {
        return null;
    }

    @Override
    public void getRequests(Query query, RequestDataVisitor visitor) {}

    @Override
    public long getCount(Query query) {
        return 0;
    }

    @Override
    public Iterator<RequestData> getIterator(Query query) {
        return null;
    }

    @Override
    public List<RequestData> getOwsRequests() {
        return null;
    }

    @Override
    public List<RequestData> getOwsRequests(String service, String operation, String version) {
        return null;
    }

    @Override
    public void clear() {}

    @Override
    public void dispose() {
        if (producer != null) {
            producer.close(Duration.ofSeconds(10));
            producer = null;
        }
    }

    @Override
    public void destroy() throws Exception {
        dispose();
        REQUEST_ID_GEN = new AtomicLong(1);
    }

    RequestDataRecord mapRequestData(RequestData data) {
        if (data == null) {
            return null;
        }

        RequestDataRecord.Builder recordData = RequestDataRecord.newBuilder();
        recordData.setId(data.getId());

        if (data.getStatus() != null) {
            recordData.setStatus(Status.valueOf(data.getStatus().name()));
        } else {
            recordData.setStatus(Status.UNKNOWN_STATUS);
        }

        if (data.getCategory() != null) {
            recordData.setCategory(Category.valueOf(data.getCategory().name()));
        } else {
            recordData.setCategory(Category.UNKNOWN_CATEGORY);
        }

        if (data.getPath() != null) {
            recordData.setPath(data.getPath());
        }

        if (data.getQueryString() != null) {
            recordData.setQueryString(data.getQueryString());
        }

        if (data.getBody() != null) {
            recordData.setBody(ByteBuffer.wrap(data.getBody()));
        }

        recordData.setBodyContentLength(data.getBodyContentLength());

        if (data.getBodyContentType() != null) {
            recordData.setBodyContentType(data.getBodyContentType());
        }

        if (data.getHttpMethod() != null) {
            recordData.setHttpMethod(data.getHttpMethod());
        }

        if (data.getStartTime() != null) {
            recordData.setStartTime(data.getStartTime().toInstant().toEpochMilli());
        }

        if (data.getEndTime() != null) {
            recordData.setEndTime(data.getEndTime().toInstant().toEpochMilli());
        }

        recordData.setTotalTime(data.getTotalTime());

        if (data.getRemoteAddr() != null) {
            recordData.setRemoteAddress(data.getRemoteAddr());
        }

        if (data.getRemoteHost() != null) {
            recordData.setRemoteHost(data.getRemoteHost());
        }

        if (data.getHost() != null) {
            recordData.setHost(data.getHost());
        }

        if (data.getInternalHost() != null) {
            recordData.setInternalHost(data.getInternalHost());
        }

        if (data.getRemoteUser() != null) {
            recordData.setRemoteUser(data.getRemoteUser());
        }

        if (data.getRemoteUserAgent() != null) {
            recordData.setRemoteUserAgent(data.getRemoteUserAgent());
        }

        if (data.getRemoteCountry() != null) {
            recordData.setRemoteCountry(data.getRemoteCountry());
        }

        if (data.getRemoteCity() != null) {
            recordData.setRemoteCity(data.getRemoteCity());
        }

        recordData.setRemoteLat(data.getRemoteLat());
        recordData.setRemoteLon(data.getRemoteLon());

        if (data.getService() != null) {
            recordData.setService(data.getService());
        }

        if (data.getOperation() != null) {
            recordData.setOperation(data.getOperation());
        }

        if (data.getOwsVersion() != null) {
            recordData.setOwsVersion(data.getOwsVersion());
        }

        if (data.getSubOperation() != null) {
            recordData.setSubOperation(data.getSubOperation());
        }

        recordData.setResponseLength(data.getResponseLength());

        if (data.getResponseContentType() != null) {
            recordData.setResponseContentType(data.getResponseContentType());
        }

        if (data.getErrorMessage() != null) {
            recordData.setErrorMessage(data.getErrorMessage());
        }

        if (data.getResources() != null) {
            for (String r : data.getResources()) {
                if (r != null) {
                    if (recordData.getResources() == null) {
                        recordData.setResources(new ArrayList<>());
                    }
                    recordData.getResources().add(r);
                }
            }
        }
        if (data.getBbox() != null) {
            recordData.setMinx(data.getBbox().getMinX());
            recordData.setMiny(data.getBbox().getMinY());
            recordData.setMaxx(data.getBbox().getMaxX());
            recordData.setMaxy(data.getBbox().getMaxY());
            recordData.setCoordinateReferenceSystem(
                    data.getBbox().getCoordinateReferenceSystem().getName().toString());
        }

        if (data.getCacheResult() != null) {
            recordData.setCacheResult(data.getCacheResult());
        }

        if (data.getMissReason() != null) {
            recordData.setMissReason(data.getMissReason());
        }

        return recordData.build();
    }
}
