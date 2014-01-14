import sys, optparse, getpass, urllib2, json
from base64 import b64encode
from datetime import datetime

class GitHub(object):

  def __init__(self, user, passwd):
    self.url = 'https://api.github.com/repos/geoserver/geoserver'
    self.auth = 'Basic '+b64encode('%s:%s' % (user,passwd))

  def list_pulls(self, after=None):
    n=0
    while True:
      n += 1
      r = urllib2.urlopen(urllib2.Request(self.pulls_url(n)))
      obj = json.loads(r.read())
      obj = filter(
        lambda x: x.has_key('merged_at') and x['merged_at'] is not None, obj)
      if after is not None:
        obj = filter(lambda x: 
          datetime.strptime(x['merged_at'], '%Y-%m-%dT%H:%M:%SZ') > after, obj)

      for pull in obj:
        yield PullRequest(pull)

      if len(obj) == 0:
        break 
      
  def pulls_url(self, n):
     return '%s/pulls?state=closed&page=%d' % (self.url, n)

class PullRequest(object):
  def __init__(self, obj):
    self.obj = obj

  def url(self):
    return self.obj['html_url']

  def title(self):
    return self.obj['title']

  def author(self):
    if self.obj.has_key('user') and self.obj['user'] is not None:
      return self.obj['user']['login']

  def merged(self):
    if self.obj.has_key('merged_at'):
      return self.obj['merged_at']

if __name__ == "__main__":
  
  p = optparse.OptionParser('Usage: %prog [options] username password')
  p.add_option('-d', '--date', dest='date', help='Date filter (YYYY-MM-DD)')
  """
  p.add_option('-j', '--host', dest='host', default='http://jira.codehaus.org', 
    help='JIRA host, default is codehaus') 
  p.add_option('-k', '--key', dest='key', default='GEOT',
    help='JIRA project key, default is GEOT') 
  p.add_option('-u', '--user', dest='user', default=getpass.getuser(), 
    help='JIRA user, default is current OS user')
  p.add_option('-p', '--passwd', dest='passwd', 
    help='JIRA password')
  p.add_option('-v', '--verbose', dest='verbose', action='store_true',
    help='Verbose flag')
  """

  opts, args = p.parse_args()
  if len(args) < 2:
     p.error('Must specify a GitHub username and password')

  opts = vars(opts)
  date = datetime.strptime(
    opts['date'], '%Y-%m-%d') if opts['date'] is not None else None

  gh = GitHub(args[0], args[1])
  for pr in gh.list_pulls(date):
    print pr.title()
    print pr.author()
    print pr.url()
    print pr.merged()
    print ''
