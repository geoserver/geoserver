/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.jdbc.JDBCFeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class TriggerManager {
    private static final Log logger = LogFactory.getLog(WFSNotify.class);

    public interface TriggerCallback {
        public void triggerEvent(Feature f);
    }
    
    private static class FilterBuilder {
        List<Filter> branches = new ArrayList<Filter>();
        
        public FilterBuilder() {
        }
        
        public void addFeature(Feature f, Link l) {
            Property property = f.getProperty(new NameImpl(l.getKey()));
            if (property == null || property.getValue() == null)
                return;
            branches.add(FF.equals(new AttributeExpressionImpl(new NameImpl(l.getForeign())), FF.literal(property.getValue())));
        }
        
        public Filter build() {
            try {
                if(branches.isEmpty())
                    return null;
                else if(branches.size() == 1)
                    return branches.get(0);
                else
                    return FF.or(branches);
            } finally {
                branches = new ArrayList<Filter>();
            }
        }
    }

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory(null);

    final Catalog catalog;
    final TriggerFileWatcher tfw;

    public TriggerManager(Catalog c, TriggerFileWatcher t) {
        catalog = c;
        tfw = t;
    }

    void triggerEvent(FeatureIterator<? extends Feature> affected, QName name, TriggerCallback cb, Transaction transaction)
        throws IOException {

        if(logger.isTraceEnabled()) {
            logger.trace("Triggering on features of type " + name);
        }

        List<Trigger> triggers = tfw.load().get(name);
        if(triggers == null) return;
        
        FilterBuilder[] subFilters = new FilterBuilder[triggers.size()];
        for(int i = 0; i <  subFilters.length; ++i) {
            subFilters[i] = new FilterBuilder();
        }
        
        while(affected.hasNext()) {
            Feature f = affected.next();
            for(int i = 0; i < subFilters.length; ++i)
                subFilters[i].addFeature(f, triggers.get(i).getLink().get(0));
        }
        
        for(int i = 0; i < subFilters.length; ++i) {
            triggerEvent(triggers.get(i), subFilters[i].build(), cb, transaction);
        }
    }

    @SuppressWarnings("unchecked")
    void triggerEvent(Trigger t, Filter current, TriggerCallback cb, Transaction transaction)
        throws IOException {

        for(int i = 0; i < t.getLink().size(); ++i) {
            if(current == null) // No more features
                break;
            
            Link l = t.getLink().get(i);
            Link next = i + 1 == t.getLink().size() ? null : t.getLink().get(i+1);
            
            // Get the next feature from the data store
            FeatureTypeInfo info =
                catalog.getFeatureTypeByName(l.getDest().getNamespaceURI(), l.getDest().getLocalPart());

            if(info == null) {
                logger.debug("No such feature type: " + l.getDest());
                return;
            }

            FilterBuilder[] subFilters = new FilterBuilder[l.getTrigger().size()];
            FilterBuilder nextFilter = next == null ? null : new FilterBuilder();
            
            for(int j = 0; j < l.getTrigger().size(); ++j) {
                subFilters[j] = new FilterBuilder();
            }
            
            if(logger.isTraceEnabled()) {
                logger.trace("Triggering with filter " + current + " to type " + info.getQualifiedName());
            }

            FeatureSource<? extends FeatureType, ? extends Feature> featureSource = info.getFeatureSource(null, null);
            if(featureSource instanceof ContentFeatureSource) {
                ((JDBCFeatureSource) featureSource).setTransaction(transaction);
            } else if(featureSource instanceof FeatureStore) {
                ((FeatureStore) featureSource).setTransaction(transaction);
            }
            FeatureCollection<? extends FeatureType, ? extends Feature> features = featureSource.getFeatures(current);
            FeatureIterator<? extends Feature> it = features.features();

            try {
                while(it.hasNext()) {
                    Feature f = it.next();
                    
                    // Build subfilters/next filter
                    for(int j = 0; j < l.getTrigger().size(); ++j) {
                        subFilters[j].addFeature(f, l.getTrigger().get(j).getLink().get(0));
                    }

                    if(next != null) {
                        nextFilter.addFeature(f, next);
                    } else if (!t.isSilent()) {
                        // This is the last link in the trigger and trigger is not silent
                        // Start triggering events
                        cb.triggerEvent(f);
                    }
                }
            } finally {
                it.close();
            }

            for(int j = 0; j < l.getTrigger().size(); ++j) {
                triggerEvent(l.getTrigger().get(j), subFilters[j].build(), cb, transaction);
            }

            if(next != null) {
                current = nextFilter.build();
            }
        }
    }

}
