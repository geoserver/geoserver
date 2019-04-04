/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.events;

import org.springframework.context.ApplicationEvent;

/**
 * event defining the new state of the producer
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class ToggleEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final ToggleType toggleType;

    public ToggleEvent(Boolean source, ToggleType toggleType) {
        super(source);
        this.toggleType = toggleType;
    }

    public ToggleType getType() {
        return this.toggleType;
    }

    public boolean toggleTo() {
        return (Boolean) getSource();
    }
}
