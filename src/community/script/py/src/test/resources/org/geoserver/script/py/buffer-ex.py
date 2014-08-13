from geoserver.wps import process
from geoscript.geom import Geometry

@process(
  inputs={'geom': (Geometry, 'The geometry to buffer'), 
          'distance':(float, 'The buffer distance'),
          'capStyle': (str, 'The style of buffer endings', {'min': 0, 'domain' : ('round', 'flat', 'square')}),
          'quadrantSegments': (int, 'Number of segments' , {'min': 0})},
  outputs={'result': (Geometry, 'The buffered geometry')}, 
  title='Buffer', 
  description='Buffers a geometry')
def run(geom, distance, capStyle='round', quadrantSegments=8):
  return geom.buffer(distance);
