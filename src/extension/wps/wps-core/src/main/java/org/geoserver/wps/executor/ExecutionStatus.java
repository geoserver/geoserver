/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.Serializable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.opengis.wps10.ExecuteType;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Summarizes the execution state of a certain process. Note: the class implements equals and
 * hashcode, but skips the exception in them, as common Java exceptions do not sport a usable
 * equals/hashcode implementation, and the exceptions might be cloned to due network/database
 * serialization.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ExecutionStatus implements Serializable, Comparable<ExecutionStatus> {

    static final Logger LOGGER = Logging.getLogger(ExecutionStatus.class);

    private static final long serialVersionUID = -2433524030271115410L;

    // TODO: find a GeoServer unified, non GUI specific way to get the node identifier
    public static final String NODE_IDENTIFIER = getNodeIdentifier();

    private static String getNodeIdentifier() {
        try {
            return getLocalAddress().getHostName();
        } catch (Exception e) {
            return null;
        }
    }

    private static InetAddress getLocalAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
                    interfaces.hasMoreElements(); ) {
                NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
                if (ni.getName() != null && ni.getName().startsWith("vmnet")) {
                    // skipping vmware interfaces
                    continue;
                }
                // each interface can have more than one address
                for (Enumeration inetAddrs = ni.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    // we are not interested in loopback
                    if (!inetAddr.isLoopbackAddress() && !(inetAddr instanceof Inet6Address)) {
                        if (inetAddr.isSiteLocalAddress()) {
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // Fall back to whatever localhost provides
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException(
                        "The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException =
                    new UnknownHostException("Failed to determine LAN address");
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    /** The process being executed */
    Name processName;

    /** The execution id, can be used to retrieve the process results */
    String executionId;

    /** If the request was asynchronous, or not */
    boolean asynchronous;

    /** Current execution status */
    ProcessState phase;

    /** Process execution status (as a percentage between 0 and 100) */
    float progress;

    /** The name of the user that requested the process */
    String userName;

    /** Request creation time */
    Date creationTime;

    /** Request completion time */
    Date completionTime = null;

    /** A heartbeat field, used when clustering nodes */
    Date lastUpdated;

    /** Date and time by wich the processing job will be no longer accessible */
    Date expirationDate = null;

    /** Date and time by wich the processing job will be finished */
    Date estimatedCompletion = null;

    /** Date and time for the next suggested status polling */
    Date nextPoll = null;

    /** What is the process currently working on */
    String task;

    /** The process failure */
    Throwable exception;

    /**
     * The original request. This is a transient field will be available only inside the node that
     * originated the request, and only during its execution
     */
    transient ExecuteType request;

    /** Node identifier */
    String nodeId;

    public ExecutionStatus(Name processName, String executionId, boolean asynchronous) {
        this.processName = processName;
        this.executionId = executionId;
        setPhase(ProcessState.QUEUED);
        this.creationTime = new Date();
        this.lastUpdated = this.creationTime;
        this.asynchronous = asynchronous;

        // grab the user name that made the request
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            this.userName = authentication.getName();
        }

        // grab the node id
        this.nodeId = NODE_IDENTIFIER;
    }

    public ExecutionStatus(ExecutionStatus other) {
        this.processName = other.processName;
        this.executionId = other.executionId;
        setPhase(other.phase);
        this.progress = other.progress;
        this.task = other.task;
        this.exception = other.exception;
        this.creationTime = other.creationTime;
        this.completionTime = other.completionTime;
        this.request = other.request;
        this.asynchronous = other.asynchronous;
        this.userName = other.userName;
        this.nodeId = other.nodeId;
        this.lastUpdated = other.lastUpdated;
        this.expirationDate = other.expirationDate;
        this.estimatedCompletion = other.estimatedCompletion;
        this.nextPoll = other.nextPoll;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
        setPhase(ProcessState.FAILED);
    }

    public Name getProcessName() {
        return processName;
    }

    public String getSimpleProcessName() {
        return processName.toString();
    }

    public String getExecutionId() {
        return executionId;
    }

    public ProcessState getPhase() {
        return phase;
    }

    /** Returns the progress percentage, as a number between 0 and 100 */
    public float getProgress() {
        return progress;
    }

    public void setProcessName(Name processName) {
        this.processName = processName;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public void setPhase(ProcessState phase) {
        this.phase = phase;
        if (phase != null
                && phase.isExecutionCompleted()
                // if there is already a completionTime don't overwrite it!
                && this.completionTime == null) {
            this.completionTime = new Date();
        }
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getTask() {
        return task;
    }

    public Throwable getException() {
        return exception;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * The original request. This field is available only while the request is being processed, on
     * the node that's processing it. For all other nodes, a copy of the request is stored on disk
     */
    public ExecuteType getRequest() {
        return request;
    }

    public void setRequest(ExecuteType request) {
        this.request = request;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Date completionTime) {
        this.completionTime = completionTime;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public String getNodeId() {
        return nodeId;
    }

    /** Last time this bean has been updated */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /** Sets the last updated time. Only the {@link ProcessStatusTracker} should call this method */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /** @return the expirationDate */
    public Date getExpirationDate() {
        return expirationDate;
    }

    /** @param expirationDate the expirationDate to set */
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    /** @return the estimatedCompletion */
    public Date getEstimatedCompletion() {
        return estimatedCompletion;
    }

    /** @param estimatedCompletion the estimatedCompletion to set */
    public void setEstimatedCompletion(Date estimatedCompletion) {
        this.estimatedCompletion = estimatedCompletion;
    }

    /** @return the nextPoll */
    public Date getNextPoll() {
        return nextPoll;
    }

    /** @param nextPoll the nextPoll to set */
    public void setNextPoll(Date nextPoll) {
        this.nextPoll = nextPoll;
    }

    @Override
    public String toString() {
        return "ExecutionStatus [processName="
                + processName
                + ", executionId="
                + executionId
                + ", asynchronous="
                + asynchronous
                + ", phase="
                + phase
                + ", progress="
                + progress
                + ", userName="
                + userName
                + ", creationTime="
                + creationTime
                + ", completionTime="
                + completionTime
                + ", lastUpdated="
                + lastUpdated
                + ", expirationDate="
                + expirationDate
                + ", estimatedCompletion="
                + estimatedCompletion
                + ", nextPoll="
                + nextPoll
                + ", task="
                + task
                + ", exception="
                + exception
                + ", nodeId="
                + nodeId
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (asynchronous ? 1231 : 1237);
        result = prime * result + ((completionTime == null) ? 0 : completionTime.hashCode());
        result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((executionId == null) ? 0 : executionId.hashCode());
        result = prime * result + ((lastUpdated == null) ? 0 : lastUpdated.hashCode());
        result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
        result =
                prime * result
                        + ((estimatedCompletion == null) ? 0 : estimatedCompletion.hashCode());
        result = prime * result + ((nextPoll == null) ? 0 : nextPoll.hashCode());
        result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
        result = prime * result + ((phase == null) ? 0 : phase.hashCode());
        result = prime * result + ((processName == null) ? 0 : processName.hashCode());
        result = prime * result + Float.floatToIntBits(progress);
        result = prime * result + ((task == null) ? 0 : task.hashCode());
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ExecutionStatus other = (ExecutionStatus) obj;
        if (asynchronous != other.asynchronous) return false;
        if (completionTime == null) {
            if (other.completionTime != null) {
                return false;
            }
        } else if (!completionTime.equals(other.completionTime)) return false;
        if (creationTime == null) {
            if (other.creationTime != null) return false;
        } else if (!creationTime.equals(other.creationTime)) return false;
        if (executionId == null) {
            if (other.executionId != null) return false;
        } else if (!executionId.equals(other.executionId)) return false;
        if (lastUpdated == null) {
            if (other.lastUpdated != null) return false;
        } else if (!lastUpdated.equals(other.lastUpdated)) return false;
        if (expirationDate == null) {
            if (other.expirationDate != null) return false;
        } else if (!expirationDate.equals(other.expirationDate)) return false;
        if (estimatedCompletion == null) {
            if (other.estimatedCompletion != null) return false;
        } else if (!estimatedCompletion.equals(other.estimatedCompletion)) return false;
        if (nextPoll == null) {
            if (other.nextPoll != null) return false;
        } else if (!nextPoll.equals(other.nextPoll)) return false;
        if (nodeId == null) {
            if (other.nodeId != null) return false;
        } else if (!nodeId.equals(other.nodeId)) return false;
        if (phase != other.phase) return false;
        if (processName == null) {
            if (other.processName != null) return false;
        } else if (!processName.equals(other.processName)) return false;
        if (Float.floatToIntBits(progress) != Float.floatToIntBits(other.progress)) return false;
        if (task == null) {
            if (other.task != null) return false;
        } else if (!task.equals(other.task)) return false;
        if (userName == null) {
            if (other.userName != null) return false;
        } else if (!userName.equals(other.userName)) return false;
        return true;
    }

    @Override
    public int compareTo(ExecutionStatus o) {
        return executionId.compareTo(o.executionId);
    }
}
