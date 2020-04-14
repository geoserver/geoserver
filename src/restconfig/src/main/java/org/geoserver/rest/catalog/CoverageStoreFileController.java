/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.*;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.RESTUploadPathMapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.coverage.grid.io.*;
import org.geotools.util.URLs;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(
    path =
            RestBaseController.ROOT_PATH
                    + "/workspaces/{workspaceName}/coveragestores/{storeName}/{method}.{format}"
)
public class CoverageStoreFileController extends AbstractStoreUploadController {

    /** Keys every known coverage format by lowercase name */
    protected static final HashMap<String, String> FORMAT_LOOKUP = new HashMap<>();

    static {
        for (Format format : CoverageStoreUtils.formats) {
            FORMAT_LOOKUP.put(format.getName().toLowerCase(), format.getName());
        }
    }

    @Autowired
    public CoverageStoreFileController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public void coverageStorePost(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable UploadMethod method,
            @PathVariable String format,
            @RequestParam(required = false) String filename,
            @RequestParam(name = "updateBBox", required = false) Boolean updateBBox,
            HttpServletRequest request)
            throws IOException {

        if (updateBBox == null) updateBBox = false;
        // check the coverage store exists
        CoverageStoreInfo info = catalog.getCoverageStoreByName(workspaceName, storeName);
        if (info == null) {
            throw new ResourceNotFoundException(
                    "No such coverage store: " + workspaceName + "," + storeName);
        }

        GridCoverageReader reader = info.getGridCoverageReader(null, null);
        if (reader instanceof StructuredGridCoverage2DReader) {
            StructuredGridCoverage2DReader sr = (StructuredGridCoverage2DReader) reader;
            if (sr.isReadOnly()) {
                throw new RestException(
                        "Coverage store found, but it cannot harvest extra resources",
                        HttpStatus.METHOD_NOT_ALLOWED);
            }
        } else {
            throw new RestException(
                    "Coverage store found, but it does not support resource harvesting",
                    HttpStatus.METHOD_NOT_ALLOWED);
        }

        StructuredGridCoverage2DReader sr = (StructuredGridCoverage2DReader) reader;
        // This method returns a List of the harvested files.
        final List<File> uploadedFiles = new ArrayList<>();
        for (Resource res :
                doFileUpload(method, workspaceName, storeName, filename, format, request)) {
            uploadedFiles.add(Resources.find(res));
        }
        // File Harvesting
        sr.harvest(null, uploadedFiles, GeoTools.getDefaultHints());
        if (updateBBox) new MosaicInfoBBoxHandler(catalog).updateNativeBBox(info, sr);
    }

    @PutMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(code = HttpStatus.CREATED)
    public RestWrapper<CoverageStoreInfo> coverageStorePut(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable UploadMethod method,
            @PathVariable String format,
            @RequestParam(name = "configure", required = false) String configure,
            @RequestParam(name = "USE_JAI_IMAGEREAD", required = false) Boolean useJaiImageRead,
            @RequestParam(name = "coverageName", required = false) String coverageName,
            @RequestParam(required = false) String filename,
            HttpServletRequest request)
            throws IOException {

        Format coverageFormat = getCoverageFormat(format);

        // doFileUpload returns a List of File but in the case of a Put operation the list contains
        // only a value
        List<Resource> files =
                doFileUpload(method, workspaceName, storeName, filename, format, request);
        final Resource uploadedFile = files.get(0);

        // create a builder to help build catalog objects
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(catalog.getWorkspaceByName(workspaceName));

        // create the coverage store
        CoverageStoreInfo info = catalog.getCoverageStoreByName(workspaceName, storeName);
        boolean add = false;
        if (info == null) {
            // create a new coverage store
            LOGGER.info("Auto-configuring coverage store: " + storeName);

            info = builder.buildCoverageStore(storeName);
            add = true;
        } else {
            // use the existing
            LOGGER.info("Using existing coverage store: " + storeName);
        }

        info.setType(coverageFormat.getName());
        URL uploadedFileURL = URLs.fileToUrl(Resources.find(uploadedFile));
        if (method.isInline()) {
            // TODO: create a method to figure out the relative url instead of making assumption
            // about the structure

            String defaultRoot = "/data/" + workspaceName + "/" + storeName;

            StringBuilder urlBuilder;
            try {
                urlBuilder =
                        new StringBuilder(Resources.find(uploadedFile).toURI().toURL().toString());
            } catch (MalformedURLException e) {
                throw new RestException(
                        "Error create building coverage URL", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }

            String url;
            if (uploadedFile.getType() == Type.DIRECTORY && uploadedFile.name().equals(storeName)) {

                int def = urlBuilder.indexOf(defaultRoot);

                if (def >= 0) {
                    url = "file:data/" + workspaceName + "/" + storeName;
                } else {
                    url = urlBuilder.toString();
                }
            } else {

                int def = urlBuilder.indexOf(defaultRoot);

                if (def >= 0) {

                    String itemPath = urlBuilder.substring(def + defaultRoot.length());

                    url = "file:data/" + workspaceName + "/" + storeName + itemPath;
                } else {
                    url = urlBuilder.toString();
                }
            }
            if (url.contains("+")) {
                url = url.replace("+", "%2B");
            }
            if (url.contains(" ")) {
                url = url.replace(" ", "%20");
            }
            info.setURL(url);
        } else {
            info.setURL(uploadedFileURL.toExternalForm());
        }

        // add or update the datastore info
        if (add) {
            if (!catalog.validate(info, true).isValid()) {
                throw new RuntimeException("Validation failed");
            }
            catalog.add(info);
        } else {
            if (!catalog.validate(info, false).isValid()) {
                throw new RuntimeException("Validation failed");
            }
            catalog.save(info);
        }

        builder.setStore(info);

        // check configure parameter, if set to none to not try to configure coverage
        if ("none".equalsIgnoreCase(configure)) {
            return null;
        }

        GridCoverage2DReader reader = null;
        try {
            CatalogRepository repository = catalog.getResourcePool().getRepository();
            Hints hints = new Hints(new RenderingHints(Hints.REPOSITORY, repository));
            reader = ((AbstractGridFormat) coverageFormat).getReader(uploadedFileURL, hints);
            if (reader == null) {
                throw new RestException(
                        "Could not acquire reader for coverage.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // coverage read params
            final Map customParameters = new HashMap();
            if (useJaiImageRead != null) {
                customParameters.put(
                        AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString(), useJaiImageRead);
            }

            // check if the name of the coverage was specified
            String[] names = reader.getGridCoverageNames();
            if (names.length > 1 && coverageName != null) {
                throw new RestException(
                        "The reader found more than one coverage, "
                                + "coverageName cannot be used in this case (it would generate "
                                + "the same name for all coverages found",
                        HttpStatus.BAD_REQUEST);
            }

            // configure all available coverages, preserving backwards compatibility for the
            // case of single coverage reader
            if (names.length > 1) {
                for (String name : names) {
                    SingleGridCoverage2DReader singleReader =
                            new SingleGridCoverage2DReader(reader, name);
                    configureCoverageInfo(
                            builder, info, add, name, name, singleReader, customParameters);
                }
            } else {
                configureCoverageInfo(
                        builder, info, add, names[0], coverageName, reader, customParameters);
            }

            // poach the coverage store data format
            return wrapObject(info, CoverageStoreInfo.class);
        } catch (RestException e) {
            throw e;
        } catch (Exception e) {
            throw new RestException(
                    "Error auto-configuring coverage", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (IOException e) {
                    // it's ok, we tried
                }
            }
        }
    }

    private Format getCoverageFormat(String format) {
        String coverageFormatName = FORMAT_LOOKUP.get(format);

        if (coverageFormatName == null) {
            throw new RestException(
                    "Unsupported format: "
                            + format
                            + ", available formats are: "
                            + FORMAT_LOOKUP.keySet().toString(),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            return CoverageStoreUtils.acquireFormat(coverageFormatName);
        } catch (Exception e) {
            throw new RestException(
                    "Coveragestore format unavailable: " + coverageFormatName,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void configureCoverageInfo(
            CatalogBuilder builder,
            CoverageStoreInfo storeInfo,
            boolean add,
            String nativeName,
            String coverageName,
            GridCoverage2DReader reader,
            final Map customParameters)
            throws Exception {
        CoverageInfo cinfo = builder.buildCoverage(reader, customParameters);

        if (coverageName != null) {
            cinfo.setName(coverageName);
        }
        if (nativeName != null) {
            cinfo.setNativeCoverageName(nativeName);
        }

        if (!add) {
            // update the existing
            String name = coverageName != null ? coverageName : nativeName;
            CoverageInfo existing = catalog.getCoverageByCoverageStore(storeInfo, name);
            if (existing == null) {
                // grab the first if there is only one
                List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore(storeInfo);
                // single coverage reader?
                if (coverages.size() == 1 && coverages.get(0).getNativeName() == null) {
                    existing = coverages.get(0);
                }
                // check if we have it or not
                if (coverages.size() == 0) {
                    // no coverages yet configured, change add flag and continue on
                    add = true;
                } else {
                    for (CoverageInfo ci : coverages) {
                        if (ci.getNativeName().equals(name)) {
                            existing = ci;
                        }
                    }
                    if (existing == null) {
                        add = true;
                    }
                }
            }

            if (existing != null) {
                builder.updateCoverage(existing, cinfo);
                catalog.validate(existing, false).throwIfInvalid();
                catalog.save(existing);
                cinfo = existing;
            }
        }

        // do some post configuration, if srs is not known or unset, transform to 4326
        if ("UNKNOWN".equals(cinfo.getSRS())) {
            cinfo.setSRS("EPSG:4326");
        }

        // add/save
        if (add) {
            catalog.validate(cinfo, true).throwIfInvalid();
            catalog.add(cinfo);

            LayerInfo layerInfo = builder.buildLayer(cinfo);

            boolean valid = true;
            try {
                if (!catalog.validate(layerInfo, true).isValid()) {
                    valid = false;
                }
            } catch (Exception e) {
                valid = false;
            }

            layerInfo.setEnabled(valid);
            catalog.add(layerInfo);
        } else {
            catalog.save(cinfo);
        }
    }

    @Override
    protected Resource findPrimaryFile(Resource directory, String format) {
        AbstractGridFormat coverageFormat = (AbstractGridFormat) getCoverageFormat(format);

        // first check if the format accepts a whole directory
        if (coverageFormat.accepts(directory.dir())) {
            return directory;
        }
        for (Resource f : directory.list()) {
            if (f.getType() == Type.DIRECTORY) {
                Resource result = findPrimaryFile(f, format);
                if (result != null) {
                    return result;
                }
            } else {
                if (coverageFormat.accepts(f.file())) {
                    return f;
                }
            }
        }

        return null;
    }

    /**
     * Does the file upload based on the specified method.
     *
     * @param method The method, one of 'file.' (inline), 'url.' (via url), or 'external.' (already
     *     on server)
     * @param storeName The name of the store being added
     * @param format The store format.
     */
    protected List<Resource> doFileUpload(
            UploadMethod method,
            String workspaceName,
            String storeName,
            String filename,
            String format,
            HttpServletRequest request)
            throws IOException {
        Resource directory = null;

        boolean postRequest =
                request != null && HttpMethod.POST.name().equalsIgnoreCase(request.getMethod());

        // Prepare the directory only in case this is not an external upload
        if (method.isInline()) {
            // Mapping of the input directory
            if (method == UploadMethod.url) {
                // For URL upload method, workspace and StoreName are not considered
                directory = createFinalRoot(null, null, postRequest);
            } else {
                directory = createFinalRoot(workspaceName, storeName, postRequest);
            }
        }
        return handleFileUpload(
                storeName, workspaceName, filename, method, format, directory, request);
    }

    private Resource createFinalRoot(String workspaceName, String storeName, boolean isPost)
            throws IOException {
        // Check if the Request is a POST request, in order to search for an existing coverage
        Resource directory = null;
        if (isPost && storeName != null) {
            // Check if the coverage already exists
            CoverageStoreInfo coverage = catalog.getCoverageStoreByName(storeName);
            if (coverage != null) {
                if (workspaceName == null
                        || coverage.getWorkspace().getName().equalsIgnoreCase(workspaceName)) {
                    // If the coverage exists then the associated directory is defined by its URL
                    directory =
                            Resources.fromPath(
                                    URLs.urlToFile(new URL(coverage.getURL())).getPath(),
                                    catalog.getResourceLoader().get(""));
                }
            }
        }
        // If the directory has not been found then it is created directly
        if (directory == null) {
            directory =
                    catalog.getResourceLoader().get(Paths.path("data", workspaceName, storeName));
        }

        // Selection of the original ROOT directory path
        StringBuilder root = new StringBuilder(directory.path());
        // StoreParams to use for the mapping.
        Map<String, String> storeParams = new HashMap<>();
        // Listing of the available pathMappers
        List<RESTUploadPathMapper> mappers =
                GeoServerExtensions.extensions(RESTUploadPathMapper.class);
        // Mapping of the root directory
        for (RESTUploadPathMapper mapper : mappers) {
            mapper.mapStorePath(root, workspaceName, storeName, storeParams);
        }
        directory = Resources.fromPath(root.toString());
        return directory;
    }
}
