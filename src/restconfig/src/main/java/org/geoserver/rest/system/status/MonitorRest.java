/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.system.status;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.system.status.MetricValue;
import org.geoserver.system.status.Metrics;
import org.geoserver.system.status.SystemInfoCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint that return the available system information.
 *
 * <p>Every time this endpoint is hitted the informations are retrieved from the system, no cached
 * information is used.
 *
 * <p>HTML, XML and JSON are supported.
 *
 * @author sandr
 */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/about/system-status")
public class MonitorRest extends RestBaseController {

    @Autowired SystemInfoCollector systemInfoCollector;

    @GetMapping(
        value = "",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    @ResponseStatus(HttpStatus.OK)
    public RestWrapper<Metrics> getData(HttpServletRequest request, HttpServletResponse response) {
        Metrics si = systemInfoCollector.retrieveAllSystemInfo();
        return wrapObject(si, Metrics.class);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        xs.alias("metric", MetricValue.class);
        xs.alias("metrics", Metrics.class);
        xs.omitField(MetricValue.class, "value");
        xs.registerConverter(new ValueHolderConverter());
        xs.aliasField("value", MetricValue.class, "holder");
        xs.addImplicitCollection(Metrics.class, "metrics");
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Arrays.asList(MetricValue.class));
    }

    /** Will convert a metric value to is correct representation. */
    private static final class ValueHolderConverter implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return MetricValue.ValueHolder.class.isAssignableFrom(type);
        }

        @Override
        public void marshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Object value = ((MetricValue.ValueHolder) source).getValue();
            writer.setValue(value != null ? value.toString() : "");
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            return null;
        }
    }
}
