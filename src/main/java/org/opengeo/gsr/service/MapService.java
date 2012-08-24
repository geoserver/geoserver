package org.opengeo.gsr.service;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 
 *  @author Juan Marin, OpenGeo
 *
 */

@XStreamAlias("")
public class MapService extends AbstractService {

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

    public MapService(String name) {
        this.name = name;
        serviceType = ServiceType.MapServer;
    }

}
