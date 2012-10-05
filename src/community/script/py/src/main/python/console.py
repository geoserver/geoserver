import base64, code, httplib, logging, optparse
try:
  import json
except ImportError:
  try:
    import simplejson as json
  except ImportError, e:
    print e 
    exit('Install simplejson or run on Python 2.6+')

logging.basicConfig(format='%(levelname)s - %(message)s')
logger = logging.getLogger('session')

class SessionClient(object):

  def __init__(self, host, port=8080, context='geoserver', 
               user='admin', passwd='geoserver'):

    self.host = host
    self.port = port
    self.auth = base64.encodestring('Basic %s:%s' % (user,passwd)).replace('\n','')
    self.context = context
    self.cx = None
  
  def list(self):
    r = self._request('GET', 'sessions/py') 
    if self._check(r, 200, 'GET sessions failed'):
      obj = json.loads(r.read())
      return [(s['id'],s['engine']) for s in obj['sessions']]

  def new(self):
      return Session(int(r.read()), self)

  def connect(self, sid=None):
    if sid == None:
      r = self._request('POST', 'sessions/py')
      if self._check(r, 201, 'POST new session failed'):
        sid = int(r.read())
      else:
        return False
      
    r = self._request('GET', 'sessions/py/%d' % sid)
    if self._check(r, 200, 'GET session failed'):
      while True: 
        buf = raw_input('>>> ')
        try:
          while not code.compile_command(buf):
            buf = buf + '\n' + raw_input('... ')
    
          if "exit()" == buf :
            self.close()
            break
    
          r = self._request('PUT', 'sessions/py/%d' % sid, buf)
          self._check(r, 200, 'PUT statement failed')
          result = r.read()
          if result and len(result.strip()) > 0:
            print result,
            if not result[-1] == '\n':
              print
        except SyntaxError, e:
          print e  
    
  def close(self):
     if self.cx:
       self.cx.close()

  def _request(self, method, path, body=None):
    if self.cx:
      self.cx.close()

    self.cx = httplib.HTTPConnection(self.host, self.port)
    self.cx.request(method, '/geoserver/script/%s' % path, body, 
      {'Authorization':self.auth})
    return self.cx.getresponse()

  def _check(self, resp, status, msg):
    if resp.status != status:
      logger.warning('%s, expecting status %d but got %d' 
        % (msg, status, resp.status))
      return False
    return True

if __name__ == '__main__':
  p = optparse.OptionParser('Usage: %prog [options] host [list|connect]') 
  p.add_option('-p', '--port', dest='port', type='int', default=8080, 
               help='server port, default is 8080')
  p.add_option('-u', '--user', dest='user', default='admin',
               help='username, default is admin')
  p.add_option('-w', '--password', dest='passwd', default='geoserver',
               help='password, default is geoserver')
  p.add_option('-c', '--context', dest='context', default='geoserver', 
               help='context, default is geoserver')
  p.add_option('-s', '--session', dest='session', type=int,
               help='session identifier')

  opts, args = p.parse_args()
  if len(args) == 0:
     p.error('host is required')

  opts = vars(opts)
  cmds = ('list', 'connect')
  cmd = args[1] if len(args) > 1 else cmds[0]
  if cmd not in cmds:
    p.error("unrecognized command '%s'" % cmd)

  if cmd == 'connect' and not opts.has_key('session'):
    p.error('connect command requires session option')

  try:
    sid = opts['session']
    del opts['session']
  except KeyError:
    sid = None

  c = SessionClient(args[0], **opts)

  if cmd == 'list':
    print c.list();
  elif cmd == 'connect':
    c.connect(sid)

