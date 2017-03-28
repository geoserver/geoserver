package org.geoserver.catalog.rest;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import freemarker.template.*;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.rest.wrapper.RestWrapperAdapter;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@RestController
@RequestMapping(path = "/restng", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
		MediaType.TEXT_HTML_VALUE })
public class WorkspaceController extends CatalogController {

	private static final Logger LOGGER = Logging.getLogger(WorkspaceController.class);

	@Autowired
	public WorkspaceController(Catalog catalog) {
		super(catalog);
		// TODO Auto-generated constructor stub
	}

	@GetMapping(value = "/workspaces", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
			MediaType.TEXT_HTML_VALUE })
	public RestWrapper getWorkspaces() {

		List<WorkspaceInfo> wkspaces = catalog.getWorkspaces();
		return wrapList(wkspaces, WorkspaceInfo.class);
	}

	@GetMapping(value = "/workspaces/{workspaceName}", produces = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XML_VALUE })
	public RestWrapper<WorkspaceInfo> getWorkspace(@PathVariable String workspaceName) {

		WorkspaceInfo wkspace = catalog.getWorkspaceByName(workspaceName);
		LOGGER.info("GET " + workspaceName);
		LOGGER.info("got " + wkspace.getName());

		if (wkspace == null) {
			throw new ResourceNotFoundException("No such workspace: " + workspaceName + " found");
		}
		return wrapObject(wkspace, WorkspaceInfo.class);
	}

	@PostMapping(value = "/workspaces", consumes = { "text/xml", MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<String> postWorkspace(@RequestBody WorkspaceInfo workspace,
												@RequestParam(defaultValue = "false", name = "default") boolean makeDefault, UriComponentsBuilder builder) {
		catalog.add(workspace);
		String name = workspace.getName();
		LOGGER.info("Added workspace " + name);
		if (makeDefault) {
			catalog.setDefaultWorkspace(workspace);
			LOGGER.info("made workspace " + name + " default");
		}
		LOGGER.info("POST Style " + name);

		// build the new path
		UriComponents uriComponents = getUriComponents(name, builder);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(uriComponents.toUri());
		return new ResponseEntity<String>(name, headers, HttpStatus.CREATED);
	}

	private UriComponents getUriComponents(String name, UriComponentsBuilder builder) {
		UriComponents uriComponents;

		uriComponents = builder.path("/workspaces/{id}").buildAndExpand(name);

		return uriComponents;
	}

	public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {

		template.getConfiguration().setObjectWrapper(new ObjectToMapWrapper(WorkspaceInfo.class) {

			@Override
			protected void wrapInternal(Map properties, SimpleHash model, Object object) {
				Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder.getRequestAttributes().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
				String workspace = uriTemplateVars.get("workspaceName");

				WorkspaceInfo wkspace = (WorkspaceInfo)object;

				List<Map<String, Map<String, String>>> dsProps = new ArrayList<>();

				List<DataStoreInfo> datasources = catalog.getDataStoresByWorkspace(wkspace);
				for (DataStoreInfo ds : datasources) {
					Map<String, String> names = new HashMap<>();
					names.put("name", ds.getName());
					dsProps.add(Collections.singletonMap("properties", names));
				}
				if (!dsProps.isEmpty())
					properties.putIfAbsent("dataStores", dsProps);

				dsProps = new ArrayList<>();

				List<CoverageStoreInfo> coverages = catalog.getCoverageStoresByWorkspace(wkspace);
				for (CoverageStoreInfo ds : coverages) {
					Map<String, String> names = new HashMap<>();
					names.put("name", ds.getName());
					dsProps.add(Collections.singletonMap("properties", names));
				}
				if (!dsProps.isEmpty())
					properties.putIfAbsent("coverageStores", dsProps);

				dsProps = new ArrayList<>();

				List<WMSStoreInfo> wmssources = catalog.getStoresByWorkspace(wkspace, WMSStoreInfo.class);
				for (WMSStoreInfo ds : wmssources) {
					Map<String, String> names = new HashMap<>();
					names.put("name", ds.getName());
					dsProps.add(Collections.singletonMap("properties", names));
				}
				if (!dsProps.isEmpty())
					properties.putIfAbsent("wmsStores", dsProps);
			}

		});
	}

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
		return WorkspaceInfo.class.isAssignableFrom(methodParameter.getParameterType());
	}

	@Override
	public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
		persister.setCallback(new XStreamPersister.Callback() {
			@Override
			protected Class<WorkspaceInfo> getObjectClass() {
				return WorkspaceInfo.class;
			}

			@Override
			protected CatalogInfo getCatalogObject() {
				Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder.getRequestAttributes().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
				String workspace = uriTemplateVars.get("workspaceName");

				if (workspace == null) {
					return null;
				}
				return catalog.getWorkspaceByName(workspace);
			}

			@Override
			protected void postEncodeWorkspace(WorkspaceInfo cs, HierarchicalStreamWriter writer,
											   MarshallingContext context) {

				// add a link to the datastores
				writer.startNode("dataStores");
				converter.encodeCollectionLink("datastores", writer);
				writer.endNode();

				writer.startNode("coverageStores");
				converter.encodeCollectionLink("coveragestores", writer);
				writer.endNode();

				writer.startNode("wmsStores");
				converter.encodeCollectionLink("wmsstores", writer);
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


	@Override
	protected String getTemplateName(Object object) {
		if (object instanceof WorkspaceInfo) {
			return "WorkspaceInfo";
		}
		return null;
	}

}
