package org.geoserver.monitor.rest;

import org.geoserver.monitor.Monitor;

public class OwsRequestResource extends RequestResource {

    public OwsRequestResource(Monitor monitor) {
        super(monitor);
    }
    
    @Override
    protected Object handleObjectGet() throws Exception {
        String req = getAttribute("request");
        if (req == null) {
            //return all
            return monitor.getDAO().getOwsRequests();
        }
        else {
            return super.handleObjectGet();
        }
    }

}
