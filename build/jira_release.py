import sys, optparse, getpass
import SOAPpy, SOAPpy.Types

class JIRA(object):

  def __init__(self, jira_host, proj, user, passwd):
    self.proj = proj
    self.soap = SOAPpy.WSDL.Proxy(
      '%s/rpc/soap/jirasoapservice-v2?wsdl'%jira_host)
    self.auth = self.soap.login(user, passwd)


  def list_methods(self):
    for m in self.soap.methods.keys():
      print m
  
  def describe_method(self, name):
     m = self.soap.methods[name]
     for p in m.inparams:
       print p.name.ljust(10), p.type 

  def list_versions(self):
    versions = self.soap.getVersions(self.auth, self.proj)
    return [v.name for v in versions]

  def is_version_released(self, name):
    versions = self.soap.getVersions(self.auth, self.proj)
    for v in versions:
       if v.name == name:
         return v.released
   
  def get_version_id(self, name):
    """
    Return the id for a named version, returning None if no such version exists.
    """
    versions = self.soap.getVersions(self.auth, self.proj)
    for v in versions:
       if v.name == name:
         return v.id
   
  def create_version(self, name):
    """
    Creates a new version with the specified name.
    """
    self.soap.addVersion(self.auth, self.proj, {'name': name})

  def get_open_issues(self, ver_name):
    """
    Returns keys for unresolved/open issues for the specified version name.
    """
    jql = 'project = %s and status in (Open, "In Progress", Reopened) and fixVersion = "%s"' % (self.proj, ver_name)
    n = SOAPpy.Types.intType(500)
    issues = self.soap.getIssuesFromJqlSearch(self.auth, jql, n)
    return [i.key for i in issues]
      
  def set_fixed_version(self, issue, ver_name):
    """
    Sets the fixVersion for an issue
    """
    ver_id = self.get_version_id(ver_name)
    if ver_id:
      self.soap.updateIssue(self.auth, issue, 
       [{'id': 'fixVersions', 'values': [ver_id]}])

  def release_version(self, ver_name):
     self.soap.releaseVersion(self.auth, self.proj,
       {'name': ver_name, "released": True})

if __name__ == "__main__":
  
  p = optparse.OptionParser('Usage: %prog [options] version next_version')
  p.add_option('-j', '--host', dest='host', default='https://osgeo-org.atlassian.net', 
    help='JIRA host, default is atlassian') 
  p.add_option('-k', '--key', dest='key', default='GEOS',
    help='JIRA project key, default is GEOS') 
  p.add_option('-u', '--user', dest='user', default=getpass.getuser(), 
    help='JIRA user, default is current OS user')
  p.add_option('-p', '--passwd', dest='passwd', 
    help='JIRA password')
  p.add_option('-v', '--verbose', dest='verbose', action='store_true',
    help='Verbose flag')

  opts, args = p.parse_args()
  if len(args) < 2:
     p.error('Must specify version to release and next version')

  opts = vars(opts)
  jira = JIRA(opts['host'], opts['key'], opts['user'], opts['passwd'])

  verbose = opts['verbose']
  ver = args[0]
  next_ver = args[1]  

  ver_id = jira.get_version_id(ver)
  if not ver_id:
     sys.exit('Version %s does not exist in JIRA' % ver)

  if jira.is_version_released(ver) == True:
     print 'Version %s is already released in JIRA, exiting' % ver
     sys.exit(0)

  if not jira.get_version_id(next_ver): 
     # create id
     print 'creating version %s in JIRA' % next_ver
     jira.create_version(next_ver)

  if not jira.get_version_id(next_ver):
     sys.exit('Unable to create version %s in JIRA' % next_ver)

  # move over all open issues from old to new
  x = 0
  while x < 100:
    issues = jira.get_open_issues(ver)
    if len(issues) == 0:
      break

    for i in jira.get_open_issues(ver):
       if verbose == True:
         print 'Setting fix version of %s to %s' % (i, next_ver)
       jira.set_fixed_version(i, next_ver)
    x += 1

  # check all versions were moved
  if len(jira.get_open_issues(ver)) > 0:
    sys.exit('Unable to move back all issues to version %s' % next_ver)

  # mark the version as released
  print 'Releasing %s (%s) in JIRA' % (ver, ver_id)
  jira.release_version(ver) 
