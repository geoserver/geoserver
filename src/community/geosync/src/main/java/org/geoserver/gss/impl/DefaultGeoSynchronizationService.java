/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl;

import org.geoserver.gss.config.GSSInfo;
import org.geoserver.gss.impl.change.AcceptChangeOp;
import org.geoserver.gss.impl.change.RejectChangeOp;
import org.geoserver.gss.impl.change.ReviewedChangesOp;
import org.geoserver.gss.impl.discovery.GetCapabilitiesOp;
import org.geoserver.gss.impl.query.GetEntriesOp;
import org.geoserver.gss.impl.subscription.CreateTopicOp;
import org.geoserver.gss.impl.subscription.ListSubscriptionsOp;
import org.geoserver.gss.impl.subscription.ListTopicsOp;
import org.geoserver.gss.impl.subscription.RemoveTopicOp;
import org.geoserver.gss.impl.subscription.SubscribeOp;
import org.geoserver.gss.impl.transaction.TransactionOp;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.gss.service.AcceptChange;
import org.geoserver.gss.service.AcceptChangeResponse;
import org.geoserver.gss.service.CreateTopic;
import org.geoserver.gss.service.CreateTopicResponse;
import org.geoserver.gss.service.GeoSynchronizationService;
import org.geoserver.gss.service.GetCapabilities;
import org.geoserver.gss.service.GetEntries;
import org.geoserver.gss.service.ListSubscriptions;
import org.geoserver.gss.service.ListSubscriptionsResponse;
import org.geoserver.gss.service.ListTopics;
import org.geoserver.gss.service.ListTopicsResponse;
import org.geoserver.gss.service.RejectChange;
import org.geoserver.gss.service.RejectChangeResponse;
import org.geoserver.gss.service.RemoveTopic;
import org.geoserver.gss.service.RemoveTopicResponse;
import org.geoserver.gss.service.ReviewedChanges;
import org.geoserver.gss.service.ReviewedChangesResponse;
import org.geoserver.gss.service.Subscribe;
import org.geoserver.gss.service.SubscribeResponse;
import org.geoserver.gss.service.Transaction;
import org.geoserver.gss.service.TransactionResponse;
import org.geoserver.platform.ServiceException;
import org.springframework.util.Assert;

/**
 * @author Gabriel Roldan
 * 
 */
public class DefaultGeoSynchronizationService implements GeoSynchronizationService {

    private final GSS gss;

    public DefaultGeoSynchronizationService(final GSS gss) {
        this.gss = gss;
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#getServiceInfo()
     */
    public GSSInfo getServiceInfo() {
        return gss.getGssInfo();
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#getCapabilities(org.geoserver.gss.service.GetCapabilities)
     */
    public Object getCapabilities(final GetCapabilities request) throws ServiceException {
        return new GetCapabilitiesOp().execute(request, gss);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#transaction(org.geoserver.gss.service.Transaction)
     */
    public TransactionResponse transaction(final Transaction request) throws ServiceException {
        return new TransactionOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#getEntries(org.geoserver.gss.service.GetEntries)
     */
    public FeedImpl getEntries(final GetEntries request) throws ServiceException {
        Assert.notNull(request, "request is null");
        Assert.notNull(request.getFeed(), "request.getFeed() is null");
        Assert.notNull(request.getOutputFormat(), "request.getOutputFormat() is null");

        return new GetEntriesOp(gss).execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#acceptChange(org.geoserver.gss.service.AcceptChange)
     */
    public AcceptChangeResponse acceptChange(final AcceptChange request) throws ServiceException {
        return new AcceptChangeOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#rejectChange(org.geoserver.gss.service.RejectChange)
     */
    public RejectChangeResponse rejectChange(final RejectChange request) throws ServiceException {
        return new RejectChangeOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#reviewedChanges(org.geoserver.gss.service.ReviewedChanges)
     */
    public ReviewedChangesResponse reviewedChanges(final ReviewedChanges request)
            throws ServiceException {
        return new ReviewedChangesOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#createTopic(org.geoserver.gss.service.CreateTopic)
     */
    public CreateTopicResponse createTopic(final CreateTopic request) throws ServiceException {
        return new CreateTopicOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#removeTopic(org.geoserver.gss.service.RemoveTopic)
     */
    public RemoveTopicResponse removeTopic(final RemoveTopic request) throws ServiceException {
        return new RemoveTopicOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#listTopics(org.geoserver.gss.service.ListTopics)
     */
    public ListTopicsResponse listTopics(final ListTopics request) throws ServiceException {
        return new ListTopicsOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#subscribe(org.geoserver.gss.service.Subscribe)
     */
    public SubscribeResponse subscribe(final Subscribe request) throws ServiceException {
        return new SubscribeOp().execute(request);
    }

    /**
     * @see org.geoserver.gss.service.GeoSynchronizationService#listSubscriptions(org.geoserver.gss.service.ListSubscriptions)
     */
    public ListSubscriptionsResponse listSubscriptions(final ListSubscriptions request)
            throws ServiceException {
        return new ListSubscriptionsOp().execute(request);
    }

}
