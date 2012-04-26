package org.geoserver.gss.internal.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Information about a GSS client subscription maintained by the GSS server
 * 
 */
@Entity
public class Subscription {

    @PrimaryKey
    private String sid;

    private String topicId;

    private DeliveryMethodType delivery;

    private SubscriptionStatus status;

    private String url;

    /**
     * @return the subscription id
     */
    public String getSid() {
        return sid;
    }

    /**
     * @param uuid
     *            subscription id
     */
    public void setSid(String uuid) {
        this.sid = uuid;
    }

    /**
     * @return the topicId
     */
    public String getTopicId() {
        return topicId;
    }

    /**
     * @param topicId
     *            the topicId to set
     */
    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    /**
     * @return the delivery
     */
    public DeliveryMethodType getDelivery() {
        return delivery;
    }

    /**
     * @param delivery
     *            the delivery to set
     */
    public void setDelivery(DeliveryMethodType delivery) {
        this.delivery = delivery;
    }

    /**
     * @return the status
     */
    public SubscriptionStatus getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url
     *            the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

}
