from geoserver.format import map_format
from geoscript import geom

@map_format('Foo', 'text/plain')
def write(map, output):
   output.write("%d;%d\n" % (map.width, map.height));
   output.write("%s\n" % str(map.bounds));
   for l in map.layers:
     for f in l.features():
       output.write("%s\n" % geom.writeWKT(f.geom))
