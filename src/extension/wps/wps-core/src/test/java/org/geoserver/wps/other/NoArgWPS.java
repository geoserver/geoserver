package org.geoserver.wps.other;


//import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geoserver.wps.gs.GeoServerProcess;

@DescribeProcess(title="NoArgWPS", description="NoArgWPS - test case for no argument process")
public class NoArgWPS implements GeoServerProcess {

   @DescribeResult(name="result", description="output result")
   public String execute( ) {
        return "Completed!"  ;
   }
}
