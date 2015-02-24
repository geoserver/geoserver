/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.geoxacml;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.VersionConstraints;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;
import com.sun.xacml.support.finder.PolicyCollection;
import com.sun.xacml.support.finder.PolicyReader;
import com.sun.xacml.support.finder.TopLevelPolicyException;

/**
 * A PolicyFinderModule implementation reading policies form the GEOSERVER_DATA_DIR
 * 
 * Assumptions:
 * 
 * starting directory: geoxacml directory for policy matched against requests : geoxacml/byRequest
 * directory for policy referenced by ohter policieis : geoxacml/byReference
 * 
 * Sub directories are searched recursively , all files with extension .xml or .XML are assumed to
 * be policy files.
 * 
 * @author Christian Mueller
 * 
 */
public class DataDirPolicyFinderModlule extends PolicyFinderModule {

    public static String BASE_DIR = "geoxacml";

    public static String BY_REQUEST_DIR = "byRequest";

    public static String BY_REFERENCE_DIR = "byReference";

    protected PolicyCollection policiesByReference;

    protected PolicyCollection policiesByRequest;

    protected boolean validate;

    protected String baseDir;

    private static final Logger logger = Logger.getLogger(DataDirPolicyFinderModlule.class
            .getName());

    /**
     * Constructor, policies are not validated against the XML schema
     * 
     */
    public DataDirPolicyFinderModlule() {
        this(false);
    }

    /**
     * Constructor
     * 
     * @param validate
     *            if true, perform a XML schema validation
     * 
     */
    public DataDirPolicyFinderModlule(boolean validate) {
        this.validate = validate;
        this.policiesByReference = new PolicyCollection();
        this.policiesByRequest = new PolicyCollection();
    }

    @Override
    public void init(PolicyFinder finder) {

        PolicyReader reader = null;
        try {

            if (validate)
                reader = new PolicyReader(finder, logger, new File(GeoXACML.getPolicyXMLSchemaURL()
                        .toURI()));
            else
                reader = new PolicyReader(finder, logger);
        } catch (URISyntaxException e) {
            // should not happen
        }
        readPolicies(policiesByReference, BY_REFERENCE_DIR, reader);
        readPolicies(policiesByRequest, BY_REQUEST_DIR, reader);

    }

    private void readPolicies(PolicyCollection coll, String subdir, PolicyReader reader) {
        List<String> fileNames = getXMLFileNames(subdir);
        for (String fileName : fileNames) {
            try {
                AbstractPolicy policy = reader.readPolicy(new File(fileName));
                if (!coll.addPolicy(policy)) {
                    if (logger.isLoggable(Level.WARNING))
                        logger.log(Level.WARNING, "tried to load the same "
                                + "policy multiple times: " + fileName);
                } else {
                    logger.fine("Read policy(Set) " + policy.getId() + " from "
                            + fileName.toString());
                }

            } catch (ParsingException e) {
                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Error reading policy: " + fileName, e);
            }
        }
    }

    private List<String> getXMLFileNames(String subdir) {

        File parentDir = null;
        if (baseDir == null) {
            String parent = "file:" + BASE_DIR + "/" + subdir;
            parentDir = GeoserverDataDirectory.findDataFile(parent);
        } else {
            parentDir = new File(baseDir, BASE_DIR + "/" + subdir);
        }

        List<String> fileNames = new ArrayList<String>();
        collectXMLFiles(parentDir, fileNames);
        return fileNames;
    }

    private void collectXMLFiles(File f, List<String> fileNames) {
        if (f.isFile()) {
            if (f.getName().endsWith(".xml") || f.getName().endsWith(".XML"))
                fileNames.add(f.getAbsolutePath());
        }
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children == null)
                return;
            for (File child : children) {
                collectXMLFiles(child, fileNames);
            }
        }
    }

    @Override
    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        try {
            AbstractPolicy policy = policiesByRequest.getPolicy(context);

            if (policy == null)
                return new PolicyFinderResult();
            else
                return new PolicyFinderResult(policy);
        } catch (TopLevelPolicyException tlpe) {
            return new PolicyFinderResult(tlpe.getStatus());
        }

    }

    @Override
    public PolicyFinderResult findPolicy(URI idReference, int type, VersionConstraints constraints,
            PolicyMetaData parentMetaData) {

        AbstractPolicy policy = policiesByReference.getPolicy(idReference.toString(), type,
                constraints);

        if (policy == null)
            return new PolicyFinderResult();
        else
            return new PolicyFinderResult(policy);
    }

    @Override
    public void invalidateCache() {
        // TODO
        super.invalidateCache();
    }

    @Override
    public boolean isIdReferenceSupported() {
        return true;
    }

    @Override
    public boolean isRequestSupported() {
        return true;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

}
