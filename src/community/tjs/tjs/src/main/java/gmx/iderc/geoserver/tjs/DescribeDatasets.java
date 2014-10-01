/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import net.opengis.tjs10.DescribeDatasetsType;
import net.opengis.tjs10.VersionType2;
import org.geoserver.ows.util.RequestUtils;
import org.geotools.xml.transform.TransformerBase;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Web Feature Service DescribeFeatureType operation.
 * <p>
 * This operation returns an array of  {@link org.geoserver.data.feature.FeatureTypeInfo} metadata
 * objects corresponding to the feature type names specified in the request.
 * </p>
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @version $Id: DescribeFeatureType.java 14551 2010-07-07 16:44:21Z groldan $
 */
public class DescribeDatasets {
    /**
     * Catalog reference
     */
    private TJSCatalog catalog;

    /**
     * WFS service
     */
    private TJSInfo tjs;

    /**
     * Creates a new wfs DescribeFeatureType operation.
     *
     * @param wfs     The wfs configuration
     * @param catalog The geoserver catalog.
     */
    public DescribeDatasets(TJSInfo wfs, TJSCatalog catalog) {
        this.catalog = catalog;
        this.tjs = tjs;
    }

    public TJSInfo getTJS() {
        return tjs;
    }

    public void setTJS(TJSInfo wfs) {
        this.tjs = wfs;
    }

    public TJSCatalog getCatalog() {
        return catalog;
    }

    public void setCatalog(TJSCatalog catalog) {
        this.catalog = catalog;
    }

    private List<String> VersionType2ToStringList(List<VersionType2> vtypes) {
        List<String> versions = new ArrayList<String>();
        for (VersionType2 vtype : vtypes) {
            versions.add(vtype.getLiteral());
        }
        return versions;
    }

    public TransformerBase run(DescribeDatasetsType request)
            throws TJSException {

        List<String> provided = new ArrayList<String>();
        provided.add("1.0.0");
//        provided.add("1.1.0");
        List<String> requested = new ArrayList<String>();

        if (request.getVersion() != null)
            requested.add(request.getVersion()._100_LITERAL.getLiteral());
        else {
            requested.add("1.0.0");
        }
        String version = RequestUtils.getVersionPreOws(provided, requested);

        final DescribeDatasetsTransformer ddsTransformer;
        if ("1.0.0".equals(version)) {
            ddsTransformer = new DescribeDatasetsTransformer.TJS1_0(tjs, catalog);
        } else {
            throw new TJSException("Could not understand version:" + version);
        }
        try {
            ddsTransformer.setEncoding(Charset.forName(tjs.getGeoServer().getGlobal().getCharset()));
        } catch (Exception ex) {
            Logger.getLogger(GetCapabilities.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        return ddsTransformer;
    }
}
