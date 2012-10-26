/* Copyright (c) 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.XStream;

/**
 * Manages access to the XLST transformation definitions
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class TransformRepository {

    static final Logger LOGGER = Logging.getLogger(TransformRepository.class);

    static final FilenameFilter CONFIG_NAME_FILTER = new SuffixFileFilter(".xml");

    XStream xs;

    GeoServerDataDirectory dataDir;

    FileItemCache<TransformInfo> infoCache = new FileItemCache<TransformInfo>(100) {

        @Override
        protected TransformInfo loadItem(File file) throws IOException {
            if(!file.exists()) {
                return null;
            }
            
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                TransformInfo info = (TransformInfo) xs.fromXML(fis);
                info.setName(getTransformName(file));

                return info;
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
    };

    public TransformRepository(GeoServerDataDirectory dataDir, Catalog catalog) {
        this.dataDir = dataDir;
        initXStream(catalog);
    }

    /**
     * Sets up xstream to get nice xml output
     * 
     * @param catalog
     */
    private void initXStream(Catalog catalog) {
        xs = new XStream();
        xs.alias("transform", TransformInfo.class);
        xs.registerLocalConverter(TransformInfo.class, "featureType", new ReferenceConverter(
                FeatureTypeInfo.class, catalog));
        xs.addDefaultImplementation(FeatureTypeInfoImpl.class, FeatureTypeInfo.class);
    }

    /**
     * The transform name is the same as the config file, minus the extension
     * 
     * @param file
     * @return
     */
    protected String getTransformName(File file) {
        String name = file.getName();
        int idx = name.indexOf(".");
        if (idx > 0) {
            return name.substring(0, idx);
        } else {
            return name;
        }
    }

    /**
     * Returns all the transform (either global or feature type specific)
     * 
     * @return
     */
    public List<TransformInfo> getAllTransforms() throws IOException {
        File root = dataDir.findOrCreateDir("wfs", "transform");
        List<TransformInfo> result = new ArrayList<TransformInfo>();
        for (File f : root.listFiles(CONFIG_NAME_FILTER)) {
            try {
                TransformInfo tx = infoCache.getItem(f);
                result.add(tx);
            } catch (Exception e) {
                LOGGER.log(Level.FINE,
                        "Failed to load configuration from file " + f.getAbsolutePath(), e);
            }
        }

        return result;
    }

    /**
     * Returns all the global transformations (not attached to a particular layer)
     * 
     * @return
     */
    public List<TransformInfo> getGlobalTransforms() throws IOException {
        List<TransformInfo> allTransformations = getAllTransforms();
        List<TransformInfo> result = new ArrayList<TransformInfo>();
        for (TransformInfo ti : allTransformations) {
            if (ti.getFeatureType() == null) {
                result.add(ti);
            }
        }

        return result;
    }

    /**
     * Returns transformations associated to a specific feature type
     * 
     * @param featureType
     * @return
     */
    public List<TransformInfo> getTypeTransforms(FeatureTypeInfo featureType) throws IOException {
        List<TransformInfo> allTransformations = getAllTransforms();
        List<TransformInfo> result = new ArrayList<TransformInfo>();
        for (TransformInfo ti : allTransformations) {
            if (ti.getFeatureType() != null
                    && ti.getFeatureType().getId().equals(featureType.getId())) {
                result.add(ti);
            }
        }

        return result;
    }

    /**
     * Returns a specific transformation by hand
     * 
     * @param name
     * @return
     */
    public TransformInfo getTransformInfo(String name) throws IOException {
        File infoFile = getTransformInfoFile(name);
        return infoCache.getItem(infoFile);
    }

    /**
     * Saves/updates the specified transformation
     * 
     * @param transform
     * @throws IOException
     */
    public void putTransformInfo(TransformInfo transform) throws IOException {
        if (transform.getName() == null) {
            throw new IllegalArgumentException("Transformation does not have a name set");
        }
        File file = getTransformInfoFile(transform.getName());

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            xs.toXML(transform, fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }

        infoCache.put(transform, file);
    }

    File getTransformInfoFile(String name) throws IOException {
        File root = dataDir.findOrCreateDir("wfs", "transform");
        File infoFile = new File(root, name + ".xml");
        return infoFile;
    }

}
