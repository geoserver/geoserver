/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.hibernate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.event.EventListeners;
import org.hibernate.event.PostLoadEventListener;

/**
 * Custom HibernatePersistence implementation that allows for setting an intercetor that is 
 * a spring injected bean.
 * <p>
 * Taken from http://blog.krecan.net/tag/interceptor/.
 * </p>
 *
 */
public class ConfigurableHibernatePersistence extends HibernatePersistence {
    private Interceptor interceptor;
    private List<PostLoadEventListener> postLoadEventListeners;

    public Interceptor getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }
    
    public void setPostLoadEventListeners(List<PostLoadEventListener> postLoadEventListeners) {
        this.postLoadEventListeners = postLoadEventListeners;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info,
            Map map) {
        Ejb3Configuration cfg = new Ejb3Configuration();
        Ejb3Configuration configured = cfg.configure(info, map);
        postprocessConfiguration(info, map, configured);
        return configured != null ? configured.buildEntityManagerFactory() : null;
    }

    @SuppressWarnings("unchecked")
    protected void postprocessConfiguration(PersistenceUnitInfo info, Map map,
            Ejb3Configuration configured) {
        if (this.interceptor != null) {
            if (configured.getInterceptor() == null
                    || EmptyInterceptor.class.equals(configured.getInterceptor().getClass())) {
                configured.setInterceptor(this.interceptor);
            } else {
                throw new IllegalStateException(
                    "Hibernate interceptor already set in persistence.xml ("
                                + configured.getInterceptor() + ")");
            }
        }
        if (this.postLoadEventListeners != null) {
            EventListeners el = configured.getEventListeners();
            List<PostLoadEventListener> listeners = new LinkedList(Arrays.asList(el.getPostLoadEventListeners()));
            listeners.addAll(this.postLoadEventListeners);
            el.setPostLoadEventListeners(listeners.toArray(new PostLoadEventListener[listeners.size()]));
        }
    }
}
