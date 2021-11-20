/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.coverage.grid.io.GranuleRemovalPolicy;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Structured coverage controller, allows to visit and query granules */
@RestController
@ControllerAdvice
@RequestMapping(
    path =
            RestBaseController.ROOT_PATH
                    + "/workspaces/{workspaceName}/coveragestores/{storeName}/coverages/{coverageName}/index"
)
public class StructuredCoverageController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(StructuredCoverageController.class);

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    /**
     * Just holds a list of attributes
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class IndexSchema {
        List<AttributeTypeInfo> attributes;

        public IndexSchema(List<AttributeTypeInfo> attributes) {
            this.attributes = attributes;
        }
    }

    @Autowired
    public StructuredCoverageController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        }
    )
    public RestWrapper<IndexSchema> indexGet(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable String coverageName)
            throws IOException {

        GranuleSource source = getGranuleSource(workspaceName, storeName, coverageName);
        SimpleFeatureType schema = source.getSchema();
        List<AttributeTypeInfo> attributes =
                new CatalogBuilder(catalog).getAttributes(schema, null);

        IndexSchema indexSchema = new IndexSchema(attributes);
        return wrapObject(indexSchema, IndexSchema.class);
    }

    @GetMapping(
        path = "/granules",
        produces = {
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        }
    )
    @ResponseBody
    public SimpleFeatureCollection granulesGet(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable String coverageName,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit)
            throws IOException {

        GranuleSource source = getGranuleSource(workspaceName, storeName, coverageName);
        Query q = toQuery(filter, offset, limit);

        LOGGER.log(Level.SEVERE, "Still need to parse the filters");

        return forceNonNullNamespace(source.getGranules(q));
    }

    @DeleteMapping(path = "/granules")
    @ResponseBody
    public void granulesDelete(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable String coverageName,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "purge", required = false, defaultValue = "none") String purge,
            @RequestParam(name = "updateBBox", required = false) Boolean updateBBox)
            throws IOException {

        if (updateBBox == null) updateBBox = false;
        Query q = toQuery(filter, 0, 1);
        granulesDeleteInternal(
                workspaceName, storeName, coverageName, purge, q.getFilter(), updateBBox);
    }

    /*
     * Note, the .+ regular expression allows granuleId to contain dots instead of having them
     * interpreted as format extension
     */
    @GetMapping(
        path = "/granules/{granuleId:.+}",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseBody
    public FormatCollectionWrapper granuleGet(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable String coverageName,
            @PathVariable String granuleId)
            throws IOException {

        GranuleSource source = getGranuleSource(workspaceName, storeName, coverageName);
        Filter filter = getGranuleIdFilter(granuleId);
        Query q = new Query(null, filter);

        SimpleFeatureCollection granules = source.getGranules(q);
        if (granules.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Could not find a granule with id "
                            + granuleId
                            + " in coverage "
                            + coverageName);
        }

        SimpleFeatureCollection collection = forceNonNullNamespace(granules);

        // and now for the fun part, figure out the extension if it was there, and force
        // the right output format... ugly as hell, but we could not find a better solution
        // regexes with positive and negative lookaheads were tried
        if (granuleId.endsWith(".json")) {
            return new FormatCollectionWrapper.JSONCollectionWrapper(collection);
        } else {
            return new FormatCollectionWrapper.XMLCollectionWrapper(collection);
        }
    }

    private Filter getGranuleIdFilter(String granuleId) {
        if (granuleId.endsWith(".xml")) {
            granuleId = granuleId.substring(0, granuleId.length() - 4);
        } else if (granuleId.endsWith(".json")) {
            granuleId = granuleId.substring(0, granuleId.length() - 5);
        }
        return FF.id(FF.featureId(granuleId));
    }

    /*
     * Note, the .+ regular expression allows granuleId to contain dots instead of having them
     * interpreted as format extension.
     * Note: the optional /{format} suffix is required for compatibility with gsconfig, but does not actually do
     * anything otherwise.
     */
    @DeleteMapping(path = {"/granules/{granuleId:.+}", "/granules/{granuleId:.+}/{format}"})
    @ResponseBody
    public void granuleDelete(
            @PathVariable(name = "workspaceName") String workspaceName,
            @PathVariable String storeName,
            @PathVariable String coverageName,
            @PathVariable String granuleId,
            @RequestParam(name = "purge", required = false, defaultValue = "none") String purge,
            @RequestParam(name = "updateBBox", required = false) Boolean updateBBox)
            throws IOException {

        if (updateBBox == null) updateBBox = false;
        // gsConfig allows for weird calls, like granules/granule.id/.json
        Filter filter = getGranuleIdFilter(granuleId);

        granulesDeleteInternal(workspaceName, storeName, coverageName, purge, filter, updateBBox);
    }

    private void granulesDeleteInternal(
            String workspaceName,
            String storeName,
            String coverageName,
            String purge,
            Filter filter,
            boolean updateBBox)
            throws IOException {
        GranuleStore store = getGranuleStore(workspaceName, storeName, coverageName);
        if (purge != null) {
            GranuleRemovalPolicy policy = mapRemovalPolicy(purge);
            Hints hints = new Hints(Hints.GRANULE_REMOVAL_POLICY, policy);
            store.removeGranules(filter, hints);
        } else {
            store.removeGranules(filter);
        }
        if (updateBBox) {
            // before updating checks that the delete request
            // has not been performed over all granules
            if (filter == null || (!filter.equals(Filter.INCLUDE)))
                new MosaicInfoBBoxHandler(catalog).updateNativeBBox(workspaceName, storeName, null);
        }
    }

    private GranuleRemovalPolicy mapRemovalPolicy(String key) {
        try {
            return GranuleRemovalPolicy.valueOf(key.toUpperCase());
        } catch (Exception e) {
            throw new RestException(
                    "Invalid purge value "
                            + key
                            + ", allowed values are "
                            + Arrays.toString(GranuleRemovalPolicy.values()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return IndexSchema.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("Schema", IndexSchema.class);
        xstream.alias("Attribute", AttributeTypeInfo.class);
        xstream.omitField(AttributeTypeInfoImpl.class, "featureType");
        xstream.omitField(AttributeTypeInfoImpl.class, "metadata");
        ReflectionConverter rc =
                new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider()) {
                    @Override
                    public boolean canConvert(Class type) {
                        return type.equals(IndexSchema.class);
                    }

                    @Override
                    public void marshal(
                            Object original,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        super.marshal(original, writer, context);
                        converter.encodeLink("granules", writer);
                    }
                };
        xstream.registerConverter(rc);
    }

    private GranuleSource getGranuleSource(
            String workspaceName, String storeName, String coverageName) throws IOException {
        CoverageInfo coverage =
                getExistingStructuredCoverage(workspaceName, storeName, coverageName);

        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        String nativeCoverageName = getNativeCoverageName(coverage, reader);

        return reader.getGranules(nativeCoverageName, true);
    }

    private GranuleStore getGranuleStore(
            String workspaceName, String storeName, String coverageName) throws IOException {
        CoverageInfo coverage =
                getExistingStructuredCoverage(workspaceName, storeName, coverageName);

        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        if (reader.isReadOnly()) {
            throw new RestException(
                    "Coverage " + coverage.prefixedName() + " is read ony",
                    HttpStatus.METHOD_NOT_ALLOWED);
        }
        String nativeCoverageName = getNativeCoverageName(coverage, reader);

        return (GranuleStore) reader.getGranules(nativeCoverageName, false);
    }

    private String getNativeCoverageName(
            CoverageInfo coverage, StructuredGridCoverage2DReader reader) throws IOException {
        String nativeCoverageName = coverage.getNativeCoverageName();
        if (nativeCoverageName == null) {
            if (reader.getGridCoverageNames().length > 1) {
                throw new IllegalStateException(
                        "The grid coverage configuration for "
                                + coverage.getName()
                                + " does not specify a native coverage name, yet the reader provides more than one coverage. "
                                + "Please assign a native coverage name (the GUI does so automatically)");
            } else {
                nativeCoverageName = reader.getGridCoverageNames()[0];
            }
        }
        return nativeCoverageName;
    }

    private SimpleFeatureCollection forceNonNullNamespace(SimpleFeatureCollection features)
            throws IOException {
        SimpleFeatureType sourceSchema = features.getSchema();
        if (sourceSchema.getName().getNamespaceURI() == null) {
            try {
                String targetNs = "http://www.geoserver.org/rest/granules";
                AttributeDescriptor[] attributes =
                        sourceSchema
                                .getAttributeDescriptors()
                                .toArray(
                                        new AttributeDescriptor
                                                [sourceSchema.getAttributeDescriptors().size()]);
                SimpleFeatureType targetSchema =
                        FeatureTypes.newFeatureType(
                                attributes,
                                sourceSchema.getName().getLocalPart(),
                                new URI(targetNs));
                return new RetypingFeatureCollection(features, targetSchema);
            } catch (Exception e) {
                throw new IOException(
                        "Failed to retype the granules feature schema, in order to force "
                                + "it having a non null namespace",
                        e);
            }
        } else {
            return features;
        }
    }

    private CoverageInfo getExistingStructuredCoverage(
            String workspaceName, String storeName, String coverageName) {
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new ResourceNotFoundException("No such workspace : " + workspaceName);
        }
        CoverageStoreInfo store = catalog.getCoverageStoreByName(ws, storeName);
        if (store == null) {
            throw new ResourceNotFoundException("No such coverage store: " + storeName);
        }
        Optional<CoverageInfo> optCoverage =
                catalog.getCoveragesByStore(store)
                        .stream()
                        .filter(si -> coverageName.equals(si.getName()))
                        .findFirst();
        if (!optCoverage.isPresent()) {
            throw new ResourceNotFoundException("No such coverage in store: " + coverageName);
        }
        return optCoverage.get();
    }

    private Query toQuery(String filter, Integer offset, Integer limit) {
        // build the query
        Query q = new Query(Query.ALL);

        // ... filter
        if (filter != null) {
            try {
                Filter ogcFilter = ECQL.toFilter(filter);
                q.setFilter(ogcFilter);
            } catch (CQLException e) {
                throw new RestException(
                        "Invalid cql syntax: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }

        // ... offset
        if (offset != null) {
            if (offset < 0) {
                throw new RestException("Invalid offset value: " + offset, HttpStatus.BAD_REQUEST);
            }
            q.setStartIndex(offset);
        }

        if (limit != null) {
            if (limit <= 0) {
                throw new RestException("Invalid limit value: " + offset, HttpStatus.BAD_REQUEST);
            }
            q.setMaxFeatures(limit);
        }

        return q;
    }
}
