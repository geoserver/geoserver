package org.geoserver.data.gss;

import org.geoserver.gss.internal.storage.DeliveryMethodType;
import org.geoserver.gss.internal.storage.SubscriptionStatus;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Information about the subscription to a GSS server maintained by the GSS client
 * 
 */
/**
 * @author groldan
 * 
 */
@Entity
public class ServerSubscription {

    @PrimaryKey
    private String sid;

    private String topicId;

    private DeliveryMethodType delivery;

    private SubscriptionStatus status;

    private String url;

    private String user;

    private String password;

    private Boolean usePassiveReplication;

    private Boolean useActiveReplication;

    private Integer activeReplPollIntervalSecs;

    private String namespace;

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

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsePassiveReplication(Boolean usePassiveReplication) {
        this.usePassiveReplication = usePassiveReplication;
    }

    public void setUseActiveReplication(Boolean useActiveReplication) {
        this.useActiveReplication = useActiveReplication;
    }

    public void setActiveReplicationPollIntervalSecs(Integer activeReplPollIntervalSecs) {
        this.activeReplPollIntervalSecs = activeReplPollIntervalSecs;
    }

    /**
     * @return the activeReplPollIntervalSecs
     */
    public Integer getActiveReplPollIntervalSecs() {
        return activeReplPollIntervalSecs;
    }

    /**
     * @param activeReplPollIntervalSecs
     *            the activeReplPollIntervalSecs to set
     */
    public void setActiveReplPollIntervalSecs(Integer activeReplPollIntervalSecs) {
        this.activeReplPollIntervalSecs = activeReplPollIntervalSecs;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the usePassiveReplication
     */
    public Boolean getUsePassiveReplication() {
        return usePassiveReplication;
    }

    /**
     * @return the useActiveReplication
     */
    public Boolean getUseActiveReplication() {
        return useActiveReplication;
    }

    public void setReplicatedNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getReplicatedNamespace() {
        return namespace;
    }
}
