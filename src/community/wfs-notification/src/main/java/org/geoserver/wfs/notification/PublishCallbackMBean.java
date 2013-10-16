/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;


public interface PublishCallbackMBean {
    public void resetStats();
    public long getUpdates();
    public long getDeletes();
    public double getAverageSerializationTime();
    public double getMinimumSerializationTime();
    public double getMaximumSerializationTime();
    public double getTotalSerializationTime();
    public long getAverageMessageSize();
    public long getMinimumMessageSize();
    public long getMaximumMessageSize();
    public long getTotalMessageSize();
    public String createSerializationTimeHistogram();
    public long getFailures();
    public Throwable getLastFailure();
}
