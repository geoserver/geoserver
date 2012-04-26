/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import java.io.IOException;
import java.util.Set;

import javax.xml.namespace.QName;

import org.eclipse.xsd.XSDSchema;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geotools.xml.XSD;

/**
 * This interface contains the qualified names of all the types,elements, and attributes in the
 * {@code http://www.opengis.net/gss/1.0} schema.
 */
public class GSSSchema extends XSD {

    public static final String DEFAULT_PREFIX = "gss";

    public static final String NAMESPACE = "http://www.opengis.net/gss/1.0";

    /* Type Definitions */
    public static final QName AbstractTransactionActionType = new QName(NAMESPACE,
            "AbstractTransactionActionType", DEFAULT_PREFIX);

    public static final QName BaseRequestType = new QName(NAMESPACE, "BaseRequestType",
            DEFAULT_PREFIX);

    public static final QName DeleteType = new QName(NAMESPACE, "DeleteType", DEFAULT_PREFIX);

    public static final QName EntryIdType = new QName(NAMESPACE, "EntryIdType", DEFAULT_PREFIX);

    public static final QName EntryResponseType = new QName(NAMESPACE, "EntryResponseType",
            DEFAULT_PREFIX);

    public static final QName GSS_CapabilitiesType = new QName(NAMESPACE, "GSS_CapabilitiesType",
            DEFAULT_PREFIX);

    public static final QName InsertType = new QName(NAMESPACE, "InsertType", DEFAULT_PREFIX);

    public static final QName SubscriptionType = new QName(NAMESPACE, "SubscriptionType",
            DEFAULT_PREFIX);

    public static final QName TopicResponseType = new QName(NAMESPACE, "TopicResponseType",
            DEFAULT_PREFIX);

    public static final QName TopicType = new QName(NAMESPACE, "TopicType", DEFAULT_PREFIX);

    public static final QName TransactionType = new QName(NAMESPACE, "TransactionType",
            DEFAULT_PREFIX);

    public static final QName UpdateType = new QName(NAMESPACE, "UpdateType", DEFAULT_PREFIX);

    public static final QName ActionType = new QName(NAMESPACE, "ActionType", DEFAULT_PREFIX);

    public static final QName ConformanceClassName = new QName(NAMESPACE, "ConformanceClassName",
            DEFAULT_PREFIX);

    public static final QName DateType = new QName(NAMESPACE, "DateType", DEFAULT_PREFIX);

    public static final QName DeliveryMethodType = new QName(NAMESPACE, "DeliveryMethodType",
            DEFAULT_PREFIX);

    public static final QName SubscriptionStatusType = new QName(NAMESPACE,
            "SubscriptionStatusType", DEFAULT_PREFIX);

    /* Element definitions */
    public static final QName AbstractTransactionAction = new QName(NAMESPACE,
            "AbstractTransactionAction", DEFAULT_PREFIX);

    public static final QName AcceptChange = new QName(NAMESPACE, "AcceptChange", DEFAULT_PREFIX);

    public static final QName AcceptChangeResponse = new QName(NAMESPACE, "AcceptChangeResponse",
            DEFAULT_PREFIX);

    public static final QName CancelSubscription = new QName(NAMESPACE, "CancelSubscription",
            DEFAULT_PREFIX);

    public static final QName ConformanceClass = new QName(NAMESPACE, "ConformanceClass",
            DEFAULT_PREFIX);

    public static final QName ConformanceDeclaration = new QName(NAMESPACE,
            "ConformanceDeclaration", DEFAULT_PREFIX);

    public static final QName CreateTopic = new QName(NAMESPACE, "CreateTopic", DEFAULT_PREFIX);

    public static final QName CreateTopicResponse = new QName(NAMESPACE, "CreateTopicResponse",
            DEFAULT_PREFIX);

    public static final QName Delete = new QName(NAMESPACE, "Delete", DEFAULT_PREFIX);

    public static final QName EntryId = new QName(NAMESPACE, "EntryId", DEFAULT_PREFIX);

    public static final QName GetEntries = new QName(NAMESPACE, "GetEntries", DEFAULT_PREFIX);

    public static final QName GSS_Capabilities = new QName(NAMESPACE, "GSS_Capabilities",
            DEFAULT_PREFIX);

    public static final QName Insert = new QName(NAMESPACE, "Insert", DEFAULT_PREFIX);

    public static final QName ListSubscriptions = new QName(NAMESPACE, "ListSubscriptions",
            DEFAULT_PREFIX);

    public static final QName ListSubscriptionsResponse = new QName(NAMESPACE,
            "ListSubscriptionsResponse", DEFAULT_PREFIX);

    public static final QName ListTopics = new QName(NAMESPACE, "ListTopics", DEFAULT_PREFIX);

    public static final QName ListTopicsResponse = new QName(NAMESPACE, "ListTopicsResponse",
            DEFAULT_PREFIX);

    public static final QName PauseSubscription = new QName(NAMESPACE, "PauseSubscription",
            DEFAULT_PREFIX);

    public static final QName PauseSubscriptionResponse = new QName(NAMESPACE,
            "PauseSubscriptionResponse", DEFAULT_PREFIX);

    public static final QName RejectChange = new QName(NAMESPACE, "RejectChange", DEFAULT_PREFIX);

    public static final QName RejectChangeResponse = new QName(NAMESPACE, "RejectChangeResponse",
            DEFAULT_PREFIX);

    public static final QName RemoveTopic = new QName(NAMESPACE, "RemoveTopic", DEFAULT_PREFIX);

    public static final QName RemoveTopicResponse = new QName(NAMESPACE, "RemoveTopicResponse",
            DEFAULT_PREFIX);

    public static final QName ResumeSubscription = new QName(NAMESPACE, "ResumeSubscription",
            DEFAULT_PREFIX);

    public static final QName ResumeSubscriptionResponse = new QName(NAMESPACE,
            "ResumeSubscriptionResponse", DEFAULT_PREFIX);

    public static final QName ReviewedChanges = new QName(NAMESPACE, "ReviewedChanges",
            DEFAULT_PREFIX);

    public static final QName ReviewedChangesResponse = new QName(NAMESPACE,
            "ReviewedChangesResponse", DEFAULT_PREFIX);

    public static final QName Subscribe = new QName(NAMESPACE, "Subscribe", DEFAULT_PREFIX);

    public static final QName SubscribeResponse = new QName(NAMESPACE, "SubscribeResponse",
            DEFAULT_PREFIX);

    public static final QName Subscription = new QName(NAMESPACE, "Subscription", DEFAULT_PREFIX);

    public static final QName Topic = new QName(NAMESPACE, "Topic", DEFAULT_PREFIX);

    public static final QName Transaction = new QName(NAMESPACE, "Transaction", DEFAULT_PREFIX);

    public static final QName TransactionResponse = new QName(NAMESPACE, "TransactionResponse",
            DEFAULT_PREFIX);

    public static final QName Update = new QName(NAMESPACE, "Update", DEFAULT_PREFIX);

    /** wfs dependency */
    private WFS wfs;

    public GSSSchema(final WFS wfs) {
        this.wfs = wfs;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void addDependencies(Set dependencies) {
        dependencies.add(wfs);
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    @Override
    public String getSchemaLocation() {
        return getClass().getResource("schemas/gss/1.0.0/gss.xsd").toString();
    }

    @Override
    protected XSDSchema buildSchema() throws IOException {
        XSDSchema gssSchema = super.buildSchema();
        gssSchema = wfs.getSchemaBuilder().addApplicationTypes(gssSchema);
        return gssSchema;
    }
}
