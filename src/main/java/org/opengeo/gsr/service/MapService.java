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

    public MapService(String name) {
        this.name = name;
        type = ServiceType.MapServer;
    }

}
