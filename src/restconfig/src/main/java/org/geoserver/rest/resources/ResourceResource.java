/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.AtomLink;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveHTMLFormat;
import org.geoserver.rest.format.ReflectiveJSONFormat;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.InputRepresentation;
import org.restlet.resource.Representation;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 *
 *
 * @author Niels Charlier
 *
 */
public class ResourceResource extends AbstractResource {
    
    public enum Operation {DEFAULT, METADATA, MOVE, COPY};
           
    /**
     *
     * XML/Json object for resource reference.
     *
     */
    protected static class ResourceReference {
                
        private String path;

        private AtomLink link;
        
        public ResourceReference(String path, AtomLink link) {
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
     * 
     * XML/Json object for resource child of directory.
     * 
     */
    @XStreamAlias("child")
    protected static class ResourceChild {
                
        private String name;

        private AtomLink link;
        
        public ResourceChild(String name, AtomLink link) {
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
               
        public ResourceMetadata(String name, ResourceReference parent, Date lastModified, String type) {
            this.name = name;
            this.parent = parent;
            this.lastModified = lastModified;
            this.type = type;
        }

        /**
         * Create from resource.
         * The class must be static for serialization, but output is request dependent so passing on self.
         */
        protected ResourceMetadata(Resource resource, ResourceResource self, boolean isDir) {
            if (!"".equals(resource.path())) {
                parent = new ResourceReference("/" + resource.parent().path(),
                        new AtomLink(self.href(resource.parent().path(), true), "alternate", 
                                     self.getFormatGet(false).getMediaType().getName()));
            }            
            lastModified = new Date(resource.lastmodified());
            type = isDir ? null : resource.getType().toString().toLowerCase();
            name = resource.name();
        }
        
        public ResourceMetadata(Resource resource, ResourceResource self) {
            this(resource, self, false);
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

        public ResourceDirectory(String name, ResourceReference parent, Date lastModified, String type) {
            super(name, parent, lastModified, type);
        }
        
        /**
         * Create from resource.
         * The class must be static for serialization, but output is request dependent so passing on self.
         */
        public ResourceDirectory(Resource resource, ResourceResource self) {
            super(resource, self, true);
            for (Resource child : resource.list()) {
                children.add(new ResourceChild(child.name(), 
                        new AtomLink(self.href(child.path(), child.getType() == Type.DIRECTORY), 
                                "alternate", self.getMediaType(child).getName())));
            }
        }
                
        
        public List<ResourceChild> getChildren() {
            return children;
        }
    }
    
    private ResourceStore store;
    private Resource resource;
    private Operation operation;
    
    public ResourceResource(Context context, Request request, Response response, Resource resource, Operation operation,
            ResourceStore store) {
        super(context, request, response);
        this.resource = resource;
        this.operation = operation;
        this.store = store;
    }
    
    public Resource getResource() {
        return resource;
    }

    public Operation getOperation() {
        return operation;
    }
        
    private String href(String path, boolean isDir) {
        String href = getPageInfo().rootURI("resources/" + path);
        if (isDir) {
            String format = (String) getRequest().getAttributes().get("format");
            if (format != null) {
                href += "?format=" + format;
            }
        }
        return href;
    }
    
    public String formatHtmlLink(String link) {
        return link.replaceAll("&", "&amp;");
    }

    private MediaType getMediaType(Resource resource) {
        if (resource.getType() == Type.DIRECTORY) {
            return getFormatGet(false).getMediaType();
        } else if (resource.getType() == Type.RESOURCE) {
            String mimeType = URLConnection.guessContentTypeFromName(resource.name());
            if (mimeType == null || MediaType.APPLICATION_OCTET_STREAM.getName().equals(mimeType)) {
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
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = new ArrayList<DataFormat>();
        
        //HTML
        if (operation == Operation.METADATA || resource.getType() == Type.DIRECTORY) {
            formats.add(new ReflectiveHTMLFormat(request,response,this) {   
                @Override
                protected Configuration createConfiguration(Object data, Class clazz) {
                    Configuration cfg = super.createConfiguration(data, clazz);
                    cfg.setClassForTemplateLoading(ResourceResource.class, "");
                    cfg.setObjectWrapper(new ObjectToMapWrapper<ResourceMetadata>(ResourceMetadata.class) {
                        @Override
                        protected void wrapInternal(Map properties, SimpleHash model, ResourceMetadata object) {
                            super.wrapInternal(properties, model, object);
                            properties.put("path", object.getParent() == null ? "/" :
                                Paths.get(object.getParent().getPath(), object.getName()) ); 
                            properties.put("parent_path", object.getParent() == null ? "" : object.getParent().getPath());
                            properties.put("parent_href", object.getParent() == null ? "" : 
                                formatHtmlLink(object.getParent().getLink().getHref()));
                            if (object instanceof ResourceDirectory) {
                                properties.put("children", new CollectionModel(((ResourceDirectory) object).getChildren(),
                                        new ObjectToMapWrapper<ResourceChild>(ResourceChild.class) {
                                            @Override
                                            protected void wrapInternal(Map properties, SimpleHash model, ResourceChild object) {
                                                super.wrapInternal(properties, model, object);
                                                properties.put("href", formatHtmlLink(object.getLink().getHref()));
                                            }
                                }));
                            }
                        }
                    });
                    return cfg;
                }
                
                @Override protected String getTemplateName(Object data) {
                    if (operation == Operation.METADATA) {
                        return "resourceMetadata";
                    } else {
                        return "resourceDirectory";
                    }
                }
            });
        } 
        
        //XML        
        ReflectiveXMLFormat xmlFormat = new ReflectiveXMLFormat();
        AtomLink.configureXML(xmlFormat.getXStream());
        xmlFormat.getXStream().processAnnotations(ResourceChild.class);
        xmlFormat.getXStream().processAnnotations(ResourceMetadata.class);
        xmlFormat.getXStream().processAnnotations(ResourceDirectory.class);
        xmlFormat.getXStream().aliasField("atom:link", ResourceReference.class, "link");
        xmlFormat.getXStream().aliasField("atom:link", ResourceChild.class, "link");
        formats.add(xmlFormat);
        
        //JSON
        ReflectiveJSONFormat jsonFormat = new ReflectiveJSONFormat();
        AtomLink.configureJSON(jsonFormat.getXStream());
        jsonFormat.getXStream().processAnnotations(ResourceChild.class);
        jsonFormat.getXStream().processAnnotations(ResourceMetadata.class);
        jsonFormat.getXStream().processAnnotations(ResourceDirectory.class);
        formats.add(jsonFormat);
        
        return formats;
    }  

    @Override
    public void handleGet() {
        if (operation == Operation.METADATA) {
            getVariants().add(getFormatGet(false).toRepresentation(new ResourceMetadata(resource, this)));
        }
        else {
            if (resource.getType() == Type.UNDEFINED) {
                throw new RestletException("Undefined resource path.", Status.CLIENT_ERROR_NOT_FOUND);
            } else {
                Representation rep;
                if (getRequest().getMethod().equals(Method.HEAD)) {
                    rep = RESTUtils.emptyBody();
                    rep.setMediaType(getMediaType(resource));
                } else if (resource.getType() == Type.DIRECTORY) {
                    rep = getFormatGet(false).toRepresentation(new ResourceDirectory(resource, this));
                } else {
                    rep = new InputRepresentation(resource.in(), getMediaType(resource));
                }
                rep.setModificationDate(new Date(resource.lastmodified()));
                getVariants().add(rep);

                if (!"".equals(resource.path())) {
                    getResponseHeaders().add("Resource-parent", "/" + resource.parent().path());
                }
                getResponseHeaders().add("Resource-type", resource.getType().toString().toLowerCase());
            }
        }

        super.handleGet();
    }
    
    @Override
    public boolean allowPut() {        
        return true;        
    }
    
    @Override
    public void handlePut() {
        if (resource.getType() == Type.DIRECTORY) {
            throw new RestletException("Attempting to write data to a directory.", Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
        } else {
            try {
                boolean isNew = resource.getType() == Type.UNDEFINED;
                
                if (operation == Operation.COPY || operation == Operation.MOVE) {                    
                    try (InputStream is = getRequest().getEntity().getStream()) {
                        Resource source = store.get(IOUtils.toString(is, "UTF-8"));
                        if (source.getType() == Type.UNDEFINED) {
                            throw new RestletException("Undefined source path.", Status.CLIENT_ERROR_NOT_FOUND);
                        }
                        try {
                            if (operation == Operation.MOVE) {
                                if (!source.renameTo(resource)) {
                                    throw new RestletException("Rename operation failed.", Status.SERVER_ERROR_INTERNAL);
                                }
                            } else {
                                if (source.getType() == Type.DIRECTORY) {
                                    throw new RestletException("Cannot copy directory.", Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
                                }
                                IOUtils.copyStream(source.in(), resource.out(), true, true);
                            }
                        } catch (IllegalStateException e) {
                            throw new RestletException("I/O Error", Status.SERVER_ERROR_INTERNAL, e);
                        }
                    }
                } else {                
                    IOUtils.copyStream(getRequest().getEntity().getStream(), resource.out(), true, true);
                }
                
                if (isNew) {
                    getResponse().setStatus(Status.SUCCESS_CREATED);
                }
            } catch (IOException e) {
                throw new RestletException("I/O Error", Status.SERVER_ERROR_INTERNAL, e);
            }
        }
    }    
    
    @Override
    public boolean allowDelete() {        
        return true;
    }
    
    @Override
    public void handleDelete() {
        if (resource.getType() == Type.UNDEFINED) {
            throw new RestletException("Undefined resource path.", Status.CLIENT_ERROR_NOT_FOUND);
        } else {
            if (!resource.delete()) {
                throw new RestletException("Delete operation failed.", Status.SERVER_ERROR_INTERNAL);
            }
        }
    }
}
