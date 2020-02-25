/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

public class EchoParameter {

    private final String id;
    private final String parameter;
    private final boolean activated;

    public EchoParameter(String id, String parameter, boolean activated) {
        this.id = id;
        this.parameter = parameter;
        this.activated = activated;
    }

    public String getId() {
        return id;
    }

    public String getParameter() {
        return parameter;
    }

    public boolean getActivated() {
        return activated;
    }
}
