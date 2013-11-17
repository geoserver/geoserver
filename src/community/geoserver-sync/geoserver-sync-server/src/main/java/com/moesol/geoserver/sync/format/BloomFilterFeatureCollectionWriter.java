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

package com.moesol.geoserver.sync.format;




import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.moesol.geoserver.sync.format.SyncChecksumOutputFormat;

public class BloomFilterFeatureCollectionWriter {
	private static final Logger LOGGER = Logging.getLogger(SyncChecksumOutputFormat.class.getName());
	
	private final Charset UTF8 = Charset.forName("UTF-8");
	private final FeatureCollectionType m_featureCollection;
	private final PrintWriter m_out;
	private final MessageDigest m_sha1;
	private final TreeMap<String, Feature> m_sortedFeatures = new TreeMap<String, Feature>();

	private List<String> m_attributesToInclude;

	public BloomFilterFeatureCollectionWriter(FeatureCollectionType featureCollection,
			OutputStream output, Operation getFeature) throws ServiceException {
		
		m_featureCollection = featureCollection;
		m_out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output, UTF8)));
		try {
			m_sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new ServiceException(e, "Unable to load SHA-1 message digest");
		}
	}

	public void write() throws IOException, ServiceException {
	    List<FeatureCollection> resultsList = m_featureCollection.getFeature();
	    
	    processAllCollections(resultsList);
	    sha1SortedFeatures();
	    writeSha1Digest();
    }

	private void processAllCollections(List<FeatureCollection> resultsList) {
		for (int i = 0; i < resultsList.size(); i++) {
	        FeatureCollection collection = resultsList.get(i);
	        processOne(collection);
	    }
	}

	private void processOne(FeatureCollection collection) {
        FeatureIterator iterator = collection.features();
        while (iterator.hasNext()) {
        	Feature feature = iterator.next();
        	processOne(feature);

        }
	}

	private void processOne(Feature feature) {
		SimpleFeature simple = (SimpleFeature) feature;
		m_sortedFeatures.put(simple.getID(), simple);
	}
	
	
	private void sha1SortedFeatures() {
		for (Feature f : m_sortedFeatures.values()) {
			sha1One(f);
		}
		m_sortedFeatures.clear();
	}

	private void sha1One(Feature feature) {
		SimpleFeature simple = (SimpleFeature) feature;
		
		LOGGER.log(Level.FINER, "sha1 += {0}", simple.getID());
		m_sha1.update(simple.getID().getBytes(UTF8));
		
        SimpleFeatureType fType = simple.getFeatureType();
        List<AttributeDescriptor> types = fType.getAttributeDescriptors();
        for (int i = 0; i < types.size(); i++) {
            Object value = simple.getAttribute(i);
            AttributeDescriptor ad = types.get(i);
        	sha1OneAttribute(ad, value);
        }
	}

	private void sha1OneAttribute(AttributeDescriptor attr, Object value) {
		if (m_attributesToInclude.contains(attr.getName().toString())) {
			LOGGER.log(Level.FINER, "sha1 += {0}({1})", new Object[] {
					attr.getName(), value.toString()
			});
			m_sha1.update(value.toString().getBytes(UTF8));
		}
		// System.out.println(attr.getName() + "=" + value);
	}

	private void writeSha1Digest() {
		byte[] digest = m_sha1.digest();
	    for (byte b : digest) {
	    	m_out.printf("%02x", 0xFF & b);
	    }
    	m_out.flush();
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
