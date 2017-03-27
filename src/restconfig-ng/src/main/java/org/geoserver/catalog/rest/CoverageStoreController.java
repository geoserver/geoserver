package org.geoserver.catalog.rest;

import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.rest.wrapper.RestWrapperAdapter;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Coverage store controller
 */
@RestController
@RequestMapping(path = "/restng/workspaces/{workspace}/coveragestores", produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE })
public class CoverageStoreController extends CatalogController {

    private static final Logger LOGGER = Logging.getLogger(CoverageStoreController.class);

    @Autowired
    public CoverageStoreController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE })
    public RestWrapper<CoverageStoreInfo> getCoverageStores(
            @PathVariable(name = "workspace") String workspaceName) {
        List<CoverageStoreInfo> coverageStores = catalog
                .getCoverageStoresByWorkspace(workspaceName);
        return wrapList(coverageStores, CoverageStoreInfo.class);
    }

    @GetMapping(path = "{store}", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<CoverageStoreInfo> getCoverageStore(
            @PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName) {
        CoverageStoreInfo coverageStore = catalog.getCoverageStoreByName(workspaceName, storeName);
        if (coverageStore == null) {
            throw new ResourceNotFoundException(
                    "No such coverage store : " + storeName + " in workspace " + workspaceName);
        }
        return wrapCoverageStore(coverageStore);
    }
    
    RestWrapper<CoverageStoreInfo> wrapCoverageStore(CoverageStoreInfo store) {
        return new RestWrapperAdapter<CoverageStoreInfo>(store, CoverageStoreInfo.class) {
            @Override
            public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
                persister.setCallback(new XStreamPersister.Callback() {
                    @Override
                    protected Class<CoverageStoreInfo> getObjectClass() {
                        return CoverageStoreInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        return store;
                    }

                    @Override
                    protected void postEncodeCoverageStore(CoverageStoreInfo cs,
                            HierarchicalStreamWriter writer, MarshallingContext context) {
                        // add a link to the coverages
                        writer.startNode("coverages");
                        converter.encodeCollectionLink("coverages", writer);
                        writer.endNode();
                    }

                    @Override
                    protected void postEncodeReference(Object obj, String ref, String prefix,
                            HierarchicalStreamWriter writer, MarshallingContext context) {
                        if (obj instanceof WorkspaceInfo) {
                            converter.encodeLink("/workspaces/" + converter.encode(ref), writer);
                        }
                    }
                });
            }
        };

    }
}