/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the OpenSearch collections
 *
 * @author Andrea Aime - GeoSolutions
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/oseo/collections")
public class CollectionsController extends AbstractOpenSearchController {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public CollectionsController(OpenSearchAccessProvider accessProvider) {
        super(accessProvider);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public CollectionReferences getCollections(HttpServletRequest request, 
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit) throws IOException {
        // query the collections for their names
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getCollectionSource();
        Query query = new Query();
        if(offset != null) {
            validateMin(offset, 0, "offset");
            query.setStartIndex(offset);
        }
        final int maximumRecordsPerPage = accessProvider.getService().getMaximumRecordsPerPage();
        if(limit != null) {
            validateMin(limit, 0, "limit");
            validateMax(limit, maximumRecordsPerPage, "limit");
            query.setMaxFeatures(limit);
        } else {
            query.setMaxFeatures(maximumRecordsPerPage);
        }
        query.setSortBy(new SortBy[] {FF.sort("name", SortOrder.ASCENDING)});
        query.setPropertyNames(new String[] { "name" });
        FeatureCollection<FeatureType, Feature> features = fs.getFeatures(query);
        
        // map to java beans for JSON encoding
        String baseURL = ResponseUtils.baseURL(request);
        List<CollectionReference> list = new ArrayList<>();
        features.accepts(f -> {
            String name = (String) f.getProperty("name").getValue();
            String collectionHref = ResponseUtils.buildURL(baseURL, "/rest/oseo/collections/" + name, null, URLType.RESOURCE);
            String oseoHref = ResponseUtils.buildURL(baseURL, "/oseo/description", Collections.singletonMap("parentId", name), URLType.RESOURCE);
            CollectionReference cr = new CollectionReference(name, collectionHref, oseoHref);
            list.add(cr);
        }, null);
        return new CollectionReferences(list);
    }

    @GetMapping(path = "{collection}",  produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public FeatureCollection getCollection(@PathVariable(name="collection", required=true) String collection) throws IOException {
        // query the collections for their names
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getCollectionSource();
        Query query = new Query();
        query.setFilter(FF.equal(FF.property("name"), FF.literal("collection"), true));
        FeatureCollection<FeatureType, Feature> fc = fs.getFeatures(query);
        if(fc.isEmpty()) {
            throw new ResourceNotFoundException("Could not find a collection named '" + collection + "'");
        }
        
        return fc;
    }
    

}
