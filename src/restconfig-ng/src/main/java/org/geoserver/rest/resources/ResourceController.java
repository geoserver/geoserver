/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;
 

import static org.geoserver.rest.RestBaseController.ROOT_PATH;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.AtomLink;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamJSONMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.converters.XStreamXMLMessageConverter;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import freemarker.template.ObjectWrapper;

@RestController
@RequestMapping(path = {ROOT_PATH + "/resource", ROOT_PATH + "/resource/**"})
public class ResourceController extends RestBaseController {
    private ResourceStore resources;
    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");
    
    private final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
    //TODO: Should we actually be doing this?
    private final DateFormat FORMAT_HEADER = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    {
        FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        FORMAT_HEADER.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Autowired
    public ResourceController(@Qualifier("resourceStore") ResourceStoreFactory factory) throws Exception {
        super();
        this.resources = factory.getObject();
    }

    public ResourceController(ResourceStore store) {
        super();
        this.resources = store;
    }

    /**
     * Extract expected media type from supplied resource
     * @param resource
     * @param request
     * @return Content type requested
     */
    protected static MediaType getMediaType(Resource resource, HttpServletRequest request) {
        if (resource.getType() == Resource.Type.DIRECTORY) {
            return getFormat(request);
        } else if (resource.getType() == Resource.Type.RESOURCE) {
            String mimeType = URLConnection.guessContentTypeFromName(resource.name());
            if (mimeType == null || MediaType.APPLICATION_OCTET_STREAM.toString().equals(mimeType)) {
                //try guessing from data
                try (InputStream is = new BufferedInputStream(resource.in())) {
                    mimeType = URLConnection.guessContentTypeFromStream(is);
                } catch (IOException e) {
                    //do nothing, we'll just use application/octet-stream
                }
            }
            return mimeType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.valueOf(mimeType);
        } else {
            return null;
        }
    }

    /**
     * Access resource requested, note this may be UNDEFINED
     * 
     * @param request
     * @return Resource requested, may be UNDEFINED if not found.
     */
    protected Resource resource(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring((ROOT_PATH+"/resource").length());
        if ("".equals(path)) { //root
            path = Paths.BASE;
        }

        return resources.get(path);
    }

    protected static Operation operation(HttpServletRequest request) {
        Operation operation = Operation.DEFAULT;
        String strOp = RESTUtils.getQueryStringValue(request, "operation");
        if (strOp != null) {
            try {
                operation = Operation.valueOf(strOp.trim().toUpperCase());
            } catch (IllegalArgumentException e) {}
        }
        return operation;
    }

    protected static MediaType getFormat(HttpServletRequest request) {
        String format = RESTUtils.getQueryStringValue(request, "format");
        if ("xml".equals(format)) {
            return MediaType.APPLICATION_XML;
        } else if ("json".equals(format)) {
            return MediaType.APPLICATION_JSON;
        } else {
            return MediaType.TEXT_HTML;
        }
    }

    protected static String href(String path) {
        return ResponseUtils.buildURL(RequestInfo.get().servletURI("resource/"),
                ResponseUtils.urlEncode(path, '/'), null, URLMangler.URLType.RESOURCE);
    }
    protected static String formatHtmlLink(String link) {
        return link.replaceAll("&", "&amp;");
    }

    /**
     * Actual get implementation handles a distrubing number of cases.
     * <p>
     * All the inner Resource classes are data transfer object for representing resource metadata, this method also can return direct access to
     * resource contents.
     * <p>
     * Headers:
     * <ul>
     * <li>Location: Link to resource
     * <li>Resource-Type: DIRECTORY, RESOURCE, UNDEFINED
     * <li>Resource-Parent: Link to parent DIRECTORY
     * <li>Last-Modified: Last modifed date (this is a standard header).
     * </ul>
     * 
     * @param request Request indicating resource, parameters indicating {@link ResourceController.Operation} and {@link MediaType}.
     * @param response Response provided allowing us to set headers (content type, content length, Resource-Parent, Resource-Type).
     * @return Returns wrapped info object, or direct access to resource contents depending on requested operation
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, produces = {MediaType.ALL_VALUE})
    public Object get(HttpServletRequest request, HttpServletResponse response) {
        Resource resource = resource(request);
        Operation operation = operation(request);
        Object result;
        response.setContentType(getFormat(request).toString());

        if (operation == Operation.METADATA) {
            result =  wrapObject(new ResourceMetadataInfo(resource, request), ResourceMetadataInfo.class);
        } else {
            if (resource.getType() == Resource.Type.UNDEFINED) {
                throw new ResourceNotFoundException("Undefined resource path.");
            } else {
                HttpHeaders responseHeaders = new HttpHeaders();
                MediaType mediaType = getMediaType(resource, request);
                responseHeaders.setContentType(mediaType);
                response.setContentType(mediaType.toString());

                if (request.getMethod().equals("HEAD")) {
                    result = new ResponseEntity("", responseHeaders, HttpStatus.OK);
                } else if (resource.getType() == Resource.Type.DIRECTORY) {
                    result = wrapObject(new ResourceDirectoryInfo(resource, request), ResourceDirectoryInfo.class);
                } else {
                    result = new ResponseEntity(resource.in(), responseHeaders, HttpStatus.OK);
                }
                response.setHeader("Location", href(resource.path()));
                response.setHeader("Last-Modified", FORMAT_HEADER.format(resource.lastmodified()).toString());
                if (!"".equals(resource.path())) {
                    response.setHeader("Resource-Parent", href(resource.parent().path()));
                }
                response.setHeader("Resource-Type", resource.getType().toString().toLowerCase());
            }
        }
        return result;
    }
    /**
     * Upload resource contents.
     */
    @PutMapping(consumes = {MediaType.ALL_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public void resourcePut(HttpServletRequest request){
        Resource resource = resource(request);
        Resource directory = resource.parent();
        String filenName = resource.name();
        
        Resource result = fileUpload(directory, filenName, request);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("PUT resource: " + resource.path());
        }
    }

    /**
     * Delete resourc
     * 
     * @return All templates
     */
    @DeleteMapping
    public void resourceDelete(HttpServletRequest request){
        Resource resource = resource(request);
        boolean removed = resource.delete();
        if (!removed) {
            throw new RestException("Resource '" + resource.path() + "' not removed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Verifies mime type and use {@link RESTUtils}
     * @param directory
     * @param filename
     * @param request
     * @return
     */
    protected Resource fileUpload(Resource directory, String filename, HttpServletRequest request) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("PUT file: mimetype=" + request.getContentType() + ", path="
                    + directory.path());
        }
        try {
            Resource upload = RESTUtils.handleBinUpload(filename, directory, false, request);
            return upload;
        } catch (IOException problem) {
            throw new RestException(problem.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
                    problem);
        }
    }
    //@Override
    //protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
    //    return new ResourceToMapWrapper<>(clazz);
    //}

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("child", ResourceChildInfo.class);
        xstream.alias("ResourceDirectory", ResourceDirectoryInfo.class);
        xstream.alias("ResourceMetadata", ResourceMetadataInfo.class);

        if (converter instanceof XStreamXMLMessageConverter) {
            AtomLink.configureXML(xstream);
            xstream.aliasField("atom:link", ResourceParentInfo.class, "link");
            xstream.aliasField("atom:link", ResourceChildInfo.class, "link");
        } else if (converter instanceof XStreamJSONMessageConverter) {
            AtomLink.configureJSON(xstream);
        }
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<T>(clazz, Arrays.asList(AtomLink.class,
                ResourceDirectoryInfo.class, ResourceMetadataInfo.class, ResourceParentInfo.class, ResourceChildInfo.class));
    }
    /**
     * Operation requested from the REST endpoint.
     */
    public static enum Operation {
        /** Depends on context (different functionality for directory, resource, undefined) */
        DEFAULT,
        /** Requests metadata summary of resource */
        METADATA,
        /** Moves resource to new location */
        MOVE,
        /** Duplicate resource to a new location */
        COPY
    }

    /**
     * Used for parent reference (to indicate directory containing resource).
     * 
     * XML/Json object for resource reference.
     */
    protected static class ResourceParentInfo { 

        private String path;

        private AtomLink link;

        public ResourceParentInfo(String path, AtomLink link) {
            this.path = path;
            this.link = link;
        }

        public String getPath() {
            return path;
        }

        public AtomLink getLink() {
            return link;
        }
    }

    /**
     * Lists Resource for html, json, xml output, as the contents of {@link ResourceDirectoryInfo}.
     */
    @XStreamAlias("child")
    protected static class ResourceChildInfo {

        private String name;

        private AtomLink link;

        public ResourceChildInfo(String name, AtomLink link) {
            this.name = name;
            this.link = link;
        }

        public String getName() {
            return name;
        }

        public AtomLink getLink() {
            return link;
        }
    }

    /**
     * Resource metadata for individual resource entry (name, last modified, type, etc...).
     */
    @XStreamAlias("ResourceMetadata")
    protected static class ResourceMetadataInfo {

        private String name;
        private ResourceParentInfo parent;
        private Date lastModified;
        private String type;

        public ResourceMetadataInfo(String name, ResourceParentInfo parent,
                                Date lastModified, String type) {
            this.name = name;
            this.parent = parent;
            this.lastModified = lastModified;
            this.type = type;
        }

        /**
         * Create from resource.
         * The class must be static for serialization, but output is request dependent so passing on self.
         */
        protected ResourceMetadataInfo(Resource resource, HttpServletRequest request, boolean isDir) {
            if (!resource.path().isEmpty()) {
                parent = new ResourceParentInfo("/" + resource.parent().path(),
                        new AtomLink(href(resource.parent().path()), "alternate",
                                getFormat(request).toString()));
            }
            lastModified = new Date(resource.lastmodified());
            type = isDir ? null : resource.getType().toString().toLowerCase();
            name = resource.name();
        }

        public ResourceMetadataInfo(Resource resource, HttpServletRequest request) {
            this(resource, request, false);
        }

        public ResourceParentInfo getParent() {
            return parent;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Extends ResourceMetadataInfo to list contents.
     * 
     * @author Niels Charlier
     */
    @XStreamAlias("ResourceDirectory")
    protected static class ResourceDirectoryInfo extends ResourceMetadataInfo {

        private List<ResourceChildInfo> children = new ArrayList<ResourceChildInfo>();

        public ResourceDirectoryInfo(String name, ResourceParentInfo parent, Date lastModified,
                                     String type) {
            super(name, parent, lastModified, type);
        }

        /**
         * Create from resource.
         * The class must be static for serialization, but output is request dependent so passing on self.
         */
        public ResourceDirectoryInfo(Resource resource, HttpServletRequest request) {
            super(resource, request, true);
            for (Resource child : resource.list()) {
                children.add(new ResourceChildInfo(child.name(),
                        new AtomLink(href(child.path()), "alternate",
                                getMediaType(child, request).toString())));
            }
        }


        public List<ResourceChildInfo> getChildren() {
            return children;
        }
    }

}
