/*
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.sldservice.rest.resource;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

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
import org.geoserver.sldservice.utils.classifier.ColorRamp;
import org.geoserver.sldservice.utils.classifier.RulesBuilder;
import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.CustomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RandomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RedColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.JetColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.GrayColorRamp;
import org.geotools.feature.FeatureCollection;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDTransformer;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.type.FeatureType;
import org.restlet.Context;
import org.restlet.data.Form;
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
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class ClassifierResource extends AbstractCatalogResource {
	private final static Logger LOGGER = Logger.getLogger(ClassifierResource.class.toString());
	
	final private RulesBuilder builder = new RulesBuilder();

	public ClassifierResource(Context context, Request request, Response response, Catalog catalog) {
		super(context, request, response, ClassifierResource.class, catalog);
	}

	@Override
	protected Object handleObjectGet() throws Exception {
		final Request req = getRequest();
		final Map<String,Object> attributes = req.getAttributes();
		final Form parameters = req.getResourceRef().getQueryAsForm();
		
		final String layer = getAttribute("layer");

		if (layer == null) {
			return new ArrayList();
		}

		final LayerInfo layerInfo = catalog.getLayerByName(layer);
		if (layerInfo != null) {
			final List<Rule> rules = this.generateClassifiedSLD(attributes, parameters);
			RulesList jsonRules = null;

			if (rules != null)
				jsonRules = generateRulesList(layer, getRequest(), rules);

			if (jsonRules != null) {
				return jsonRules;
			} else {
				throw new RestletException("Error generating Classification!", Status.CLIENT_ERROR_BAD_REQUEST);
			}
		}
		
		return new ArrayList();
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

	private RulesList generateRulesList(String layer, Request req, List<Rule> rules) {
		final RulesList ruleList = new RulesList(layer);
		for (Rule rule : rules) {
			ruleList.addRule(jsonRule(rule));
		}

		return ruleList;
	}

	/**
     * 
     */
	@Override
	protected ReflectiveXMLFormat createXMLFormat(Request request, Response response) {
		return new ReflectiveXMLFormat() {

			@Override
			protected void write(Object data, OutputStream output)
					throws IOException {
				XStream xstream = new XStream();
				xstream.setMode(XStream.NO_REFERENCES);

				// Aliases
				xstream.alias("Rules", RulesList.class);

				// Converters
				xstream.registerConverter(new RulesListConverter());

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
	protected ReflectiveJSONFormat createJSONFormat(Request request, Response response) {
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
				xstream.alias("Rules", RulesList.class);

				// Converters
				xstream.registerConverter(new RulesListConverter());

				// Marshalling
				xstream.toXML(data, new OutputStreamWriter(output, "UTF-8"));
			}
		};
	}

	/**
	 * 
	 * @see
	 * org.geoserver.catalog.rest.CatalogResourceBase#createHTMLFormat(org.restlet
	 * .data.Request, org.restlet.data.Response)
	 */
	@Override
	protected DataFormat createHTMLFormat(Request request, Response response) {
		return new ReflectiveHTMLFormat(RulesList.class, request, response, this) {

			@Override
			protected Configuration createConfiguration(Object data, Class clazz) {
				final Configuration cfg = super.createConfiguration(data, clazz);
				cfg.setClassForTemplateLoading(getClass(), "templates");
				cfg.setObjectWrapper(new ObjectToMapWrapper<RulesList>(RulesList.class) {
	                @Override
	                protected void wrapInternal(Map properties, SimpleHash model, RulesList object) {
	                    properties.put( "rules", new CollectionModel( object.rules, new ObjectToMapWrapper(RulesList.class) ) );
	                }
	            });
				return cfg;
			}

		};
	}

	/**
	 * 
	 * @param rule
	 * @return a string with json Rule representation
	 */
	private JSONObject jsonRule(Rule rule) {

		JSONObject ruleSz = null;
		String xmlRule;
		XMLSerializer xmlS = new XMLSerializer();

		SLDTransformer transform = new SLDTransformer();
		transform.setIndentation(2);
		try {
			xmlRule = transform.transform(rule);
			xmlS.setRemoveNamespacePrefixFromElements(true);
			xmlS.setSkipNamespaces(true);
			ruleSz = (JSONObject) xmlS.read(xmlRule);
		} catch (TransformerException e) {
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Exception occurred while transformin the Rule " 
						+ e.getLocalizedMessage(),e);
		}

		return ruleSz;
	}

	private List<Rule> generateClassifiedSLD(Map<String,Object> attributes, Form form) {
		/* Looks in attribute map if there is the featureType param */
		if (attributes.containsKey("layer")) {
			final String layerName = (String) attributes.get("layer");

			final String property = form.getFirstValue("attribute");
			final String method = form.getFirstValue("method", "equalInterval");
			final String intervals = form.getFirstValue("intervals", "2");
			final String open = form.getFirstValue("open", "false");
			final String colorRamp = form.getFirstValue("ramp");
            final boolean reverse = Boolean.parseBoolean(form.getFirstValue("reverse"));

			if (property != null && property.length() > 0) {
				/* First try to find as a FeatureType */
				try {
					LayerInfo layerInfo = catalog.getLayerByName(layerName);
					if (layerInfo != null) {
						ResourceInfo obj = layerInfo.getResource();
						/* Check if it's feature type or coverage */
						if (obj instanceof FeatureTypeInfo) {
							final FeatureType ftType = ((FeatureTypeInfo) obj).getFeatureType();
							final FeatureCollection ftCollection = ((FeatureTypeInfo) obj).getFeatureSource(new NullProgressListener(), null).getFeatures();
							List<Rule> rules = null;

							if ("equalInterval".equals(method)) {
								rules = builder.equalIntervalClassification(ftCollection, property, Integer.parseInt(intervals), Boolean.parseBoolean(open));
							} else if ("uniqueInterval".equals(method)) {
								rules = builder.uniqueIntervalClassification(ftCollection, property);
							} else if ("quantile".equals(method)) {
								rules = builder.quantileClassification(ftCollection, property, Integer.parseInt(intervals), Boolean.parseBoolean(open));
							} else if ("jenks".equals(method)) {
                                rules = builder.jenksClassification(ftCollection, property, Integer.parseInt(intervals), Boolean.parseBoolean(open));
							}

							if (colorRamp != null && colorRamp.length() > 0) {
								ColorRamp ramp = null;
								if (colorRamp.equalsIgnoreCase("random"))
									ramp = (ColorRamp) new RandomColorRamp();
								else if (colorRamp.equalsIgnoreCase("red"))
									ramp = (ColorRamp) new RedColorRamp();
								else if (colorRamp.equalsIgnoreCase("blue"))
									ramp = (ColorRamp) new BlueColorRamp();
                                else if (colorRamp.equalsIgnoreCase("jet"))
                                    ramp = (ColorRamp) new JetColorRamp();
                                else if (colorRamp.equalsIgnoreCase("gray"))
                                    ramp = (ColorRamp) new GrayColorRamp();
								else if (colorRamp.equalsIgnoreCase("custom")) {
									Color startColor = Color.decode(form.getFirst("startColor").getValue());
									Color endColor = Color.decode(form.getFirst("endColor").getValue());
									Color midColor = (form.contains("midColor") ? Color.decode(form.getFirst("midColor").getValue()) : null);
									if (startColor != null && endColor != null) {
										CustomColorRamp tramp = new CustomColorRamp();
										tramp.setStartColor(startColor);
										tramp.setEndColor(endColor);
										if (midColor != null)
											tramp.setMid(midColor);
										ramp = (ColorRamp) tramp;
									}
								}

								final Class geomT = ftType.getGeometryDescriptor().getType().getBinding();

								/*
								 * Line Symbolizer
								 */
								if (geomT == LineString.class || geomT == MultiLineString.class) {
									builder.lineStyle(rules, ramp, reverse);
								}

								/*
								 * Polygon Symbolyzer
								 */
								else if (geomT == MultiPolygon.class
										|| geomT == Polygon.class
										|| geomT == Point.class
										|| geomT == MultiPoint.class) {
									builder.polygonStyle(rules, ramp, reverse);
								}
							}

							return rules;
						}
					}
				} catch (NoSuchElementException e) {
					if (LOGGER.isLoggable(Level.FINE))
						LOGGER.log(Level.FINE,"The following exception has occurred " 
								+ e.getLocalizedMessage(), e);
					return null;
				} catch (IOException e) {
					if (LOGGER.isLoggable(Level.FINE))
						LOGGER.log(Level.FINE, "The following exception has occurred " 
								+ e.getLocalizedMessage(), e);
					return null;
				}
			} else
				return null;
		} else
			return null;

		return null;
	}

	/**
	 * 
	 * @author Fabiani
	 * 
	 */
	public class RulesList {
		private String layerName;
		private List<JSONObject> rules = new ArrayList<JSONObject>();

		public RulesList(final String layer) {
			setLayerName(layer);
		}
		
		public void addRule(JSONObject object) {
			rules.add(object);
		}

		public List<JSONObject> getRules() {
			return rules;
		}

		/**
		 * @param layerName the layerName to set
		 */
		public void setLayerName(String layerName) {
			this.layerName = layerName;
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
	public class RulesListConverter implements Converter {

		/**
		 * @see
		 * com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java
		 * .lang.Class)
		 */
		public boolean canConvert(Class clazz) {
			return RulesList.class.isAssignableFrom(clazz);
		}

		/**
		 * @see
		 * com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object
		 * , com.thoughtworks.xstream.io.HierarchicalStreamWriter,
		 * com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		public void marshal(Object value, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			RulesList obj = (RulesList) value;

			for (JSONObject rule : obj.getRules()) {
				if (!rule.isEmpty() && !rule.isNullObject() && !rule.isArray()) {
					writer.startNode("Rule");
					for (Object key : rule.keySet()) {
						writer.startNode((String) key);
						writeChild(writer, rule.get(key));
						writer.endNode();
					}
					writer.endNode();
				}
			}
		}

		private void writeChild(HierarchicalStreamWriter writer, Object object) {
			if (object instanceof JSONObject && !((JSONObject) object).isArray()) {
				for (Object key : ((JSONObject) object).keySet()) {
					final Object obj = ((JSONObject) object).get(key);
					if (obj instanceof JSONArray) {
						for (int i = 0; i < ((JSONArray) obj).size(); i++) {
							final JSONObject child = (JSONObject) ((JSONArray) obj).get(i);
							writer.startNode((String) key);
							for (Object cKey : child.keySet()) {
								if (!((String) cKey).startsWith("@")) {
									writer.startNode((String) cKey);
									writeChild(writer, child.get(cKey));
									writer.endNode();
								} else {
									writer.addAttribute(((String) cKey).substring(1), (String) child.get(cKey));
								}
							}
							writer.endNode();
						}
                    }
                    else if (((String)key).startsWith("@")){
                        writer.addAttribute(((String) key).substring(1), (String)obj);
                    }
                    else if (((String)key).startsWith("#text")){
                        writer.setValue((String) obj);
					} else {
						writer.startNode((String) key);
						writeChild(writer, obj);
						writer.endNode();
					}
				}
			} else if (object instanceof JSONArray) {
				for (int i = 0; i < ((JSONArray) object).size(); i++) {
					final Object child = ((JSONArray) object).get(i);
					if (child instanceof JSONObject) {
						for (Object key : ((JSONObject) child).keySet()) {
							if (((JSONObject) child).get(key) instanceof String)
								writer.addAttribute((String) key, (String) ((JSONObject) child).get(key));
							else
								writeChild(writer, ((JSONObject) child).get(key));
						}

					} else {
						writeChild(writer, child);
					}
				}
			} else {
				writer.setValue(object.toString());
			}
		}

		/**
		 * 
		 * @seecom.thoughtworks.xstream.converters.Converter#unmarshal(com.
		 * thoughtworks.xstream.io. HierarchicalStreamReader,
		 * com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		public Object unmarshal(HierarchicalStreamReader arg0,
				UnmarshallingContext arg1) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
