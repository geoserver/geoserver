package org.geoserver.status.monitoring.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.status.monitoring.collector.SystemInfoCollector;
import org.geoserver.status.monitoring.collector.SystemInfoProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.xstream.XStream;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/about/monitoring")
public class MonitorRest extends RestBaseController {

    private static Log log = LogFactory.getLog(MonitorRest.class);

    @Autowired
    SystemInfoCollector systemInfoCollector;

    // RESTful method
    @GetMapping(value = "", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    @ResponseStatus(HttpStatus.OK)
    public RestWrapper<Infos> getData(HttpServletRequest request, HttpServletResponse response) {
        log.debug("CALLED");
        Infos si = systemInfoCollector.retriveAllSystemInfo();
        return wrapObject(si, Infos.class);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        xs.alias("monitoring", Infos.class);
        xs.alias("infos", SystemInfoProperty.class);
        xs.addImplicitArray(Infos.class, "data");
        xs.registerConverter(new InfoListConverter());
        xs.registerConverter(new SystemInfoConverter());
    }

}
