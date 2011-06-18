from geoserver.filter import function

@function
def myBuffer(feature, geom):
  return geom.buffer(2);
