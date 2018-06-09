/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.events;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * An instance of this class can be used to send over the Application Context ToggleEvent events.
 * Those events can be used by the a producer to enable or disable the message events production
 * over the JMS channel.
 *
 * @see {@link JMSEventListener}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class ToggleSwitch implements ApplicationContextAware {

    private ApplicationContext ctx;

    /** true if the toggle can run enable and disable publishing events, false otherwise */
    private volatile Boolean status = true;

    private final ToggleType toggleType;

    public ToggleSwitch(final ToggleType toggleType) {
        this.toggleType = toggleType;
    }

    public ToggleSwitch(
            final ApplicationContext ctx, final Boolean status, final ToggleType toggleType) {
        super();
        this.ctx = ctx;
        this.status = status;
        this.toggleType = toggleType;
    }

    /** @param toggleEnabled set enabled and disabled the toggle itself */
    public final void setToggle(boolean status) {
        synchronized (this.status) {
            this.status = status;
        }
    }

    /** @return the true if the toggle can enable and disable, false otherwise */
    public final boolean isToggleEnabled() {
        return status;
    }

    public void setApplicationContext(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public void enable() {
        if (isToggleEnabled()) {
            ctx.publishEvent(new ToggleEvent(Boolean.TRUE, toggleType));
        }
    }

    public void disable() {
        if (isToggleEnabled()) {
            ctx.publishEvent(new ToggleEvent(Boolean.FALSE, toggleType));
        }
    }
}
