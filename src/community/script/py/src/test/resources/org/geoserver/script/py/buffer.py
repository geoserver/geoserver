from geoserver.wps import process
from geoscript.geom import Geometry

@process(
  inputs={'geom': (Geometry, 'The geometry to buffer'), 'distance':(float,'The buffer distance')}, 
  outputs={'result': (Geometry, 'The buffered geometry')}, 
  title='Buffer', 
  description='Buffers a geometry')
def run(geom, distance):
  return geom.buffer(distance);
