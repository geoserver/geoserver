from org.geoserver.wfs import WFSException

def before(req, context):
  raise WFSException("before exception")