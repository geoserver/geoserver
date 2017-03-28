/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Structured coverage controller, allows to visit and query granules
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH
        + "/workspaces/<workspaceName>/coveragestores/<storeName>/coverages/<coverageName>/index")
public class StructuredCoverageController extends CatalogController {

    private static final Logger LOGGER = Logging.getLogger(StructuredCoverageController.class);

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
    public StructuredCoverageController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public RestWrapper<IndexSchema> getCoverageStores(
            @PathVariable(name = "workspaceNaem") String workspaceName,
            @PathVariable(name = "storeName") String storeName,
            @PathVariable(name = "coverageName") String coverageName) throws IOException {
        CoverageInfo coverage = getExistingStructuredCoverage(workspaceName, storeName,
                coverageName);

        String nativeCoverageName = coverage.getNativeCoverageName();

        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage
                .getGridCoverageReader(null, null);
        if (nativeCoverageName == null) {
            if (reader.getGridCoverageNames().length > 1) {
                throw new IllegalStateException("The grid coverage configuration for "
                        + coverage.getName()
                        + " does not specify a native coverage name, yet the reader provides more than one coverage. "
                        + "Please assign a native coverage name (the GUI does so automatically)");
            } else {
                nativeCoverageName = reader.getGridCoverageNames()[0];
            }
        }

        GranuleSource source = reader.getGranules(nativeCoverageName, true);
        SimpleFeatureType schema = source.getSchema();
        List<AttributeTypeInfo> attributes = new CatalogBuilder(catalog).getAttributes(schema,
                null);

        IndexSchema indexSchema = new IndexSchema(attributes);
        return wrapObject(indexSchema, IndexSchema.class);
    }

    private CoverageInfo getExistingStructuredCoverage(String workspaceName, String storeName,
            String coverageName) {
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new ResourceNotFoundException("No such workspace : " + workspaceName);
        }
        CoverageStoreInfo store = catalog.getCoverageStoreByName(ws, storeName);
        if (store == null) {
            throw new ResourceNotFoundException("No such coverage store: " + storeName);
        }
        Optional<CoverageInfo> optCoverage = catalog.getCoveragesByStore(store).stream()
                .filter(si -> storeName.equals(si.getName())).findFirst();
        if (!optCoverage.isPresent()) {
            throw new ResourceNotFoundException("No such coverage in store: " + coverageName);
        }
        CoverageInfo coverage = optCoverage.get();
        return coverage;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return CoverageStoreInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("Schema", IndexSchema.class);
        xstream.alias("Attribute", AttributeTypeInfoImpl.class);
        xstream.omitField(AttributeTypeInfoImpl.class, "featureType");
        xstream.omitField(AttributeTypeInfoImpl.class, "metadata");
        ReflectionConverter rc = new ReflectionConverter(xstream.getMapper(),
                xstream.getReflectionProvider()) {
            @Override
            public boolean canConvert(Class type) {
                return type.equals(IndexSchema.class);
            }

            @Override
            public void marshal(Object original, HierarchicalStreamWriter writer,
                    MarshallingContext context) {
                super.marshal(original, writer, context);
                converter.encodeLink("granules", writer);
            }
        };
        xstream.registerConverter(rc);
    }

}