/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import java.io.Serializable;

/**
 * SPI class which is used by JMSManager to instantiate the relative handler.<br>
 * Its implementations may be loaded into the Spring context as a singleton.<br>
 * SPI bean id name MUST be the same as SPI SimpleClassName <br>
 *
 * @see {@link JMSEventHandler}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @param <S> ToggleType implementing Serializable
 * @param <O> ToggleType of the object to handle
 */
public abstract class JMSEventHandlerSPI<S extends Serializable, O> {
    /**
     * The key of the property stored into message which tells the Handler used to serialize the
     * message and the one which will be used to de-serialize and synchronize
     */
    private static final String PROPERTY_KEY = "JMSEventHandlerSPI";

    /**
     * Integer representing the priority of this handler:<br>
     *
     * <p><b>Lower</b> value means <b>higher</b> priority.
     */
    private final int priority;

    public JMSEventHandlerSPI(final int priority) {
        this.priority = priority;
    }

    /** @return the priority */
    public final int getPriority() {
        return priority;
    }

    public static String getKeyName() {
        return PROPERTY_KEY;
    }

    public abstract boolean canHandle(final Object event);

    public abstract JMSEventHandler<S, O> createHandler();
}
