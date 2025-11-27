/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vectormosaic.rest;

import static org.geotools.data.util.PropertiesTransformer.paramsStringToProperties;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.AbstractStoreUploadController;
import org.geoserver.rest.catalog.VectorGranuleIngestionConfigurer;
import org.geoserver.rest.catalog.VectorGranuleIngestionMetadata;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.api.data.DataStore;
import org.geotools.util.logging.Logging;
import org.geotools.vectormosaic.VectorMosaicStore;
import org.geotools.vectormosaic.VectorMosaicStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the VectorMosaic DataStore. */
@RestController
@ControllerAdvice
@RequestMapping(
        path = RestBaseController.ROOT_PATH
                + "/workspaces/{workspaceName}/datastores/{storeName}/mosaic/{method}.{format}")
public class VectorMosaicStoreController extends AbstractStoreUploadController {

    private static final Logger LOGGER = Logging.getLogger(VectorMosaicStoreController.class);

    private static final VectorMosaicStoreFactory VM_FACTORY = new VectorMosaicStoreFactory();

    @Autowired
    public VectorMosaicStoreController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public void uploadGranule(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable UploadMethod method,
            @PathVariable String format,
            @RequestParam(value = "spi", defaultValue = "default") String spi,
            @RequestParam(name = "params", required = false) String uploadingParams,
            HttpServletRequest request)
            throws Exception {

        DataStoreInfo info = catalog.getDataStoreByName(workspaceName, storeName);
        if (info == null) {
            throw new ResourceNotFoundException("DataStore " + storeName + " not found");
        }
        String type = info.getType();
        if (!VM_FACTORY.getDisplayName().equals(type)) {
            throw new RestException("Store " + storeName + " is not a VectorMosaic store", HttpStatus.BAD_REQUEST);
        }

        // Lookup configure by SPI (e.g. "dggs")
        VectorGranuleIngestionConfigurer configurer = null;
        for (VectorGranuleIngestionConfigurer extension :
                GeoServerExtensions.extensions(VectorGranuleIngestionConfigurer.class)) {
            if (spi.equals(extension.getName())) {
                configurer = extension;
                break;
            }
        }
        if (configurer == null) {
            throw new RestException("Unsupported spi: " + spi, HttpStatus.BAD_REQUEST);
        }

        Object harvestedResource = null;
        if (method == UploadMethod.remote) {
            harvestedResource = handleRemoteUrl(request);
            LOGGER.info("Remote granule resource harvested: " + harvestedResource);

        } else {
            List<Resource> resources = doFileUpload(method, workspaceName, storeName, format, request);
            if (!resources.isEmpty()) {
                harvestedResource = Resources.find(resources.get(0));
                LOGGER.info("Uploaded granule resource harvested: " + harvestedResource);
            }
        }

        if (harvestedResource == null) {
            LOGGER.warning("No resources harvested for VectorMosaic granule upload");
            throw new RestException("No resources harvested from upload", HttpStatus.BAD_REQUEST);
        }

        VectorMosaicStore vectorMosaicDataStore =
                (VectorMosaicStore) info.getDataStore(null); // ensure datastore is created
        Properties granuleParameters = paramsStringToProperties(uploadingParams);
        Properties commonParameters = vectorMosaicDataStore.getCommonParameters();
        LOGGER.fine("Granule properties: " + granuleParameters);
        LOGGER.fine("VectorMosaic common properties: {}" + commonParameters);

        VectorGranuleIngestionMetadata result =
                configurer.configureMetadata(harvestedResource, granuleParameters, commonParameters);

        String delegateStoreName = vectorMosaicDataStore.getDelegateStoreName();
        DataStoreInfo delegateInfo = catalog.getDataStoreByName(delegateStoreName);
        if (delegateInfo == null) {
            throw new RestException(
                    "Delegate store '" + delegateStoreName + "' not found for VectorMosaic store '" + storeName + "'",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        DataStore indexStore = (DataStore) delegateInfo.getDataStore(null);
        String indexTypeName = indexStore.getTypeNames()[0];
        String paramKeys = vectorMosaicDataStore.getConnectionParameterKey();
        LOGGER.fine("Ingesting granule into VectorMosaic index store=" + delegateStoreName + "type=" + indexTypeName
                + " params=" + paramKeys);

        VectorMosaicIngestor ingestor =
                new VectorMosaicIngestor(indexStore, indexTypeName, paramKeys, commonParameters);
        ingestor.ingest(result);
        LOGGER.info("Granule successfully ingested into VectorMosaic");
    }

    /**
     * Does the file upload based on the specified method.
     *
     * @param method The method, one of 'file.' (inline), 'url.' (via url), 'external.' (already on server) or 'remote'
     * @param storeName The name of the store being added
     */
    protected List<Resource> doFileUpload(
            UploadMethod method, String workspaceName, String storeName, String format, HttpServletRequest request)
            throws IOException {
        Resource directory = null;

        boolean postRequest = request != null && HttpMethod.POST.name().equalsIgnoreCase(request.getMethod());

        // Mapping of the input directory
        if (method == UploadMethod.url) {
            // For URL upload method, workspace and StoreName are not considered
            directory = RESTUtils.createUploadRoot(catalog, null, null, postRequest);
        } else if (method == UploadMethod.file
                || (method == UploadMethod.external && RESTUtils.isZipMediaType(request))) {
            // Prepare the directory for file upload or external upload of a zip file
            directory = RESTUtils.createUploadRoot(catalog, workspaceName, storeName, postRequest);
        }
        return handleFileUpload(storeName, workspaceName, null, method, format, directory, request);
    }

    /** Return the remote URL provided in the request. */
    protected String handleRemoteUrl(HttpServletRequest request) {

        try {
            // get the URL/URI to be harvested
            return IOUtils.toString(request.getReader());
        } catch (RestException re) {
            throw re;
        } catch (Throwable t) {
            throw new RestException("Error while retrieving the remote URL:", HttpStatus.INTERNAL_SERVER_ERROR, t);
        }
    }
}
