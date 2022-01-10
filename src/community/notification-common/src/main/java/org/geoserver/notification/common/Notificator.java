/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import com.thoughtworks.xstream.XStream;
import java.io.Serializable;

/**
 * The configuration of {@link MessageProcessor}, populated by {@link XStream}
 *
 * @author Xandros
 */
public class Notificator implements Serializable {

    private static final long serialVersionUID = 6185508068154638658L;

    /**
     * Size of queue used to holding the {@link NotificationProcessor} tasks before they are
     * executed by message processor
     */
    private Long queueSize;

    /**
     * Number of threads used to manage the {@link NotificationProcessor} tasks in the message
     * processor queue
     */
    private Long processorThreads;

    /** CQL to filter the {@link Notification} before accepted by the message processor */
    private String messageFilter;

    /** Notification processor implementation used by message processor */
    private NotificationProcessor genericProcessor;

    public Long getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Long queueSize) {
        this.queueSize = queueSize;
    }

    public Long getProcessorThreads() {
        return processorThreads;
    }

    public void setProcessorThreads(Long processorThreads) {
        this.processorThreads = processorThreads;
    }

    public String getMessageFilter() {
        return messageFilter;
    }

    public void setMessageFilter(String messageFilter) {
        this.messageFilter = messageFilter;
    }

    public NotificationProcessor getGenericProcessor() {
        return genericProcessor;
    }

    public void setGenericProcessor(NotificationProcessor genericProcessor) {
        this.genericProcessor = genericProcessor;
    }
}
