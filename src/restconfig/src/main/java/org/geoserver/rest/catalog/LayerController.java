/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for managing GeoServer Layers. */
@RestController
@ControllerAdvice
@RequestMapping(
    path = {
        RestBaseController.ROOT_PATH + "/layers",
        RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/layers"
    }
)
public class LayerController extends AbstractCatalogController {
    private static final Logger LOGGER = Logging.getLogger(LayerController.class);

    @Autowired
    public LayerController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    /**
     * All layers as JSON, XML or HTML.
     *
     * @return All layers
     */
    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<LayerInfo> layersGet(@PathVariable(required = false) String workspaceName) {

        List<LayerInfo> layers;
        if (workspaceName == null) {
            layers = catalog.getLayers();
        } else {
            layers = new ArrayList<>();
            for (ResourceInfo resourceInfo :
                    catalog.getResourcesByNamespace(workspaceName, ResourceInfo.class)) {
                layers.addAll(catalog.getLayers(resourceInfo));
            }
        }
        return wrapList(layers, LayerInfo.class);
    }

    /**
     * A single layer as JSON, XML or HTML.
     *
     * @return A single layer
     */
    @GetMapping(
        path = "/{layerName}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<LayerInfo> layerGet(
            @PathVariable String layerName, @PathVariable(required = false) String workspaceName) {

        if (workspaceName != null) {
            layerName = workspaceName + ":" + layerName;
        }
        LayerInfo layer = catalog.getLayerByName(layerName);
        if (layer == null) {
            throw new ResourceNotFoundException("No such layer: " + layerName);
        }
        return wrapObject(layer, LayerInfo.class);
    }

    @DeleteMapping(value = "/{layerName}")
    public void layerDelete(
            @PathVariable String layerName,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(name = "recurse", required = false, defaultValue = "false")
                    boolean recurse)
            throws IOException {

        if (workspaceName != null) {
            layerName = workspaceName + ":" + layerName;
        }

        LayerInfo layer = catalog.getLayerByName(layerName);
        if (layer == null) {
            throw new ResourceNotFoundException(layerName);
        }
        if (!recurse) {
            catalog.remove(layer);
            LOGGER.info("DELETE layer '" + layerName + "'");
        } else {
            new CascadeDeleteVisitor(catalog).visit(layer);
            LOGGER.info("DELETE layer '" + layerName + "' recurse=true");
        }
    }

    @PutMapping(value = "/{layerName}")
    public void layerPut(
            @RequestBody LayerInfo layer,
            @PathVariable String layerName,
            @PathVariable(required = false) String workspaceName) {

        if (workspaceName != null) {
            layerName = workspaceName + ":" + layerName;
        }

        LayerInfo original = catalog.getLayerByName(layerName);

        // ensure this is not a name change
        // TODO: Uncomment this when the resource/layer split is not, now by definition
        // we cannot rename a layer, it's just not possible and it's not un-marshalled either
        //        if ( layer.getName() != null && !layer.getName().equals( original.getName() ) ) {
        //            throw new RestletException( "Can't change name of a layer",
        // Status.CLIENT_ERROR_FORBIDDEN );
        //        }

        // force in the same resource otherwise the update will simply fail as we cannot reach the
        // name
        layer.setResource(original.getResource());

        CatalogBuilder session = new CatalogBuilder(catalog);
        session.updateLayer(original, layer);
        catalog.save(original);

        LOGGER.info("PUT layer " + layerName);
    }

    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return LayerInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    //
    // Configuration and Settings
    //
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<LayerInfo> getObjectClass() {
                        return LayerInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        @SuppressWarnings("unchecked")
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String layerName = uriTemplateVars.get("layerName");
                        if (layerName == null) {
                            return null;
                        }
                        return catalog.getLayerByName(layerName);
                    }

                    @Override
                    protected void postEncodeReference(
                            Object obj,
                            String ref,
                            String prefix,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        if (obj instanceof StyleInfo) {
                            StyleInfo style = (StyleInfo) obj;
                            StringBuilder link = new StringBuilder();
                            if (style.getWorkspace() != null) {
                                String wsName = style.getWorkspace().getName();
                                writer.startNode("workspace");
                                writer.setValue(wsName);
                                writer.endNode();
                                link.append("/workspaces/").append(converter.encode(wsName));
                            }
                            link.append("/styles/").append(converter.encode(style.getName()));
                            converter.encodeLink(link.toString(), writer);
                        }
                        if (obj instanceof ResourceInfo) {
                            ResourceInfo r = (ResourceInfo) obj;
                            StringBuilder link =
                                    new StringBuilder("/workspaces/")
                                            .append(
                                                    converter.encode(
                                                            r.getStore().getWorkspace().getName()))
                                            .append("/");

                            if (r instanceof FeatureTypeInfo) {
                                link.append("datastores/")
                                        .append(converter.encode(r.getStore().getName()))
                                        .append("/featuretypes/");
                            } else if (r instanceof CoverageInfo) {
                                link.append("coveragestores/")
                                        .append(converter.encode(r.getStore().getName()))
                                        .append("/coverages/");
                            } else if (r instanceof WMSLayerInfo) {
                                link.append("wmsstores/")
                                        .append(converter.encode(r.getStore().getName()))
                                        .append("/wmslayers/");
                            } else {
                                return;
                            }

                            link.append(converter.encode(r.getName()));
                            converter.encodeLink(link.toString(), writer);
                        }
                    }
                });
    }
}
