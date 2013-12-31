/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.format;




import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.wfs.BaseRequestType;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;

import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xml.Encoder;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.xml.sax.helpers.NamespaceSupport;

public class SyncGMLOutputFormat extends WFSGetFeatureOutputFormat {
    WFSInfo wfs;
    Catalog catalog;
    GeoServerInfo global;
    WFSConfiguration configuration;

    public SyncGMLOutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
        super(geoServer, new HashSet(Arrays.asList(new Object[] {"SyncGML"})));

        wfs = geoServer.getService( WFSInfo.class );
        catalog = geoServer.getCatalog();
        global = geoServer.getGlobal();

        this.configuration = configuration;
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        return "text/xml; subtype=gml/3.1.1";
    }

    //@Override
    protected void write(FeatureCollectionType results, OutputStream output, Operation getFeature)
        throws ServiceException, IOException {
        List featureCollections = results.getFeature();
        Function syncFunction = null;

        // round up the info objects for each feature collection
        HashMap<String, Set<FeatureTypeInfo>> ns2metas = new HashMap<String, Set<FeatureTypeInfo>>();
        for (int fcIndex = 0; fcIndex < featureCollections.size(); fcIndex++) {
            if(getFeature.getParameters()[0] instanceof GetFeatureType) {
                // get the query for this featureCollection
                GetFeatureType request = (GetFeatureType) OwsUtils.parameter(getFeature.getParameters(),
                        GetFeatureType.class);
                QueryType queryType = (QueryType) request.getQuery().get(fcIndex);
                if(queryType.getFunction().size()>0) {
                    syncFunction = (Function) queryType.getFunction().get(0);
                }

                // may have multiple type names in each query, so add them all
                for (QName name : (List<QName>) queryType.getTypeName()) {
                    // get a feature type name from the query
                    Name featureTypeName = new NameImpl(name.getNamespaceURI(), name.getLocalPart());
                    FeatureTypeInfo meta = catalog.getFeatureTypeByName(featureTypeName);

                    if (meta == null) {
                        throw new WFSException("Could not find feature type " + featureTypeName
                                + " in the GeoServer catalog");
                    }

                    // add it to the map
                    Set<FeatureTypeInfo> metas = ns2metas.get(featureTypeName.getNamespaceURI());

                    if (metas == null) {
                        metas = new HashSet<FeatureTypeInfo>();
                        ns2metas.put(featureTypeName.getNamespaceURI(), metas);
                    }
                    metas.add(meta);
                }
            } else {
                FeatureType featureType = ((FeatureCollection) featureCollections.get(fcIndex)).getSchema();

                //load the metadata for the feature type
                String namespaceURI = featureType.getName().getNamespaceURI();
                FeatureTypeInfo meta = catalog.getFeatureTypeByName(featureType.getName());

                if(meta == null)
                    throw new WFSException("Could not find feature type " + featureType.getName() + " in the GeoServer catalog");

                //add it to the map
                Set metas = ns2metas.get(namespaceURI);

                if (metas == null) {
                    metas = new HashSet();
                    ns2metas.put(namespaceURI, metas);
                }

                metas.add(meta);
            }
        }

        //set feature bounding parameter
        //JD: this is quite bad as its not at all thread-safe, once we remove the configuration
        // as being a singleton on trunk/2.0.x this should not be an issue
        if ( wfs.isFeatureBounding() ) {
            configuration.getProperties().remove( GMLConfiguration.NO_FEATURE_BOUNDS );
        }
        else {
            configuration.getProperties().add( GMLConfiguration.NO_FEATURE_BOUNDS);
        }

        Encoder encoder = new Encoder(configuration, configuration.schema());
        encoder.setEncoding(Charset.forName( global.getCharset() ));

        {
            NamespaceSupport nss = encoder.getNamespaces();
            for(FeatureCollection fc : (List<FeatureCollection>)featureCollections)
                loadNamespaceBindings(nss, fc);
        }

        //declare wfs schema location
        BaseRequestType gft = (BaseRequestType)getFeature.getParameters()[0];

        encoder.setSchemaLocation(org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                buildSchemaURL(gft.getBaseUrl(), "wfs/1.1.0/wfs.xsd"));

        //declare application schema namespaces
        Map<String, String> params = params("service", "WFS", "version", "1.1.0", "request", "DescribeFeatureType");
        for (Iterator i = ns2metas.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();

            String namespaceURI = (String) entry.getKey();
            Set metas = (Set) entry.getValue();

            StringBuffer typeNames = new StringBuffer();

            String userSchemaLocation = null;
            for (Iterator m = metas.iterator(); m.hasNext();) {
                FeatureTypeInfo meta = (FeatureTypeInfo) m.next();
                if (userSchemaLocation == null) {
                    FeatureType featureType = meta.getFeatureType();
                    Object schemaUri = featureType.getUserData().get("schemaURI");
                    if (schemaUri != null) {
                        userSchemaLocation = schemaUri.toString();
                    }
                }
                typeNames.append(meta.getPrefixedName());

                if (m.hasNext()) {
                    typeNames.append(",");
                }
            }
            params.put("typeName", typeNames.toString());

            //set the schema location if the user provides it, otherwise give a default one
            if (userSchemaLocation != null) {
                encoder.setSchemaLocation(namespaceURI, userSchemaLocation);
            } else {
                String schemaLocation = buildURL(gft.getBaseUrl(), "wfs", params, URLType.SERVICE);
                LOGGER.finer("Unable to find user-defined schema location for: " + namespaceURI
                        + ". Using a built schema location by default: " + schemaLocation);
                encoder.setSchemaLocation(namespaceURI, schemaLocation);
            }
        }

        if(syncFunction != null) {
            List<FeatureCollection> filtered = new ArrayList<FeatureCollection>();
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
            Filter f = ff.equal(ff.literal("true"), syncFunction, false);
            for(FeatureCollection fc : (List<FeatureCollection>)results.getFeature())
                filtered.add(new FilteringFeatureCollection<FeatureType, Feature>(fc, f));
            results.getFeature().clear();
            results.getFeature().addAll(filtered);
        }
        
        encoder.encode(results, org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION, output);
    }

    private static void loadNamespaceBindings(NamespaceSupport nss, FeatureCollection fc) {
        XSDTypeDefinition type =
            (XSDTypeDefinition) fc.getSchema().getUserData().get(
                XSDTypeDefinition.class);
        if(type == null) return;
        loadNamespaceBindings(nss, type.getSchema());
    }

    private static void loadNamespaceBindings(NamespaceSupport nss, XSDSchema schema) {
        for(Map.Entry<String, String> e : ((Map<String,String>)schema.getQNamePrefixToNamespaceMap()).entrySet()) {
            String pref = e.getKey();
            nss.declarePrefix(pref == null ? "" : pref, e.getValue());
        }
    }

	@Override
	protected void write(FeatureCollectionResponse arg0, OutputStream arg1,
			Operation arg2) throws IOException, ServiceException {
		// TODO Auto-generated method stub
		
	}
}