package org.opengeo.gsr.service;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("GeometryService")
public class GeometryService extends AbstractService {

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

    
    public GeometryService(String name) {
        this.name = name;
        this.serviceType = ServiceType.GeometryServer;
    }
}
