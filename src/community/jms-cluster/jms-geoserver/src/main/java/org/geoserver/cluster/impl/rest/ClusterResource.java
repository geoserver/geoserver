/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cluster.impl.rest;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

import java.util.Map;
import java.util.Properties;

import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.cluster.configuration.BrokerConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.configuration.ToggleConfiguration;
import org.geoserver.cluster.events.ToggleType;
import org.geoserver.rest.ReflectiveResource;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * 
 * @author Carlo Cancellieri - GeoSolutions SAS
 * 
 */
public class ClusterResource extends ReflectiveResource {

	public final transient Controller controller;

	public final transient JMSConfiguration config;

	public ClusterResource(Context context, Request request, Response response,
			Controller controller, JMSConfiguration config) {
		super(context, request, response);
		this.controller = controller;
		this.config = config;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	protected Object handleObjectGet() throws Exception {
		return config.getConfigurations();
	}

	/**
	 * <code> 
	 * curl -u admin:geoserver -X POST -H "Content-type: text/xml" 
	 * http://localhost:8080/geoserver/rest/cluster 
	 * -d "<properties><property name=\"brokerURL\" value=\"tcp://localhost:61616\"/><property name=\"instanceName\" value=\"7fcc646c-3c34-4814-831d-c9c289379201\"/><property name=\"connection\" value=\"disabled\"/><property name=\"topicName\" value=\"VirtualTopic.&gt;\"/><property name=\"CLUSTER_CONFIG_DIR\" value=\"/home/carlo/work/code/java/geoserver-enterprise/src/web/app/src/main/webapp/data/cluster\"/><property name=\"toggleSlave\" value=\"false\"/><property name=\"readOnly\" value=\"disabled\"/><property name=\"toggleMaster\" value=\"true\"/></properties>"
	 * </code>
	 */
	@Override
	protected String handleObjectPost(Object obj) throws Exception {
		Properties props = (Properties) obj;
		for (Object key : props.keySet()) {
			String k = key.toString();
			final String value = props.get(key).toString();
			// store config
			config.putConfiguration(key.toString(), value);
			final Object oldValue = config.getConfiguration(k);
			if (props.get(k).equals(oldValue))
				continue;

			if (key.equals(ConnectionConfiguration.CONNECTION_KEY)) {
				// CONNECTION
				controller.connectClient(Boolean.getBoolean(value));
			} else if (key.equals(ToggleConfiguration.TOGGLE_MASTER_KEY)) {
				// toggle MASTER
				controller.toggle(Boolean.getBoolean(value), ToggleType.MASTER);
			} else if (key.equals(ToggleConfiguration.TOGGLE_SLAVE_KEY)) {
				// toggle SLAVE
				controller.toggle(Boolean.getBoolean(value), ToggleType.SLAVE);
			} else if (key.equals(JMSConfiguration.INSTANCE_NAME_KEY)) {
				// InstanceName
				controller.setInstanceName(value);
			} else if (key.equals(BrokerConfiguration.BROKER_URL_KEY)) {
				// BROKER_URL
				controller.setBrokerURL(value);
			} else if (key.equals(ReadOnlyConfiguration.READ_ONLY_KEY)) {
				// ReadOnly
				controller.setReadOnly(Boolean.getBoolean(value));
			}else if (key.equals(JMSConfiguration.GROUP_KEY)) {
				// group
				controller.setGroup(value);
			} 
		}
		// SAVE to disk
		controller.save();
		return obj.toString();
	}

	@Override
	protected DataFormat createHTMLFormat(Request request, Response response) {
		return new JMSConfigHTMLFormat(request, response, this);
	}

	/**
	 * HTML format
	 * 
	 * @author carlo cancellieri - GeoSolutions SAS
	 * 
	 */
	private class JMSConfigHTMLFormat extends CatalogFreemarkerHTMLFormat {

		public JMSConfigHTMLFormat(Request request, Response response,
				Resource resource) {
			super(ClusterResource.class, request, response, resource);
		}

		@Override
		protected Configuration createConfiguration(Object data, Class clazz) {
			Configuration cfg = new Configuration();
			cfg.setClassForTemplateLoading(ClusterResource.class, "templates");

			cfg.setObjectWrapper(new ObjectToMapWrapper<Properties>(
					Properties.class) {
				@Override
				protected void wrapInternal(Map properties, SimpleHash model,
						Properties props) {
					properties.putAll(props);
				}
			});
			return cfg;
		}
	}

}
