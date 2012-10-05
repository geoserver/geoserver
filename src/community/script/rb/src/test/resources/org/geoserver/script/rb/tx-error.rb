require 'java'
def before(req, context)
  raise org.geoserver.wfs.WFSException.new("before exception")
end