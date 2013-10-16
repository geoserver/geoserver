/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class JMSEventHelper implements NotificationPublisher {
    private static final Log LOG = LogFactory.getLog(JMSEventHelper.class);
    private static final long DROPPED_WARN_MS = 20000;
    private static final long SESSION_FAIL_MS = 5000;
    
    public static class JMSInfo {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        public final Connection connection;
        public final List<Destination> destinations;
        public boolean dead;
        public boolean closed;

        // Pooled implementation caused long delays when server disconnected
//        ObjectPool sessionPool = new SoftReferenceObjectPool(new BasePoolableObjectFactory() {
//            @Override
//            public Object makeObject() throws Exception {
//                return connection.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);
//            }
//
//            public void destroyObject(Object obj) throws Exception {
//                try {
//                    ((Session) obj).close();
//                } catch(Throwable t) {
//                    // ignore
//                }
//            };
//        });
        
        public void lock() {
            lock.readLock().lock();
        }
        
        public void unlock() {
            lock.readLock().unlock();
            if(dead)
                close();
        }
        
        /**
         * Close the current {@link Connection}. Must be called with no read lock on the current thread!
         */
        public void close() {
            if(closed)
                return;
            
            try {
                if(!lock.writeLock().tryLock(30, TimeUnit.MILLISECONDS)) {
                    LOG.warn("Waited 30 seconds trying to close JMS connection! Abandoning!");
                    return;
                }
            } catch(InterruptedException e1) {
                LOG.warn("Interrupted before write lock obtained while trying to close JMS session. Leaving JMS session open.");
                return;
            }

            if(closed)
                return;
            
            try {
                connection.close();
                closed = true;
            } catch(JMSException e) {
                LOG.warn("Error closing JMS connection " + connection, e);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        /**
         * Get a JMS {@link Session}. If this is not possible, call {@link #close} to discard
         * this {@link JMSEventHelper.JMSInfo}.
         * 
         * @return a {@link Session} or {@code null}.
         */
        public Session getSession() {
            try {
                return connection.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            } catch(Exception e) {
                dead = true;
                LOG.warn("Error creating JMS session", e);
                return null;
            }
        }
        
        public void returnSession(Session session) {
            try {
                session.close();
            } catch(Exception e) {
                dead = true;
                LOG.warn("Error closing JMS session " + session, e);
            }
        }
        
        public void invalidateSession(Session session) {
            returnSession(session); // Essentially a noop
        }
        
        public JMSInfo(Connection connection, List<Destination> destinations) {
            this.connection = connection;
            this.destinations = destinations;
        }
    }
    
    AtomicInteger dropped = new AtomicInteger(0);
    AtomicInteger failed = new AtomicInteger(0);
    AtomicReference<Throwable> lastFailure = new AtomicReference<Throwable>();
    boolean sessionFailed = false;
    AtomicLong lastSessionFailed = new AtomicLong(0);
    AtomicLong lastWarnDropped = new AtomicLong(0);
    
    protected abstract JMSInfo getJMSInfo();
    
    @Override
    public void publish(String byteString) {
        JMSInfo info; // Guard against reference changing without synchronizing
        
        // Request JMS sessions until we find one that isn't closed 
        while((info = getJMSInfo()) != null) {
            info.lock();
            try {
                if(info.closed)
                   continue;
                if(fireNotification(byteString, info))
                    return;
                if(info.closed)
                    continue;
            } finally {
                info.unlock();
            }
        }
        
        // If we made it here, no JMSInfo was available
        dropped.incrementAndGet();
        if(lastWarnDropped.get() + DROPPED_WARN_MS < System.currentTimeMillis()) {
            lastWarnDropped.set(System.currentTimeMillis());
            LOG.warn("No session available; messages will be dropped. " + dropped.get() + " have been dropped so far.");
        }
    }
    
    /*
     * Try to fire a notification and return true if no retry is necessary.
     */
    private boolean fireNotification(String byteString, JMSInfo info) {
        Session session = null;
        for(int i = 0; i < 5 && !info.closed; ++i) {
            session = info.getSession();
            
            if(session == null) {
                continue;
            }
            
            try {
                MessageProducer producer = session.createProducer(null);
                Message message = session.createTextMessage(byteString);
                for(Destination d : info.destinations) {
                    producer.send(d, message); 
                }
                return true;
            } catch(Exception e) {
                recordFailure(e);
                info.invalidateSession(session);
                session = null; // Don't try to return the session
                continue;
            } finally {
                try {
                    if(session != null) {
                        info.returnSession(session);
                    }
                } catch(Exception e) {
                    // ignore
                }
            }
        }
        
        // Ran out of retries
        
        return false;
    }

    protected void recordFailure(Throwable e) {
        dropped.incrementAndGet();
        failed.incrementAndGet();
        if(lastSessionFailed.get() + SESSION_FAIL_MS < System.currentTimeMillis()) {
            lastSessionFailed.set(System.currentTimeMillis());
            lastFailure.set(e);
            LOG.warn("Sending message failed. " + dropped.get() + " have been dropped so far.", e);
        }
    }
}
