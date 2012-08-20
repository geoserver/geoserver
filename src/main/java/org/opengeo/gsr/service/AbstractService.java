package org.opengeo.gsr.service;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */

public class AbstractService {

    private String name;

    private ServiceType serviceType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public AbstractService(String name, ServiceType serviceType) {
        super();
        this.name = name;
        this.serviceType = serviceType;
    }

}
