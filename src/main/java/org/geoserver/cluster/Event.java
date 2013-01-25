package org.geoserver.cluster;

import java.io.Serializable;

public class Event implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    Serializable source;

    public void setSource(Serializable source) {
        this.source = source;
    }

    public Serializable getSource() {
        return source;
    }

}
