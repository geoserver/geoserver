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

package com.moesol.geoserver.sync.core;


import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.geometry.Geometry;


/**
 * Compute the SHA-1 hash of a single feature.
 *  
 * Apartment model threading.
 * 
 * @author hastings
 */
public class FeatureSha1 {
	private static final Logger LOGGER = Logger.getLogger(FeatureSha1.class.getName());
	public static FeatureSha1Mapper MAPPER = new FeatureSha1Mapper() {
		@Override
		public Sha1Value map(Sha1Value old) {
			return old; // identity mapping
		}
	};
	private final Charset UTF8 = Charset.forName("UTF-8");
	private final MessageDigest m_sha1;
	private List<String> m_attributesToInclude = new ArrayList<String>();

	public List<String> getAttributesToInclude() {
		return Collections.unmodifiableList(m_attributesToInclude);
	}

	public void setAttributesToInclude(List<String> attributesToInclude) {
		m_attributesToInclude = attributesToInclude;
	}

	public FeatureSha1() {
		try {
			m_sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Unable to load SHA-1 message digest", e);
		}
	}
	
	public Sha1Value computeIdSha1(Feature feature) {
		try {
			String featureId = feature.getIdentifier().toString();
			m_sha1.update(featureId.getBytes(UTF8));
			Sha1Value result = new Sha1Value(m_sha1.digest());
			return MAPPER.map(result);
		} finally {
			m_sha1.reset();
		}
	}
	public Sha1Value computeValueSha1(Feature feature) {
		Collection<Property> properties = feature.getProperties();
		String featureId = feature.getIdentifier().toString();
		LOGGER.log(Level.FINER, "sha1 += fid({0})", featureId);
		m_sha1.update(featureId.getBytes(UTF8));
		
		sha1Properties(properties);
		
		Sha1Value result = new Sha1Value(m_sha1.digest());
		LOGGER.log(Level.FINER, "result: {0}", result);
		return MAPPER.map(result);
	}
	
	public Sha1Value sha1OfSha1(Sha1Value sha1One) {
		return MAPPER.map(new Sha1Value(m_sha1.digest(sha1One.get())));
	}

	void sha1Properties(Collection<Property> properties) {
		List<Property> sortedProperties = new ArrayList<Property>(properties.size());
		for (Property p : properties) {
			sortedProperties.add(p);
		}
		
		Collections.sort(sortedProperties, new Comparator<Property>() {
			@Override
			public int compare(Property o1, Property o2) {
				int r = o1.getName().getLocalPart().compareTo(o2.getName().getLocalPart());
				if (r != 0) {
					return r;
				}
				String ns1 = o1.getName().getNamespaceURI();
				String ns2 = o2.getName().getNamespaceURI();
				if (ns1 == null) {
					return ns2 == null ? 0 : -1;
				}
				return ns1.compareTo(ns2);
			}
		});
		
		for (Property p : sortedProperties) {
			if (p instanceof ComplexAttribute) {
				ComplexAttribute complex = (ComplexAttribute) p;
				sha1Properties(complex.getProperties());
			} else {
				sha1Property(p);
			}
		}
	}

	private void sha1Property(Property p) {
		if (!shouldSha1Property(p)) {
			return;
		}
		Object value = p.getValue();
		
		if (value == null) {
			LOGGER.log(Level.FINER, "No attribute value for {0}, skipping", p);
			return;
		}
		
		// TODO: Strangely the feature gets changed to its correct CRS AFTER the
		// filters run, so that if we SHA-1 a geometry in the filter we get
		// coordinates in one order (x,y), but after the filters run the CRS has
		// been applied and the coordinates are in (y,x) order. Therefore, we
		// currently skip ALL Geometries. This probably makes it faster too...
		if (value instanceof Geometry) {
			LOGGER.log(Level.FINER, "Geometry skipped {0}", value);
			return;
		}
		if (value instanceof com.vividsolutions.jts.geom.Geometry) {
			LOGGER.log(Level.FINER, "Geometry skipped {0}", value);
			return;
		}
		// TODO: Strangely some features have two names on the client.
		// TODO: Use *,-name to remove name from the list of attributes
		// -Dsha1.keep.name no longer supported
//		if ("name".equals(p.getName().toString()) && !SHA1_KEEP_NAME) {
//			LOGGER.log(Level.FINER, "Name skipped {0}", value);
//			return;
//		}
		if (value instanceof Timestamp) {
			Timestamp ts = (Timestamp) value;
			value = ts.getTime(); // Make sure we use UTC
		}
		if (value instanceof Calendar) {
			Calendar cal = (Calendar) value;
			value = cal.getTime(); // Make sure we use UTC
		}
		if (value instanceof Date) {
			Date date = (Date) value;
			value = date.getTime(); // Make sure we use UTC
		}
		
		Level level = Level.FINER;
		if (LOGGER.isLoggable(level)) {
			LOGGER.log(level, "sha1 += {0}({1})<{2}>", new Object[] {
					p.getName(), value, value == null ? "null" : value.getClass()
			});
		}
		m_sha1.update(value.toString().getBytes(UTF8));
	}

	private boolean shouldSha1Property(Property p) {
		if (m_attributesToInclude.contains(p.getName().toString())) {
			return true;
		}
		if (m_attributesToInclude.contains("-" + p.getName().toString())) {
			return false;
		}
		if (m_attributesToInclude.contains("-all")) { // deprecated
			return true;
		}
		if (m_attributesToInclude.contains("*")) {
			return true;
		}
		LOGGER.log(Level.FINER, "Skipping: {0}", p.getName());
		return false;
	}

	public void parseAttributesToInclude(String atts) {
		m_attributesToInclude = Arrays.asList(parseAttributes(atts));
	}
	
	static String [] parseAttributes(String atts) {
		if (atts == null) {
			return new String[0];
		}
		String[] r = atts.split("[,\\s]+");
		if (r[0].isEmpty()) {
			return new String[0];
		}
		return r;
	}

}
