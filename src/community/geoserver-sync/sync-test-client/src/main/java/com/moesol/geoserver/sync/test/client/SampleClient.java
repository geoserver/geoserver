/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.test.client;




import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.identity.Identifier;

import com.moesol.geoserver.sync.client.FeatureAccessor;
import com.moesol.geoserver.sync.client.GeoserverClientSynchronizer;
import com.moesol.geoserver.sync.client.xml.ComplexConfiguration;

/**
 * Reconcile with the states feature type that
 * is a sample feature type in geoserver
 * @author hastings
 */
public class SampleClient implements Runnable {
	private static final Logger LOGGER = Logging.getLogger(SampleClient.class.getName());
	private Map<Identifier, FeatureAccessor> m_features = new HashMap<Identifier, FeatureAccessor>();
	private GeoserverClientSynchronizer synchronizer;
	private String m_postTemplate = "<wfs:GetFeature " 
        + "service=\"WFS\" " 
        + "version=\"1.1.0\" "
        + "outputFormat=\"${outputFormat}\" "
        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + ">\n"
        +  "<wfs:Query typeName=\"topp:states\"> "
        +   "<ogc:Filter>"
        +    "<ogc:PropertyIsEqualTo> "
        +      "<ogc:Function name=\"sha1Sync\"> "
        +       "<ogc:Literal>-all</ogc:Literal> "
        +       "<ogc:Literal>${sha1Sync}</ogc:Literal> "
        +      "</ogc:Function> "
        +      "<ogc:Literal>true</ogc:Literal> "
        +    "</ogc:PropertyIsEqualTo> "
        +   "</ogc:Filter> "
        +  "</wfs:Query> " 
        + "</wfs:GetFeature>";
	private String m_namespace;
	private Properties m_properties = new Properties();
	private long m_cycleMillis = 10000;
	private String m_printProperty;
	private ScheduledExecutorService m_scheduler;
	
	public static void main(String args[]) throws IOException, InterruptedException {
		SampleClient client = new SampleClient();
		client.setUp();
		try {
			client.runCommandLoop();
		} finally {
			client.tearDown();
		}
	}

	private void runCommandLoop() throws IOException, InterruptedException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.print("? ");
			System.out.flush();
			while (System.in.available() < 1) {
				Thread.sleep(100);
				continue;
			}
			String line = br.readLine();
			if (line.length() < 1) {
				continue;
			}
			char c = line.charAt(0);
			try {
				processCommand(c);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private void processCommand(char c) {
		switch (c) {
		case 'p':
			dumpFids();
			break;
			
		case 'c':
			count();
			break;
			
		case 'd':
			deleteAll();
			break;
			
		case 'r':
			randomDelete();
			break;
			
		case 's':
			syncNow();
			break;
			
		case 'q':
			System.exit(0);
			break;
			
		case '?':
		default:
			System.out.println("p - print");
			System.out.println("c - count");
			System.out.println("d - delete ALL");
			System.out.println("r - random delete P=0.5");
			System.out.println("s - sync now");
			System.out.println("q = quit");
			System.out.println("? = help");
			break;
		}
	}

	private void syncNow() {
		try {
			System.out.println("SYNC NOW START");
			synchronizer.synchronize(m_features);
			System.out.println("SYNC NOW COMPLETE");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void count() {
		System.out.println("count: " + m_features.size());
	}

	private void randomDelete() {
//		m_scheduler.execute(new Runnable() {
//			@Override
//			public void run() {
				Random random = new Random();
				Set<Identifier> keys = new HashSet<Identifier>(m_features.keySet());
				int n = 0;
				for (Identifier key : keys) {
					if (random.nextDouble() < 0.5) {
						m_features.remove(key);
						n++;
					}
				}
				System.out.println("deleted: " + n);
//			}
//		});
	}

	private void deleteAll() {
//		m_scheduler.schedule(new Runnable() {
//			@Override
//			public void run() {
				m_features.clear();
				System.out.println("deleted all");
//			}
//		}, 5L, TimeUnit.MILLISECONDS);
	}

	private void setUp() throws IOException {
		maybeLoadPostTemplateFromFile();
		maybeLoadProperties();
		LOGGER.log(Level.INFO, "template {0}", m_postTemplate);
		
		m_namespace = m_properties.getProperty("namespace", "http://www.openplans.org/topp");
		String schemaLocation = m_properties.getProperty(
				"schemaLocation", 
				"http://localhost/geoserver/ows?service=WFS&version=1.0.0&request=DescribeFeatureType&typeName=topp:states&maxFeatures=50");
		ComplexConfiguration configuration = new ComplexConfiguration(m_namespace, schemaLocation);

		String wfsUrl = m_properties.getProperty("url", "http://localhost:80/geoserver/wfs");
		synchronizer = new GeoserverClientSynchronizer(configuration, wfsUrl, m_postTemplate);
		
		String cycleMillis = m_properties.getProperty("cycleMillis", "10000");
		m_cycleMillis = Long.parseLong(cycleMillis);
		m_printProperty = m_properties.getProperty("printProperty", "name");

		LOGGER.log(Level.INFO, "namespace({0}), schemaLocation({1}), url({2}), cycleMillis({3})", new Object[] {
				m_namespace, schemaLocation, wfsUrl, m_cycleMillis
		});
		
		m_scheduler = Executors.newScheduledThreadPool(1);
		m_scheduler.scheduleWithFixedDelay(this, 0L, m_cycleMillis, TimeUnit.MILLISECONDS);
	}

	private void maybeLoadProperties() throws IOException {
		File props = new File("test.properties");
		if (!props.canRead()) {
			LOGGER.log(Level.INFO, "Cannot read {0}, skipping", props);
			return;
		}
		
		FileInputStream fis = new FileInputStream(props);
		try {
			m_properties = new Properties();
			m_properties.load(fis);
		} finally {
			fis.close();
		}
	}

	private void maybeLoadPostTemplateFromFile() throws IOException {
		File tmpl = new File("postTemplate.xml");
		if (!tmpl.canRead()) {
			LOGGER.log(Level.INFO, "Cannot read {0}, skipping", tmpl);
			return;
		}
		FileInputStream fis = new FileInputStream(tmpl);
		try {
			StringWriter sw = new StringWriter();
			int c;
			while ((c = fis.read()) != -1) {
				sw.write(c);
			}
			m_postTemplate = sw.toString();
		} finally {
			fis.close();
		}
	}

	private void tearDown() {
	}

	@Override
	public void run() {
		try {
			while (true) {
				System.out.println("SYNC STARTED");
				synchronizer.synchronize(m_features);
				System.out.println("SYNC COMPLETE");
				Thread.sleep(m_cycleMillis);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "failed", e);
		}
	}

	private void dumpFids() {
		int i = 1;
		for (Identifier fid : m_features.keySet()) {
			FeatureAccessor accessor = m_features.get(fid);
			Feature feature = accessor.getFeature();
			Property p = feature.getProperty(m_printProperty);
			Object value = p != null ? p.getValue() : "<no such property>";
			System.out.printf("%d: fid: %s %s=%s%n", 
					i, feature.getIdentifier().getID(), m_printProperty, value);
			i++;
		}
	}
}
