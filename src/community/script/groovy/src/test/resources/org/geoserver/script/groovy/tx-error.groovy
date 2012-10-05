import org.geoserver.wfs.WFSException

def before(req, context) {
  throw new WFSException("before exception");
}