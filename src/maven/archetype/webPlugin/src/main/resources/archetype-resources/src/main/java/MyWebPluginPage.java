package ${groupId};

import org.geoserver.web.GeoServerBasePage;
import org.apache.wicket.markup.html.basic.Label;

public class MyWebPluginPage extends GeoServerBasePage {

  public MyWebPluginPage() {
    add(new Label("info"));
  }

}
