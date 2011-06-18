from geoserver import datastore

class Foo(object):

  @datastore('Foo', 'The foo datastore', bar=('The bar param', str), baz=('The baz param', file))
  def __init__(self, bar, baz):
    self.bar = bar
    self.baz = baz
