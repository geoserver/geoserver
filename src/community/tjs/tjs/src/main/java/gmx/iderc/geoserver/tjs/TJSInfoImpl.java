/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs;

import org.geoserver.config.impl.ServiceInfoImpl;

/**
 * @author root
 */
public class TJSInfoImpl extends ServiceInfoImpl implements TJSInfo {
    protected ServiceLevel serviceLevel = ServiceLevel.COMPLETE;
    //protected ServiceLevel serviceLevel = ServiceLevel.BASIC;
    protected boolean canonicalSchemaLocation = false;

    String tjsServerBaseURL;

    public TJSInfoImpl() {
        setId("tjs");
    }

    public String getTjsServerBaseURL() {
        return tjsServerBaseURL;
    }

    public void setTjsServerBaseURL(String tjsServerBaseURL) {
        this.tjsServerBaseURL = tjsServerBaseURL;
    }

    public ServiceLevel getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(ServiceLevel serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public boolean isCanonicalSchemaLocation() {
        return canonicalSchemaLocation;
    }

    public void setCanonicalSchemaLocation(boolean canonicalSchemaLocation) {
        this.canonicalSchemaLocation = canonicalSchemaLocation;
    }

}
