/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.rest;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.configuration.BrokerConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.configuration.ToggleConfiguration;
import org.geoserver.cluster.events.ToggleType;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/cluster")
public class ClusterController extends AbstractCatalogController {

    @Autowired private Controller controller;

    @Autowired private JMSConfiguration config;

    public ClusterController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<Properties> getClusterConfiguration() {
        return wrapObject(config.getConfigurations(), Properties.class);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public ResponseEntity<String> postClusterConfiguration(
            @RequestBody Properties props, UriComponentsBuilder builder) throws IOException {
        for (Object key : props.keySet()) {
            String k = key.toString();
            final String value = props.get(key).toString();
            // store config
            config.putConfiguration(key.toString(), value);
            final Object oldValue = config.getConfiguration(k);
            if (props.get(k).equals(oldValue)) {
                continue;
            }
            if (key.equals(ConnectionConfiguration.CONNECTION_KEY)) {
                // CONNECTION
                controller.connectClient(Boolean.getBoolean(value));
            } else if (key.equals(ToggleConfiguration.TOGGLE_MASTER_KEY)) {
                // toggle MASTER
                controller.toggle(Boolean.getBoolean(value), ToggleType.MASTER);
            } else if (key.equals(ToggleConfiguration.TOGGLE_SLAVE_KEY)) {
                // toggle SLAVE
                controller.toggle(Boolean.getBoolean(value), ToggleType.SLAVE);
            } else if (key.equals(JMSConfiguration.INSTANCE_NAME_KEY)) {
                // InstanceName
                controller.setInstanceName(value);
            } else if (key.equals(BrokerConfiguration.BROKER_URL_KEY)) {
                // BROKER_URL
                controller.setBrokerURL(value);
            } else if (key.equals(ReadOnlyConfiguration.READ_ONLY_KEY)) {
                // ReadOnly
                controller.setReadOnly(Boolean.getBoolean(value));
            } else if (key.equals(JMSConfiguration.GROUP_KEY)) {
                // group
                controller.setGroup(value);
            }
        }
        // SAVE to disk
        controller.save();
        UriComponents uriComponents = builder.path("/cluster").build();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<>(props.toString(), headers, HttpStatus.CREATED);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return Properties.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {
        template.setObjectWrapper(
                new ObjectToMapWrapper<Properties>(Properties.class) {
                    @Override
                    protected void wrapInternal(
                            Map properties, SimpleHash model, Properties props) {
                        properties.putAll(props);
                    }
                });
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.getXStream().allowTypes(new Class[] {Properties.class});
    }
}
