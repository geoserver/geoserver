/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest;

/**
 * @author Morgan Thompson - Boundless
 */
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.platform.RenderingEngineStatus;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import com.thoughtworks.xstream.XStream;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class AboutStatus extends ReflectiveResource {

    static String target;

    public AboutStatus(Context context, Request request, Response response, String module) {
        super(context, request, response);
        target = module;
    }

    @Override
    public boolean allowPut() {
        return false;
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new StatusHTMLFormat(request, response, this);
    }

    protected static Predicate<ModuleStatus> getModule() {
        return m -> m.getModule().equalsIgnoreCase(target);
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        GeoServerExtensions gse = new GeoServerExtensions();
        List<ModuleStatus> applicationStatus;
        if (target != null) {
             applicationStatus = gse.extensions(ModuleStatus.class).stream().map(ModuleStatusImpl::new).filter(getModule()).collect(Collectors.toList());
             if (applicationStatus.isEmpty()) {
                 throw new RestletException( "No such module: " + target, Status.CLIENT_ERROR_NOT_FOUND );
             }
        } else {
            applicationStatus = gse.extensions(ModuleStatus.class).stream()
                .map(ModuleStatusImpl::new).collect(Collectors.toList());
        }
        return applicationStatus;
    }

    @Override
    protected void configureXStream(XStream xs) {
        xs.processAnnotations(ModuleStatus.class);
        xs.allowTypes(new Class[] { ModuleStatus.class });
        xs.alias("about", List.class);
        xs.alias("status", ModuleStatus.class);
        xs.addDefaultImplementation(ModuleStatusImpl.class, ModuleStatus.class);
        xs.addDefaultImplementation(RenderingEngineStatus.class, ModuleStatus.class);
    }

    @Override
    protected void handleObjectPut(Object obj) throws Exception {
        throw new UnsupportedOperationException("Not allowed");
    }

    private static class StatusHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public StatusHTMLFormat(Request request, Response response, Resource resource) {
            super(ModuleStatusImpl.class, request, response, resource);
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, ModuleStatusImpl.class);
            cfg.setClassForTemplateLoading(getClass(), "templates");
            cfg.setObjectWrapper(new BeansWrapper() {
                @Override
                public TemplateModel wrap(Object obj) throws TemplateModelException {
                    if (obj instanceof List) { // we expect List of ModuleStatus
                        List<?> list = (List<?>) obj;
                        SimpleHash hash = new SimpleHash();
                        hash.put("values", new CollectionModel(list, new BeansWrapper() {
                            public TemplateModel wrap(Object object) throws TemplateModelException {
                                if (object instanceof ModuleStatus) {
                                    ModuleStatus status = (ModuleStatus) object;
                                    SimpleHash hash = new SimpleHash();
                                    hash.put("module", status.getModule());
                                    hash.put("name", status.getName());
                                    hash.put("isAvailable", Boolean.toString(status.isAvailable()));
                                    hash.put("isEnabled", Boolean.toString(status.isEnabled()));
                                    status.getComponent().ifPresent(component -> {
                                        hash.put("component", component);
                                    });
                                    status.getVersion().ifPresent(version -> {
                                        hash.put("version", version);
                                    });
                                    status.getMessage().ifPresent(message -> {
                                        hash.put("message", message.replace("\n", "<br/>"));
                                    });

                                    return hash;
                                }
                                return super.wrap(object);
                            };
                        }));
                        return hash;
                    }
                    return super.wrap(obj);
                }
            });
            return cfg;
        }
    }
}
