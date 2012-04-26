/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.service;

import org.geoserver.config.ServiceInfo;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.platform.ServiceException;

public interface GeoSynchronizationService {

    ServiceInfo getServiceInfo();

    Object getCapabilities(final GetCapabilities request) throws ServiceException;

    TransactionResponse transaction(final Transaction request) throws ServiceException;

    FeedImpl getEntries(final GetEntries request) throws ServiceException;

    AcceptChangeResponse acceptChange(final AcceptChange request) throws ServiceException;

    RejectChangeResponse rejectChange(final RejectChange request) throws ServiceException;

    ReviewedChangesResponse reviewedChanges(final ReviewedChanges request) throws ServiceException;

    CreateTopicResponse createTopic(final CreateTopic request) throws ServiceException;

    RemoveTopicResponse removeTopic(final RemoveTopic request) throws ServiceException;

    ListTopicsResponse listTopics(final ListTopics request) throws ServiceException;

    SubscribeResponse subscribe(final Subscribe request) throws ServiceException;

    ListSubscriptionsResponse listSubscriptions(final ListSubscriptions request)
            throws ServiceException;
}
