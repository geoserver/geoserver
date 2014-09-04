/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.wfs.WFSException;
import org.geotools.gml3.GML;
import org.geotools.math.Statistics;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.Identifier;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.NamespaceSupport;

public class GMLNotificationSerializer implements PublishCallbackMBean, CatalogListener, NotificationSerializer {
    private static final Log LOG = LogFactory.getLog(GMLNotificationSerializer.class);
    private static final SAXTransformerFactory STF = (SAXTransformerFactory) TransformerFactory.newInstance();
    private static final boolean isDebug = Boolean.parseBoolean(System.getProperty("com.fsi.c2rpc.geoserver.wsn.PublishCallback.debug"));

    private final Catalog catalog;
    private final Configuration xmlConfig;
    private AtomicLong catalogModCount = new AtomicLong(0);
    
    private final ThreadLocal<Long> catalogModCountLocal = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return 0L;
        }
    };
    
    private final ThreadLocal<Encoder> encoder = new ThreadLocal<Encoder>() {
        @Override
        protected Encoder initialValue() {
            try {
                // TODO: Get this patch into GeoTools?
                Encoder enc = new Encoder(xmlConfig, xmlConfig.getXSD().getSchema()) /* {
                    {
                        // setResuseIndex(true);
                    }
                    protected void resetContext(MutablePicoContainer context) {
                        context.unregisterComponent(XSDIdRegistry.class);
                        context.registerComponentInstance(new XSDIdRegistry());
                    }
                    
                    protected void clearContext(MutablePicoContainer context) {
                    };
                };
                enc.setResuseIndex(true) */;
                return enc;
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
    };
    
    // Reporting stuff
    private ConcurrentHashMap<Name, Statistics> serializationMillisByType;
    private Statistics serializationMillis;
    private Statistics serializationChars;
    private AtomicLong failures;
    private AtomicLong updates;
    private AtomicLong deletes;
    private Throwable lastFailure;
    private ObjectName name;

    public GMLNotificationSerializer(Catalog catalog, Configuration xmlConfig) {
        this.catalog = catalog;
        this.xmlConfig = xmlConfig;
        resetStats();
    }
    
    public void setObjectName(String name) {
        try {
            this.name = ObjectName.getInstance(name);
        } catch (MalformedObjectNameException e) {
            LOG.warn("Invalid ObjectName:", e);
        }
    }
    
    public void init() {
        registerMBean();
    }
    
    public void destroy() {
        unregisterMBean();
    }
    
    private final void registerMBean() {
        if(name == null) {
            return;
        }
        
        for(MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
            try {
                server.registerMBean(this, name);
            } catch (JMException e) {
                LOG.warn("Unable to register in JMX:", e);
            }
        }
    }
    
    private final void unregisterMBean() {
        if(name == null)
            return;
        
        for(MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
            try {
                server.unregisterMBean(name);
            } catch (JMException e) {
                LOG.warn("Unable to unregister from JMX:", e);
            }
        }
    }

    public String getDeleteRawMessage(QName typeName, Identifier id) {
        StringBuilder builder = new StringBuilder(1024);
        builder.append(
            "<wfs:Delete xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:")
            .append(typeName.getPrefix()).append("=\"").append(
                typeName.getNamespaceURI()).append("\" typeName=\"").append(
                typeName.getPrefix()).append(":").append(
                typeName.getLocalPart()).append("\">").append(
                id.getID()).append("</wfs:Delete>");

        return builder.toString();
    }

    /*
     * Just borrowing a couple of methods from GeoServer...
     */
    private static void loadNamespaceBindings(NamespaceSupport nss, Feature f, XSDSchema exclude) {
        XSDTypeDefinition type =
            (XSDTypeDefinition) f.getType().getUserData().get(
                XSDTypeDefinition.class);
        if(type == null) return;
        loadNamespaceBindings(nss, type.getSchema(), exclude);
    }

    private static void loadNamespaceBindings(NamespaceSupport nss, XSDSchema schema, XSDSchema exclude) {
        Map excludePrefixes = exclude.getQNamePrefixToNamespaceMap();
        for(Map.Entry<String, String> e : ((Map<String,String>)schema.getQNamePrefixToNamespaceMap()).entrySet()) {
            if(excludePrefixes.containsKey(e.getKey()) || excludePrefixes.containsValue(e.getValue())) {
                continue;
            }
            String pref = e.getKey();
            nss.declarePrefix(pref == null ? "" : pref, e.getValue());
        }
    }
    String getInsertUpdateRawMessage(Feature feature) {
        final StringBuilderWriter sw = new StringBuilderWriter();
        long start = System.nanoTime();
        boolean success = true;
        try {
            long modCount = catalogModCount.get();
            if(modCount != catalogModCountLocal.get().longValue()) {
                catalogModCountLocal.set(modCount);
                encoder.remove();
            }
            
            final Encoder encoder = this.encoder.get();

            TransformerHandler handler = STF.newTransformerHandler();
            handler.setResult(new StreamResult(sw));

            encoder.setInline(true);
            loadNamespaceBindings(encoder.getNamespaces(), feature, xmlConfig.getXSD().getSchema());
            
            encoder.encode(feature, GML._Feature, handler);

            return sw.toString();
        } catch(Exception e) {
            success = false;
            throw new WFSException(e);
        } finally {
            if(success) {
                double millis = ((double) System.nanoTime() - start) / 1000000;
                Name name = WFSNotify.getTypeName(feature);
                serializationMillis.add(millis);
                serializationChars.add(sw.getBuilder().length());
                Statistics statsByType = serializationMillisByType.get(name);
                if(statsByType == null) {
                    Statistics newStats = new Statistics();
                    statsByType = serializationMillisByType.put(name, newStats);
                    if(statsByType == null) {
                        statsByType = newStats;
                    }
                }
                statsByType.add(millis);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.fsi.geoserver.wfs.NotificationSerializer#serializeInsertOrUpdate(org.opengis.feature.Feature)
     */
    public String serializeInsertOrUpdate(Feature f) {
        try {
            String rawMessage;
            rawMessage = getInsertUpdateRawMessage(f);
            debugMessage(rawMessage);
            
            return rawMessage;
        } catch(Throwable t) {
            onFailure(t);
        } finally {
            updates.incrementAndGet();
        }
        return null;
    }

    public void debugMessage(String rawMessage) throws TransformerFactoryConfigurationError {
        if(isDebug) {
            LOG.debug(rawMessage);
            try {
                TransformerFactory.newInstance().newTransformer()
                    .transform(new StreamSource(new StringReader(rawMessage)), new SAXResult(new DefaultHandler()));
            } catch(TransformerException e) {
                LOG.error("Error parsing result", e);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.fsi.geoserver.wfs.NotificationSerializer#serializeDelete(org.opengis.feature.type.Name, org.opengis.filter.identity.Identifier)
     */
    public String serializeDelete(Name typeName, Identifier id) {
        try {
            QName qname = nameToQName(typeName);
            String rawMessage = getDeleteRawMessage(qname, id);
            debugMessage(rawMessage);
            return rawMessage;
        } catch(Throwable t) {
            onFailure(t);
        } finally {
            deletes.incrementAndGet();
        }
        return null;
    }

    protected void onFailure(Throwable t) {
        if(LOG.isWarnEnabled()) {
            LOG.warn("Unexpected exception while notifying.", t);
        }
        failures.incrementAndGet();
        lastFailure = t;
    }

    public QName nameToQName(Name typeName) {
        QName qname =
            new QName(typeName.getNamespaceURI(), typeName.getLocalPart(), catalog
                .getNamespaceByURI(typeName.getNamespaceURI()).getPrefix());
        return qname;
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        catalogModCount.incrementAndGet();
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        catalogModCount.incrementAndGet();        
    }

    @Override
    public void reloaded() {
        catalogModCount.incrementAndGet();
    }

    @Override
    public void resetStats() {
        serializationMillisByType = new ConcurrentHashMap<Name, Statistics>();
        serializationMillis = new Statistics();
        serializationChars = new Statistics();
        failures = new AtomicLong();
        updates = new AtomicLong();
        deletes = new AtomicLong();
        lastFailure = null;
    }

    @Override
    public long getUpdates() {
        return updates.get();
    }

    @Override
    public long getDeletes() {
        return deletes.get();
    }

    @Override
    public double getAverageSerializationTime() {
        return serializationMillis.mean();
    }

    @Override
    public double getMinimumSerializationTime() {
        return serializationMillis.minimum();
    }

    @Override
    public double getMaximumSerializationTime() {
        return serializationMillis.maximum();
    }
    
    @Override
    public double getTotalSerializationTime() {
        return serializationMillis.mean() * serializationMillis.count();
    }

    @Override
    public long getAverageMessageSize() {
        return (long)serializationChars.mean();
    }

    @Override
    public long getMinimumMessageSize() {
        return (long)serializationChars.minimum();
    }

    @Override
    public long getMaximumMessageSize() {
        return (long)serializationChars.maximum();
    }

    @Override
    public long getTotalMessageSize() {
        return (long)(serializationChars.mean() * serializationChars.count());
    }

    @Override
    public String createSerializationTimeHistogram() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><thead><tr><td>Type name</td><td>Count</td><td>Min</td><td>Max</td><td>Avg</td><td>Total</td></tr></thead>");
        for(Entry<Name, Statistics> e : serializationMillisByType.entrySet()) {
            Statistics st = e.getValue();
            sb.append("<tr><td>").append(e.getKey()).append("</td><td>").append(st.count()).append("</td><td>")
                .append(st.minimum()).append("</td><td>").append(st.maximum()).append("</td><td>").append(st.mean())
                .append("</td><td>").append(st.mean()*st.count()).append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }
    
    @Override
    public long getFailures() {
        return failures.get();
    }

    @Override
    public Throwable getLastFailure() {
        return lastFailure;
    }
}
