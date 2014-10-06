/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.hibernate;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;

public class AbstractHibFacade {

    /**
     * logging instance
     */
    protected final Logger LOGGER = Logging.getLogger("org.geoserver.hibernate");
    
    
    protected SessionFactory sessionFactory;
    
    
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
    /** Simple wrapper to tell which objects are bindable query parameters */
    protected static class QueryParam {
        Object param;

        public QueryParam(Object param) {
            this.param = param;
        }
    }
    
    protected static QueryParam param(Object param) {
        return new QueryParam(param);
    }

    protected Query query(Object... elems) {
        final StringBuilder builder = new StringBuilder();
        int cnt = 0;
        for (Object elem : elems) {
            if (elem instanceof String) {
                builder.append(elem);
            }
            else if (elem instanceof Class) {
                Class clazz = (Class) elem;
                ClassMappings map = ClassMappings.fromInterface(clazz); 
                if (map != null) {
                    clazz = map.getImpl();
                }
                
                builder.append(clazz.getSimpleName());
            } 
            else if (elem instanceof QueryParam) {
                builder.append(":param").append(cnt++);
            }
        }
    
        Query query = sessionFactory.getCurrentSession().createQuery(builder.toString());
        query.setCacheable(true);
        
        cnt = 0;
        
        for (Object elem : elems) {
            if (elem instanceof QueryParam) {
                query.setParameter("param" + (cnt++), ((QueryParam) elem).param);
            }
        }
    
        return query;
    }

    protected Object first(final Query query) {
        return first(query, true);
    }

    protected Object first(final Query query, boolean doWarn) {
        query.setMaxResults(doWarn ? 2 : 1);
        query.setCacheable(true);
        
        List<?> result = query.list();
        if (result.isEmpty()) {
            return null;
        } 
        else {
            //TODO: add a flag to control exception
            if (result.size() > 1) {
                throw new RuntimeException("Expected 1 result from " + query + " but got " + result.size());
                
            }
//            if (doWarn && result.size() > 1) {
//                LOGGER.log(Level.WARNING, "Found too many items in result", new RuntimeException(
//                        "Trace: Found too many items in query"));
//            }

            Object ret = result.get(0);
            if (ret instanceof HibernateProxy) {
                HibernateProxy proxy = (HibernateProxy) ret;
                ret = proxy.getHibernateLazyInitializer().getImplementation();
            }

            if (LOGGER.isLoggable(Level.FINE)){
                StringBuilder callerChain = new StringBuilder();
                for (StackTraceElement stackTraceElement : new Throwable().getStackTrace()) {
                    if ("first".equals(stackTraceElement.getMethodName()))
                        continue;
                    String cname = stackTraceElement.getClassName();
                    if (cname.startsWith("org.spring"))
                        continue;
                    cname = cname.substring(cname.lastIndexOf(".") + 1);
                    callerChain.append(cname).append('.').append(stackTraceElement.getMethodName())
                            .append(':').append(stackTraceElement.getLineNumber()).append(' ');
                    // if(++num==10) break;
                }               
                LOGGER.fine("FIRST -->" + ret.getClass().getSimpleName() + " --- " + ret + " { "
                        + callerChain + "}");
            }
            return ret;
        }
    }

    protected <T> List<T> list(Class<T> clazz) {
        Query query = query("from ", clazz);
        query.setCacheable(true);
        
        List<?> result = query.list();
        return Collections.unmodifiableList((List<T>) result);
    }
    
    protected <T extends Info> T persist(T entity) {
        return persist(entity, true);
    }
    
    protected <T extends Info> T persist(T entity, boolean clearId) {
        if (clearId) {
            //hack, clear out id if we are adding a new object
            Object id = OwsUtils.get(entity, "id");
            if (id != null) {
                OwsUtils.set(entity, "id", null);
            }
        }
        try {
            MetadataMap md = (MetadataMap) OwsUtils.get(entity, "metadata");
            if (md != null) {
                md.setId(null);
            }
        }
        catch(IllegalArgumentException e1) {}
        catch(ClassCastException e2) {}
        
        sessionFactory.getCurrentSession().persist(entity);
        return entity;
    }
    
    protected <T extends Info> T merge(T entity) {
        return (T) sessionFactory.getCurrentSession().merge(entity);
    }
    
    protected void delete(Info entity) {
        Session session = sessionFactory.getCurrentSession();
        Info attached = (Info) session.merge(entity);
        session.delete(attached);
    }

}
