package org.geoserver.wfs.notification;

public interface NotificationPublisher {
    /**
     * Fires the given notification.
     * @param byteString A serialized notification.
     */
    void publish(String byteString);
    
    /**
     * Whether or not this publisher can accept notifications. If not, the whole notification process is short-
     * circuited.
     * @return {@code true} is this publisher is ready.
     */
    boolean isReady();
}