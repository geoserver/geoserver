package gmx.iderc.geoserver.tjs;


import junit.framework.Test;
import org.w3c.dom.Document;

public class DescribeJoinAbilitiesTest extends TJSTestSupport {
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeJoinAbilitiesTest());
    }

    public void testGet() throws Exception {
        Document doc = getAsDOM("tjs?service=TJS&request=DescribeJoinAbilities&version=1.0.0");
        print(doc);
    }

}
