package org.geoserver.geosearch;

import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.FreemarkerFormat;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Status;

public abstract class AbstractFeatureDescription extends GeoServerProxyAwareRestlet {
    private Catalog myCatalog;

    private final DataFormat format =
        new FreemarkerFormat(
                "featurepage.ftl", 
                HTMLFeatureDescription.class, 
                MediaType.TEXT_HTML
                );

    private String GEOSERVER_BASE_URL;

    public void setCatalog(Catalog c){
        myCatalog = c;
    }

    public Catalog getCatalog(){
        return myCatalog;
    }

    public SimpleFeature findFeature(Request req){
        String layer = (String)req.getAttributes().get("layer");
        String namespace = (String)req.getAttributes().get("namespace");
        String feature = (String)req.getAttributes().get("feature");
        
        NamespaceInfo ns = myCatalog.getNamespaceByPrefix(namespace);
        if ( ns == null ) {
            throw new RestletException( 
                    "No such namespace:" + namespace,
                    Status.CLIENT_ERROR_NOT_FOUND 
                    );
        }

        FeatureTypeInfo featureType = null;
        try {
            featureType = myCatalog.getFeatureTypeByName(ns, layer);
        } catch (NoSuchElementException e) {
            throw new RestletException(
                e.getMessage(),
                Status.CLIENT_ERROR_NOT_FOUND
            );
        }

        if (!(Boolean)featureType.getMetadata().get("indexingEnabled")){
            throw new RestletException(
                "Indexing is disabled for this layer (" + ns + ":" + layer + ") "
                + featureType.getMetadata().get("indexingEnabled"),
                Status.CLIENT_ERROR_FORBIDDEN
                );
        }

        Query q = new Query();
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        q.setFilter(ff.id(Collections.singleton(ff.featureId(feature))));

        FeatureCollection col = null;
        try { 
            col = featureType.getFeatureSource(null, null).getFeatures(q);
        } catch (IOException e) {
            throw new RestletException(
                    e.getMessage(),
                    Status.SERVER_ERROR_INTERNAL
                    );
        }

        if (col.size() != 1) {
            throw new RestletException(
                "Unexpected results from data query, "
                + "should be exactly one feature with given ID",
                Status.SERVER_ERROR_INTERNAL
            );
        }

        return (SimpleFeature)col.iterator().next();
    }
}
