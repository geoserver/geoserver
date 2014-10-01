/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import net.opengis.tjs10.*;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.xml.transform.TransformerBase;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;

import javax.xml.transform.TransformerException;
import java.util.logging.Logger;

/**
 * @author root
 */
public class DefaultTableJoiningService implements TableJoiningService, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static Logger LOGGER = Logger.getLogger(DefaultTableJoiningService.class.getName());

    private Catalog geoServerCatalog;
    private TJSCatalog catalog;
    private GeoServer geoServer;

    ApplicationContext applicationContext;

    public DefaultTableJoiningService(GeoServer gs) {
        this(gs.getCatalog());
        this.geoServer = gs;
    }

    public DefaultTableJoiningService(Catalog geoServerCatalog) {
        this.geoServerCatalog = geoServerCatalog;
        catalog = TJSExtension.getTJSCatalog();
        init();
    }

    public DefaultTableJoiningService(Catalog geoServerCatalog, TJSCatalog tjsCatalog) {
        this.geoServerCatalog = geoServerCatalog;
        catalog = tjsCatalog;
        init();
    }

    protected String getBaseURL() {
        try {
            Request owsRequest = ((ThreadLocal<Request>) Dispatcher.REQUEST).get();
            if (owsRequest != null){
                return owsRequest.getHttpRequest().getRequestURL().toString();
            }else{
                //ocurre cuando se realizan los test
                return "http://localhost:8080/geoserver/";
            }
        } catch (Exception ex) {
            return null;
        }
    }

    void init() {
        deleteTJSTempWorkspace();
        //remakeJoinedMaps();
        GeoServerExtensions.getProperty("Servlet context parameter ");
    }

    private void remakeJoinedMaps() {
        for (FrameworkInfo frameworkInfo : catalog.getFrameworks()) {
            for (DatasetInfo datasetInfo : catalog.getDatasetsByFramework(frameworkInfo.getId())) {
                if (datasetInfo.getAutoJoin()){
                    GetDataXMLType dataXMLType = Tjs10Factory.eINSTANCE.createGetDataXMLType();
                    dataXMLType.setFrameworkURI(frameworkInfo.getUri());
                    dataXMLType.setDatasetURI(datasetInfo.getDatasetUri());
                    String atts = null;
                    for (ColumnInfo columnInfo : datasetInfo.getColumns()) {
                        if (atts != null){
                            atts = atts + ","+columnInfo.getName();
                        }else{
                            atts = columnInfo.getName();
                        }
                    }
                    dataXMLType.setAttributes(atts);

                    JoinDataType joinDataType = Tjs10Factory.eINSTANCE.createJoinDataType();
                    AttributeDataType attributeDataType =Tjs10Factory.eINSTANCE.createAttributeDataType();
                    attributeDataType.setGetDataXML(dataXMLType);
                    joinDataType.setAttributeData(attributeDataType);

                    TransformerBase transformerBase = this.JoinData(joinDataType);
//                    Operation op = new Operation("JoinData", new Service("TJS",null,null,null),null, new Object[]{joinDataType});
                    try {
                        String xmlData = transformerBase.transform(joinDataType);
                    } catch (TransformerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

/*
        for (JoinedMapInfo map : catalog.getJoinedMaps()) {
            DatasetInfo dsi = catalog.getDatasetByUri(map.getDatasetUri());
            if (dsi != null && dsi.getAutoJoin() && (map.getCreationTime() + map.getLifeTime() > System.currentTimeMillis())) {
                final String req = map.getServerURL() + "/ows?Service=TJS&Version=1.0&Request=JoinData" +
                                           "&FrameworkUri=" + map.getFrameworkURI() + "&GetDataURL=" + map.getGetDataURL();
                try {
                    final URL url = new URL(req);
                    final InputStream inputStream = url.openStream();
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null)
                        LOGGER.info(line);
                    inputStream.close();
                } catch (MalformedURLException e) {
                    LOGGER.warning("No se pudo reconstruir el mapa " + map + " por " + e);
                } catch (IOException e) {
                    LOGGER.warning("Falló conexión para reconstruir el mapa " + map + " por " + e);
                }
            } else {
                catalog.remove(map);
            }
        }
*/
    }

    private void deleteTJSTempWorkspace() {

        WorkspaceInfo wsInfo = geoServerCatalog.getWorkspaceByName(TJSExtension.TJS_TEMP_WORKSPACE);

        if (wsInfo != null) {
            CascadeDeleteVisitor deleteVisitor = new CascadeDeleteVisitor(geoServerCatalog);
            deleteVisitor.visit(wsInfo);
        }
    }

    public TJSInfo getServiceInfo() {
        return geoServer.getService(TJSInfo.class);
    }

    public TransformerBase getCapabilities(GetCapabilitiesType request) throws TJSException {
        return new GetCapabilities(getServiceInfo(), catalog).run(request);
    }

    public TransformerBase describeFrameworks(DescribeFrameworksType request) throws TJSException {
        return new DescribeFrameworks(getServiceInfo(), catalog).run(request);
    }

    public TransformerBase describeKey(DescribeKeyType request) throws TJSException {
        return new DescribeKey(getServiceInfo(), catalog).run(request);
    }

    public TransformerBase DescribeDatasets(DescribeDatasetsType request) throws TJSException {
        return new DescribeDatasets(getServiceInfo(), catalog).run(request);
    }

    public TransformerBase DescribeData(DescribeDataType request) throws TJSException {
        return new DescribeData(getServiceInfo(), catalog).run(request);
    }

    public TransformerBase getData(GetDataType request) throws TJSException {
        return new GetData(getServiceInfo(), catalog).run(request);
    }

    public TransformerBase DescribeJoinAbilities(RequestBaseType request) throws TJSException {
        return new DescribeJoinAbilities(getServiceInfo(), catalog).run(request);
    }

    public TransformerBase JoinData(JoinDataType request) throws TJSException {
        return new JoinData(getServiceInfo(), catalog).run(request);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //remakeJoinedMaps();
    }
}
