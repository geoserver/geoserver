/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gmx.iderc.geoserver.tjs.web;

import gmx.iderc.geoserver.tjs.TJSTestSupport;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.TJSExtensionTestSupport;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.web.GeoServerWicketTestSupport;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;

/**
 * @author root
 */
public abstract class TJSWicketTestSupport extends GeoServerWicketTestSupport {

    TJSCatalog tjsCatalog;
    public static QName PROVINCIAS = new QName(MockData.CITE_URI, "Provincias", MockData.CITE_PREFIX);

    @Override
    public void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        //tjsCatalog.setCatalog(catalog);
        tjsCatalog = TJSExtensionTestSupport.getTJSCatalog(getTestPersistenceDirectory());
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(PROVINCIAS.getLocalPart());
        tjsCatalog.getFrameworks().get(0).setFeatureType(fti);
        tjsCatalog.save();
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        //super.populateDataDirectory(dataDirectory);
        URL resUrl = TJSTestSupport.class.getResource("Provincias.properties");
        dataDirectory.addPropertiesType(PROVINCIAS, resUrl, Collections.EMPTY_MAP);

        URL configUrl = TJSTestSupport.class.getResource("config.xml");

        String configDir = dataDirectory.getDataDirectoryRoot().toString().concat("/gmxtjs");
        File configFile = new File(configDir);
        if (!configFile.exists()) {
            configFile.mkdir();
        }

        String config = configFile.toString().concat("/config.xml");
        configFile = new File(config);
        InputStream is = configUrl.openStream();
        IOUtils.copy(is, configFile);
    }

    protected File getTestPersistenceDirectory() {
        return getDataDirectory().root();
    }

    protected TJSCatalog getTjsCatalog() {
        return tjsCatalog;
    }
}
