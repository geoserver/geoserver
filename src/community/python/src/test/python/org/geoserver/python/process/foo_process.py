from geoserver.process import process;
from geoscript.geom import Point;

@process('Foo', 'The foo process', '1.2.3', [('bar', str, 'The bar parameter'), 
    ('baz', None, 'The baz parameter'), ('bam', Point, 'The bam parameter')], 
    ('result', float, 'The result'))
def foo(bar, baz, bam):
  return 1.2;