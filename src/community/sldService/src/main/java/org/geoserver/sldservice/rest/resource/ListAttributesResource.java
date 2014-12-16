/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveHTMLFormat;
import org.geoserver.rest.format.ReflectiveJSONFormat;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.opengis.feature.type.PropertyDescriptor;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import freemarker.ext.beans.MapModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 * @author kappu
 * 
 *         Should get all Attributes related to a featureType we have internal
 *         Style abd external sld
 * 
 */
public class ListAttributesResource extends AbstractCatalogResource {

	public ListAttributesResource(Context context, Request request,
			Response response, Catalog catalog) {
		super(context, request, response, ListAttributesResource.class, catalog);
	}

	@Override
	protected Object handleObjectGet() {
		String layer = getAttribute("layer");

		if (layer == null) {
			// return all layers
			return new ArrayList();
		}

		LayerInfo layerInfo = catalog.getLayerByName(layer);
		if (layerInfo != null) {
			ResourceInfo obj = layerInfo.getResource();
			Collection<PropertyDescriptor> attributes = null;
			/* Check if it's feature type or coverage */
			if (obj instanceof FeatureTypeInfo) {
				FeatureTypeInfo fTpInfo;
				fTpInfo = (FeatureTypeInfo) obj;

				LayerAttributesList out = new LayerAttributesList(layer);
				try {
					attributes = fTpInfo.getFeatureType().getDescriptors();
					for (PropertyDescriptor attr : attributes) {
						out.addAttribute(attr.getName().getLocalPart(), attr.getType().getBinding().getSimpleName());
					}
				} catch (IOException e) {
					throw new RestletException("Error generating Attributes List!", Status.CLIENT_ERROR_BAD_REQUEST);
				}

				return out;
			}
		}

		return new ArrayList();
	}

	/**
	 * 
	 */
	@Override
	protected ReflectiveXMLFormat createXMLFormat(Request request,
			Response response) {
		return new ReflectiveXMLFormat() {

			@Override
			protected void write(Object data, OutputStream output)
					throws IOException {
				XStream xstream = new XStream();
				xstream.setMode(XStream.NO_REFERENCES);

				// Aliases
				xstream.alias("Attributes", LayerAttributesList.class);

				// Converters
				xstream.registerConverter(new LayerAttributesListConverter());

				// Marshalling
				xstream.toXML(data, output);
			}
		};
	}

	/**
	 * @see
	 * org.geoserver.catalog.rest.AbstractCatalogResource#createJSONFormat(org
	 * .restlet.data.Request, org.restlet.data.Response)
	 */
	@Override
	protected ReflectiveJSONFormat createJSONFormat(Request request,
			Response response) {
		return new ReflectiveJSONFormat() {

			/**
			 * @see
			 * org.geoserver.rest.format.ReflectiveJSONFormat#write(java.lang
			 * .Object, java.io.OutputStream)
			 */
			@Override
			protected void write(Object data, OutputStream output)
					throws IOException {
				XStream xstream = new XStream(new JettisonMappedXmlDriver());
				xstream.setMode(XStream.NO_REFERENCES);

				// Aliases
				xstream.alias("Attributes", LayerAttributesList.class);

				// Converters
				xstream.registerConverter(new LayerAttributesListConverter());

				// Marshalling
				xstream.toXML(data, new OutputStreamWriter(output, "UTF-8"));
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.geoserver.catalog.rest.CatalogResourceBase#createHTMLFormat(org.restlet
	 * .data.Request, org.restlet.data.Response)
	 */
	@Override
	protected DataFormat createHTMLFormat(Request request, Response response) {
		return new ReflectiveHTMLFormat(LayerAttributesList.class, request, response, this) {

			@Override
			protected Configuration createConfiguration(Object data, Class clazz) {
				final Configuration cfg = super.createConfiguration(data, clazz);
				cfg.setClassForTemplateLoading(getClass(), "templates");
				cfg.setObjectWrapper(new ObjectToMapWrapper<LayerAttributesList>(LayerAttributesList.class) {
	                @Override
	                protected void wrapInternal(Map properties, SimpleHash model, LayerAttributesList object) {
	                    properties.put( "attributes", new MapModel( object.attributes, new ObjectToMapWrapper(LayerAttributesList.class) ) );
	                }
	            });
				
				return cfg;
			}

		};
	}

	@Override
	public boolean allowPost() {
		return false;
	}

	@Override
	protected String handleObjectPost(Object object) {
		return null;
	}

	@Override
	protected void handleObjectPut(Object object) {
		// do nothing, we do not allow post
	}

	/**
	 * 
	 * @author Fabiani
	 * 
	 */
	public class LayerAttributesList {
		private String layerName;
		private Map<String, String> attributes = new HashMap<String, String>();

		public LayerAttributesList(final String layer) {
			layerName = layer;
		}

		public void addAttribute(final String name, final String type) {
			attributes.put(name, type);
		}

		public List<String> getAttributesNames() {
			List<String> out = new ArrayList<String>();

			for (String key : attributes.keySet()) {
				out.add(key);
			}

			return out;
		}

		public int getAttributesCount() {
			return attributes.size();
		}

		public String getAttributeName(final int index) {
			if (index >= getAttributesCount())
				return null;

			int cnt = 0;
			for (String key : attributes.keySet()) {
				if (index == cnt)
					return key;
				cnt++;
			}

			return null;
		}

		public String getAttributeType(final String name) {
			return attributes.get(name);
		}

		/**
		 * @return the layerName
		 */
		public String getLayerName() {
			return layerName;
		}
	}

	/**
	 * 
	 * @author Fabiani
	 * 
	 */
	public class LayerAttributesListConverter implements Converter {

		/**
		 * 
		 * @see
		 * com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java
		 * .lang.Class)
		 */
		public boolean canConvert(Class clazz) {
			return LayerAttributesList.class.isAssignableFrom(clazz);
		}

		/**
		 * 
		 * @see
		 * com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object
		 * , com.thoughtworks.xstream.io.HierarchicalStreamWriter,
		 * com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
			final LayerAttributesList obj = (LayerAttributesList) value;

			writer.addAttribute("layer", obj.getLayerName());

			for (int k = 0; k < obj.getAttributesCount(); k++) {
				writer.startNode("Attribute");
				final String name = obj.getAttributeName(k);
				final String type = obj.getAttributeType(name);

				writer.startNode("name");
				writer.setValue(name);
				writer.endNode();

				writer.startNode("type");
				writer.setValue(type);
				writer.endNode();
				writer.endNode();
			}
		}

		/**
		 * @see
		 * com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks
		 * .xstream.io.HierarchicalStreamReader,
		 * com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		public Object unmarshal(HierarchicalStreamReader arg0,
				UnmarshallingContext arg1) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
