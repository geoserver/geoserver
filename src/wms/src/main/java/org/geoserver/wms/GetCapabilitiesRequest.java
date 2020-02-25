/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

/**
 * This class enforces a standard interface for GetCapabilities requests.
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @version $Id$
 */
public class GetCapabilitiesRequest extends WMSRequest {

    private String updateSequence;

    private String namespace;

    private Boolean includeRootLayer = null;

    public GetCapabilitiesRequest() {
        super("GetCapabilities");
    }

    /**
     * Returns a string representation of this CapabilitiesRequest.
     *
     * @return a string of with the service and version.
     */
    public String toString() {
        return "GetCapabilities [service: WMS, version: " + version + "]";
    }

    /** @return the updateSequence */
    public String getUpdateSequence() {
        return updateSequence;
    }

    /** @param updateSequence the updateSequence to set */
    public void setUpdateSequence(String updateSequence) {
        this.updateSequence = updateSequence;
    }

    /**
     * Returns the namespace prefix we should filter layers on (if any) (used in WMS only atm, but
     * could be easily expanded to wfs/wcs too)
     *
     * @return the namespace prefix which to filter the content for
     */
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return whether to always include the root layer also when there is a single top Layer
     *     element *
     */
    public Boolean isRootLayerEnabled() {
        return includeRootLayer;
    }

    public void setRootLayerEnabled(Boolean includeRootLayer) {
        this.includeRootLayer = includeRootLayer;
    }
}
