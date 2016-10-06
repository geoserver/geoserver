from geoserver.wps import process
from geoscript.geom import Geometry
from org.geoserver.script.wps import StatusMonitor

@process(
  inputs={'geom': (Geometry, 'The geometry to buffer'), 
          'distance': (float,'The buffer distance'),
          'monitor': (StatusMonitor, 'The monitor')}, 
  outputs={'result': (Geometry, 'The buffered geometry')}, 
  title='Buffer', 
  description='Buffers a geometry')
def run(geom, distance, monitor):
  if monitor is not None:
    monitor.task = 'The task';
    monitor.progress = 10;
  if distance < 0:
    monitor.throwException('Forbidden', 'userInput', 'distance')
  return geom.buffer(distance);
