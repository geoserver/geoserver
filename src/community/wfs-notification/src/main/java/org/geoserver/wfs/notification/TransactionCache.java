package org.geoserver.wfs.notification;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class TransactionCache {
    private static final Field CONTENTFEATURECOLLECTION_FEATURESOURCE;
    private static final Field DECORATINGSIMPLEFEATURECOLLECTION_DELEGATE;
    private static final Field DECORATINGFEATURECOLLECTION_DELEGATE;
    
    static {
        Field field1 = null;
        Field field2 = null;
        Field field3 = null;
        try {
            field1 = ContentFeatureCollection.class.getDeclaredField("featureSource");
            field1.setAccessible(true);
            field2 = DecoratingSimpleFeatureCollection.class.getDeclaredField("delegate");
            field2.setAccessible(true);
            field3 = DecoratingFeatureCollection.class.getDeclaredField("delegate");
            field3.setAccessible(true);
        } catch(SecurityException e) {
            // ignore
        } catch(NoSuchFieldException e) {
            // ignore
        }
        CONTENTFEATURECOLLECTION_FEATURESOURCE = field1;
        DECORATINGSIMPLEFEATURECOLLECTION_DELEGATE = field2;
        DECORATINGFEATURECOLLECTION_DELEGATE = field3;
    }
    
    private final Map<ContentDataStore, Transaction> cache = new IdentityHashMap<ContentDataStore, Transaction>();
    
    public void cache(FeatureCollection<? extends FeatureType, ? extends Feature> coll) {
        try {
            if(coll instanceof ContentFeatureCollection && CONTENTFEATURECOLLECTION_FEATURESOURCE != null) {
                ContentFeatureCollection cfc = (ContentFeatureCollection) coll;
                ContentFeatureSource fs = (ContentFeatureSource) CONTENTFEATURECOLLECTION_FEATURESOURCE.get(cfc);
                cache(fs);
            } else if(coll instanceof DecoratingSimpleFeatureCollection && DECORATINGSIMPLEFEATURECOLLECTION_DELEGATE != null) {
                cache((FeatureCollection<?,?>)DECORATINGSIMPLEFEATURECOLLECTION_DELEGATE.get(coll));
            } else if(coll instanceof DecoratingFeatureCollection && DECORATINGFEATURECOLLECTION_DELEGATE != null) {
                cache((FeatureCollection<?,?>)DECORATINGFEATURECOLLECTION_DELEGATE.get(coll));
            }
        } catch(IllegalArgumentException e) {
            // ignore
        } catch(IllegalAccessException e) {
            // ignore
        }
    }
    
    public void cache(ContentFeatureSource cfs) {
        cache.put(cfs.getDataStore(), cfs.getTransaction());
    }
    
    public void apply(FeatureSource source) {
        DataAccess ds = source.getDataStore();
        if(ds instanceof ContentDataStore) {
            ContentDataStore cds = (ContentDataStore) ds;
            Transaction tx = cache.get(cds);
            if(source instanceof ContentFeatureSource) {
                ContentFeatureSource cfs = (ContentFeatureSource) source;
                cfs.setTransaction(tx);
            } else if(source instanceof FeatureStore) {
                ((FeatureStore) source).setTransaction(tx);
            }
        }
    }
}
