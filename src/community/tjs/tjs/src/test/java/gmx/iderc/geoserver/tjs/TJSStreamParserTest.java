package gmx.iderc.geoserver.tjs;

import net.opengis.tjs10.FrameworkDescriptionsType;
import net.opengis.tjs10.FrameworkType;
import org.geotools.tjs.TJS;
import org.geotools.tjs.TJSConfiguration;
import org.geotools.xml.StreamingParser;

import java.io.InputStream;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 10/8/12
 * Time: 1:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class TJSStreamParserTest {

    public void parseGetFrameworkDescriptions() throws Exception {
        URL resurl = getClass().getResource("tjsDescribeFrameworks_response.xml");
        InputStream is = resurl.openStream();
        StreamingParser parser = new StreamingParser(new TJSConfiguration(), is, TJS.FrameworkDescriptions);
        FrameworkDescriptionsType frameworkDescriptions = (FrameworkDescriptionsType) parser.parse();
        for (int index = 0; index < frameworkDescriptions.getFramework().size(); index++) {
            FrameworkType framework = (FrameworkType) frameworkDescriptions.getFramework().get(index);
            System.out.println();
            framework.getFrameworkURI();
        }
    }


    static void Main(String[] args) {
        TJSStreamParserTest test = new TJSStreamParserTest();
        try {
            test.parseGetFrameworkDescriptions();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
