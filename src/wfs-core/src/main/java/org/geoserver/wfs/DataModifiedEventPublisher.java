/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.data.DataModifiedEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Turns {@link TransactionEvent} into simpler {@link org.geoserver.data.DataModifiedEvent} for listeners that don't
 * need to get all details about the transaction itself
 */
public class DataModifiedEventPublisher implements TransactionListener, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void dataStoreChange(TransactionEvent event) throws WFSException {
        DataModifiedEvent springEvent = new DataModifiedEvent(this);
        applicationContext.publishEvent(springEvent);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
