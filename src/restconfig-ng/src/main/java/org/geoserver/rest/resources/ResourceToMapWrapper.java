package org.geoserver.rest.resources;

import freemarker.ext.beans.MapModel;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.geoserver.rest.ObjectToMapWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Freemarker {@link freemarker.template.ObjectWrapper} for
 * {@link org.geoserver.rest.resources.ResourceController.ResourceReference}.
 */
class ResourceToMapWrapper<T> extends ObjectToMapWrapper<T> {
    private final Class<T> clazz;

    public ResourceToMapWrapper(Class<T> clazz) {
        super(clazz);
        this.clazz = clazz;
    }

    @Override
    public TemplateModel wrap(Object object) throws TemplateModelException {
        Map<String, Object> map;
        if (object instanceof ResourceController.ResourceDirectory) {
            map = wrapResourceDirectory((ResourceController.ResourceDirectory) object);
        } else if (object instanceof ResourceController.ResourceMetadata) {
            map = wrapResource((ResourceController.ResourceMetadata) object);
        } else {
            return super.wrap(object);
        }
        SimpleHash model = new SimpleHash();
        model.put("properties", new MapModel(map, this));
        model.put("className", clazz.getSimpleName());
        setRequestInfo(model);
        wrapInternal(map, model, (T) object);
        return model;
    }

    private Map<String, Object> wrapResourceDirectory(ResourceController.ResourceDirectory resource) {
        Map<String, Object> map = wrapResource(resource);
        List<Map<String, Object>> children = new ArrayList<>();
        for (ResourceController.ResourceChild child : resource.getChildren()) {
            children.add(wrapResourceChild(child));
        }
        map.put("children", children);
        return map;
    }

    private Map<String, Object> wrapResource(ResourceController.ResourceMetadata resource) {
        if (resource == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("path", "/"+resource.getPath());
        map.put("href", resource.getHref());
        map.put("parent", wrapResourceReference(resource.getParent()));
        map.put("name", resource.getName());
        map.put("lastModified", resource.getLastModified().toString());
        map.put("type", resource.getType());

        return map;
    }

    private Map<String, Object> wrapResourceChild(ResourceController.ResourceChild resource) {
        Map<String, Object> map = wrapResource(resource);
        map.put("name", resource.getName());
        return map;
    }

    private Map<String, Object> wrapResourceReference(ResourceController.ResourceReference resource) {
        if (resource == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();

        map.put("path", "/"+resource.getPath());
        map.put("href", resource.getHref());

        return map;
    }
}
