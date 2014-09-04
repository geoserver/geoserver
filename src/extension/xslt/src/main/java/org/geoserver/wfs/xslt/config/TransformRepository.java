/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.wfs.WFSException;
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

    /**
     * Caches the {@link TransformInfo} objects so that we don't have to load them from disk all the time
     */
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
    
    /**
     * Caches the XSLT Templates to avoid parsing the XSLT over and over (Templates is thread safe,
     * {@link Transformer} is not.
     */
    FileItemCache<Templates> transformCache = new FileItemCache<Templates>(100) {
        
        @Override
        protected Templates loadItem(File file) throws IOException {
            try {
                Source xslSource = new StreamSource(file);
                
                TransformerFactory tf = TransformerFactory.newInstance( );
                final List<TransformerException> errors = new ArrayList<TransformerException>();
                tf.setErrorListener(new ErrorListener() {
                    
                    @Override
                    public void warning(TransformerException e) throws TransformerException {
                        LOGGER.log(Level.WARNING, "Found warning while loading XSLT template", e);
                        
                    }
                    
                    @Override
                    public void fatalError(TransformerException e) throws TransformerException {
                        errors.add(e);
                    }
                    
                    @Override
                    public void error(TransformerException e) throws TransformerException {
                        errors.add(e);
                        
                    }
                });
                Templates template = tf.newTemplates(xslSource);
                
                if(errors.size() > 0) {
                    StringBuilder sb = new StringBuilder("Errors found in the template");
                    for (TransformerException e : errors) {
                        sb.append("\n").append(e.getMessageAndLocation());
                    }
                    
                    throw new IOException(sb.toString());
                }
                
                return template;
            } catch(TransformerException e) {
                throw new IOException("Error found in the template: " + e.getMessageAndLocation());
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
        xs.omitField(TransformInfo.class, "name");
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
     * Deletes a transformation definition and its associated XSLT file (assuming the
     * latter is not shared with other transformations)
     * 
     * @param info
     * @return
     * @throws IOException
     */
    public boolean removeTransformInfo(TransformInfo info) throws IOException {
         File infoFile = getTransformInfoFile(info.getName());
         boolean result = infoFile.delete();
         
         File xsltFile = getTransformFile(info);
         infoCache.removeItem(infoFile);
         
         boolean shared = false;
         if(xsltFile.exists()) {
             for(TransformInfo ti : getAllTransforms()) {
                 File curr = getTransformFile(ti);
                 if(curr.equals(xsltFile)) {
                     shared = true;
                     break;
                 }
             }
         }
         if(!shared) {
             result = result && xsltFile.delete();
             transformCache.removeItem(xsltFile);
         }
         
         return result;
    }

    
    /**
     * Returns the XSLT transformer for a specific {@link TransformInfo}
     * 
     * @param name
     * @return
     */
    public Transformer getTransformer(TransformInfo info) throws IOException {
        File txFile = getTransformFile(info);

        Templates templates = transformCache.getItem(txFile);
        if(templates != null) {
            try {
                return templates.newTransformer();
            } catch (TransformerConfigurationException e) {
                throw new WFSException("Failed to load XSLT transformation " + info.getXslt(), e);
            }
        } else {
            throw new IOException("No XLST found at " + txFile.getAbsolutePath());
        }
    }
    
    /**
     * Returns the stylesheet of a transformation. It is the duty of the caller to close the input stream after reading it.
     * @return
     * @throws IOException
     */
    public InputStream getTransformSheet(TransformInfo info) throws IOException {
        File txFile = getTransformFile(info);
        
        return new FileInputStream(txFile);
    }

   
    
    /**
     * Writes the stylesheet of a transformation. This method will close the provided input stream.
     * 
     * @param info
     * @param sheet
     * @throws IOException
     */
    public void putTransformSheet(TransformInfo info, InputStream sheet) throws IOException {
        File txFile = getTransformFile(info);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(txFile);
            IOUtils.copy(sheet, fos);
        } finally {
            IOUtils.closeQuietly(sheet);
            IOUtils.closeQuietly(fos);
        }
        
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
    
    private File getTransformFile(TransformInfo info) throws IOException {
        String txName = info.getXslt();
        File root = dataDir.findOrCreateDir("wfs", "transform");
        File txFile = new File(root, txName);
        return txFile;
    }

    
}
