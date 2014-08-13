from geoserver.wps import process
from geoscript.geom import Geometry

@process(
  inputs={'geom': (Geometry, 'The geometry to buffer'), 'distance':(float,'The buffer distance')}, 
  outputs={'geom': (Geometry, 'The buffered geometry'), 'distance':(float,'The buffer distance')}, 
  title='Buffer', 
  description='Buffers a geometry')
def run(geom, distance):
  return {'geom': geom.buffer(distance), 'distance': distance}