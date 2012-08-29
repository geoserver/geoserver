package org.opengeo.gsr.service;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("GeometryService")
public class GeometryService extends AbstractService {

    private String name;

    private ServiceType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType serviceType) {
        this.type = serviceType;
    }

    public GeometryService(String name) {
        this.name = name;
        this.type = ServiceType.GeometryServer;
    }
}
