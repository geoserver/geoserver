package org.geoserver.csw.store.internal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.Arrays;

import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geotools.csw.CSWConfiguration;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * 
 * @author Niels Charlier
 * 
 */
public class LayerGroupTest extends CSWInternalTestSupport {
    
    private static String ID_FORESTSANDSTREAMS = "forestsandstreams";
    private static String ID_BUILDINGSANDBRIDGES = "buildingsandbridges";
    private static String NAME_FORESTSANDSTREAMS = "Forests and Streams";
    private static String NAME_BUILDINGSANDBRIDGES = "Buildings and Bridges";
    
    
    private void addLayerGroup(String id, String name, PublishedInfo... publisheds) {
        LayerGroupInfoImpl group = (LayerGroupInfoImpl) getCatalog().getFactory().createLayerGroup();
        group.setId(id);
        group.setName(name);
        group.setTitle(name);
        group.getLayers().addAll(Arrays.asList(publisheds));
        
        getCatalog().add(group);    
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        addLayerGroup(ID_FORESTSANDSTREAMS, NAME_FORESTSANDSTREAMS, 
                getCatalog().getLayerByName("Forests"), 
                getCatalog().getLayerByName("Streams") );
        addLayerGroup(ID_BUILDINGSANDBRIDGES, NAME_BUILDINGSANDBRIDGES, 
                getCatalog().getLayerByName("Buildings"), 
                getCatalog().getLayerByName("Bridges") );
    }
    
    @Test
    public void testRecords() throws Exception {
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record" +
            "&resultType=results&elementSetName=full&maxRecords=100";
        Document d = getAsDOM(request);
        //print(d);
        checkValidationErrors(d, new CSWConfiguration());
        

        assertXpathExists("//csw:Record[dc:title='"+ NAME_BUILDINGSANDBRIDGES + "']", d);   
        assertXpathExists("//csw:Record[dc:title='"+ NAME_FORESTSANDSTREAMS + "']", d);        
        
    }
    
    @Test
    public void testRecordById() throws Exception {
        String request = "csw?service=CSW&version=2.0.2&request=GetRecordById&typeNames=csw:Record&id=" + ID_FORESTSANDSTREAMS;
        Document d = getAsDOM(request);
        //print(d);
        checkValidationErrors(d);
        
        assertXpathEvaluatesTo("1", "count(//csw:SummaryRecord)", d);   
    }
    
    @Test
    public void testRecordSortedAndPaged() throws Exception {
        String request = "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&SortBy=title&StartPosition=4&maxRecords=2";
        
        Document d = getAsDOM(request);
        print(d);
        checkValidationErrors(d);
        
        assertXpathEvaluatesTo("2", "count(//csw:SummaryRecord)", d); 
        assertXpathExists("//csw:SummaryRecord[dc:title='"+ NAME_BUILDINGSANDBRIDGES + "']", d);   
        assertXpathExists("//csw:SummaryRecord[dc:title='Buildings']", d);        
    }
    
}
