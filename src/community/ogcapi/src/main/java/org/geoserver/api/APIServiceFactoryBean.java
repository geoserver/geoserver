package org.geoserver.api;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** Builds a OWS {@link Service} object finding all OGC API controllers */
public class APIServiceFactoryBean implements FactoryBean, ApplicationContextAware {

    static final Logger LOGGER = Logging.getLogger(APIServiceFactoryBean.class);

    private final String serviceName;
    private final Version version;
    private Service service;

    public APIServiceFactoryBean(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = new Version(version);
    }

    @Override
    public Object getObject() throws Exception {
        return service;
    }

    @Override
    public Class<?> getObjectType() {
        return Service.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        List<Object> serviceBeans =
                applicationContext
                        .getBeansWithAnnotation(APIService.class)
                        .values()
                        .stream()
                        .filter(
                                bean -> {
                                    APIService annotation =
                                            APIDispatcher.getApiServiceAnnotation(bean.getClass());
                                    return serviceName.equalsIgnoreCase(annotation.service())
                                            && version.equals(new Version(annotation.version()));
                                })
                        .collect(Collectors.toList());

        if (serviceBeans.isEmpty()) {
            throw new RuntimeException(
                    "Was expecting a service bean marked with service name '"
                            + serviceName
                            + " and version "
                            + version
                            + "' but found none");
        }

        List<Object> coreBeans =
                serviceBeans
                        .stream()
                        .filter(
                                bean -> {
                                    APIService annotation =
                                            APIDispatcher.getApiServiceAnnotation(bean.getClass());
                                    return annotation.core();
                                })
                        .collect(Collectors.toList());
        Object coreBean;
        if (coreBeans.isEmpty()) {
            LOGGER.log(
                    Level.WARNING,
                    "No is marked as 'core' for service "
                            + serviceName
                            + " a random one will be picked for OWS service compatibility.");
            coreBean = serviceBeans.get(0);
        } else {
            coreBean = coreBeans.get(0);

            if (coreBeans.size() > 1) {
                LOGGER.log(
                        Level.WARNING,
                        "More than one bean is marked as 'core' for service "
                                + serviceName
                                + " a random one will be picked for OWS service compatibility.");
            }
        }

        APIService annotation = coreBean.getClass().getAnnotation(APIService.class);

        List<String> operations =
                serviceBeans
                        .stream()
                        .flatMap(sb -> Arrays.stream(sb.getClass().getMethods()))
                        .filter(m -> APIDispatcher.hasRequestMapping(m))
                        .map(m -> APIDispatcher.getOperationName(m))
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

        this.service = new Service(annotation.service(), coreBean, version, operations);
        this.service.setCustomCapabilitiesLink("../" + annotation.landingPage());
    }
}
