package org.geoserver.rest.resources;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.RESTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.*;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;


@RestController
@RequestMapping(path = {ROOT_PATH + "/resource", ROOT_PATH + "/resource/**"}, produces="*")
public class ResourceController extends RestBaseController {
    private ResourceStore store;

    @Autowired
    public ResourceController(@Qualifier("resourceStore") ResourceStoreFactory factory) throws Exception {
        super();
        this.store = factory.getObject();
    }

    public ResourceController(ResourceStore store) {
        super();
        this.store = store;
    }

    private static MediaType getMediaType(Resource resource) {
        if (resource.getType() == Resource.Type.RESOURCE) {
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
    protected Resource getResource(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring((ROOT_PATH+"/resource").length());
        if ("".equals(path)) { //root
            path = Paths.BASE;
        }

        return store.get(path);
    }

    protected Operation getOperation(HttpServletRequest request) {
        Operation operation = Operation.DEFAULT;
        String strOp = RESTUtils.getQueryStringValue(request, "operation");
        if (strOp != null) {
            try {
                operation = Operation.valueOf(strOp.trim().toUpperCase());
            } catch (IllegalArgumentException e) {}
        }
        return operation;
    }

    protected MediaType getFormat(HttpServletRequest request) {
        String format = RESTUtils.getQueryStringValue(request, "format");
        if ("xml".equals(format)) {
            return MediaType.APPLICATION_XML;
        } else if ("json".equals(format)) {
            return MediaType.APPLICATION_JSON;
        } else {
            return MediaType.TEXT_HTML;
        }
    }

    private static String href(String path) {
        return ResponseUtils.buildURL(RequestInfo.get().servletURI("resource/"),
                ResponseUtils.urlEncode(path, '/'), null, URLMangler.URLType.RESOURCE);
    }
    private static String formatHtmlLink(String link) {
        return link.replaceAll("&", "&amp;");
    }

    @GetMapping
    public Object get(HttpServletRequest request, HttpServletResponse response) {
        Resource resource = getResource(request);
        Operation operation = getOperation(request);
        Object result;
        response.setContentType(getFormat(request).toString());

        if (operation == Operation.METADATA) {
            result =  wrapObject(new ResourceMetadata(resource), ResourceMetadata.class);
        } else {
            if (resource.getType() == Resource.Type.UNDEFINED) {
                throw new ResourceNotFoundException("Undefined resource path.");
            } else {
                if (request.getMethod().equals("HEAD")) {
                    result = wrapObject("", String.class);
                } else if (resource.getType() == Resource.Type.DIRECTORY) {
                    result = wrapObject(new ResourceDirectory(resource), ResourceDirectory.class);
                } else {
                    result = resource.in();
                    response.setContentType(getMediaType(resource).toString());
                }

                //UriComponents uriComponents = getUriComponents(name, workspaceName, builder);
                //headers.setLocation(uriComponents.toUri());
                response.setHeader("Location", href(resource.path()));
                response.setHeader("Last-Modified", new Date(resource.lastmodified()).toString());
                if (!"".equals(resource.path())) {
                    response.setHeader("Resource-Parent", href(resource.parent().path()));
                }
                response.setHeader("Resource-Type", resource.getType().toString().toLowerCase());

                //Probably return this all the time


            }
        }
        return result;
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ResourceToMapWrapper<>(clazz);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("child", ResourceChild.class);
        xstream.alias("ResourceDirectory", ResourceDirectory.class);
        xstream.alias("ResourceMetadata", ResourceMetadata.class);

        xstream.registerConverter(
            new Converter() {
                public boolean canConvert(Class type) {
                    return ResourceMetadata.class.isAssignableFrom( type );
                }

                private void writeLink(String link, String mimeType, HierarchicalStreamWriter writer) {
                    writer.startNode( "atom:link");
                    writer.addAttribute("xmlns:atom", "http://www.w3.org/2005/Atom");
                    writer.addAttribute("rel", "alternate");
                    writer.addAttribute("href", link);

                    writer.addAttribute("type", mimeType);
                    writer.endNode();
                }

                public void marshal(Object source,
                                    HierarchicalStreamWriter writer,
                                    MarshallingContext context) {

                    ResourceMetadata resource = (ResourceMetadata) source;

                    writer.startNode("name");
                    writer.setValue(resource.getName());
                    writer.endNode();

                    if (resource instanceof ResourceChild) {
                        writeLink(href(resource.getPath()), resource.getMediaType() == null ?
                                        converter.getSupportedMediaTypes().get(0).toString() : resource.getMediaType().toString(),
                                writer);
                    }
                    ResourceReference parent = resource.getParent();
                    writer.startNode("parent");
                    writer.startNode("path");
                    context.convertAnother("/" + parent.getPath());
                    writer.endNode();
                    writeLink(href(parent.getPath()), converter.getSupportedMediaTypes().get(0).toString(), writer);
                    writer.endNode();
                    writer.startNode("lastModified");
                    context.convertAnother(resource.getLastModified());
                    writer.endNode();
                    if (resource.getClass().equals(ResourceMetadata.class)) {
                        writer.startNode("type");
                        context.convertAnother(resource.getType());
                        writer.endNode();
                    }
                    if (resource instanceof ResourceDirectory) {
                        writer.startNode("children");
                        Collection collection = ((ResourceDirectory) resource).getChildren();
                        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
                            Object item = iterator.next();
                            String name = xstream.getMapper().serializedClass(item.getClass());
                            ExtendedHierarchicalStreamWriterHelper.startNode(writer, name, item.getClass());
                            context.convertAnother(item);
                            writer.endNode();
                        }
                        writer.endNode();
                    }
                }

                public Object unmarshal(HierarchicalStreamReader reader,
                                        UnmarshallingContext context) {
                    return null;
                }
            }
        );
    }

    public enum Operation {
        DEFAULT, METADATA, MOVE, COPY
    }

    /**
     *
     * XML/Json object for resource reference.
     *
     */
    protected static class ResourceReference {
        private String path;

        public ResourceReference(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public String getHref() {
            return href(path);
        }
    }

    /**
     *
     * XML/Json object for resource child of directory.
     *
     */
    @XStreamAlias("child")
    protected static class ResourceChild extends ResourceMetadata {
        public ResourceChild(Resource resource, ResourceReference parent) {
            super(resource);
        }
    }

    /**
     *
     * XML/Json object for resource metadata.
     *
     */
    @XStreamAlias("ResourceMetadata")
    protected static class ResourceMetadata {

        private String name;
        private ResourceReference parent;
        private Date lastModified;
        private String type;
        private MediaType mimeType;

        /**
         * Create from resource.
         * The class must be static for serialization, but output is request dependent so passing on self.
         */
        protected ResourceMetadata(Resource resource) {
            if (!resource.path().isEmpty()) {
                parent = new ResourceReference(resource.parent().path());
            }
            mimeType = ResourceController.getMediaType(resource);
            lastModified = new Date(resource.lastmodified());
            type = resource.getType().toString().toLowerCase();
            name = resource.name();
        }

        public ResourceReference getParent() {
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

        public String getPath() {
            return (parent == null ? "" : parent.getPath()+"/")+name;
        }

        public String getHref() {
            return href(getPath());
        }

        public MediaType getMediaType() {
            return mimeType;
        }
    }

    /**
     *
     * XML/Json object for resource directory.
     *
     * @author Niels Charlier
     *
     */
    @XStreamAlias("ResourceDirectory")
    protected static class ResourceDirectory extends ResourceMetadata {

        private List<ResourceChild> children = new ArrayList<ResourceChild>();

        /**
         * Create from resource.
         * The class must be static for serialization, but output is request dependent so passing on self.
         */
        public ResourceDirectory(Resource resource) {
            super(resource);
            for (Resource child : resource.list()) {
                children.add(new ResourceChild(child, new ResourceReference(getPath())));
            }
        }

        public List<ResourceChild> getChildren() {
            return children;
        }
    }

}
