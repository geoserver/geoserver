insert into roles (name,parent) values ('ROLE_ADMINISTRATOR',null)
insert into roles (name,parent) values ('ROLE_AUTHENTICATED',null)
insert into roles (name,parent) values ('ROLE_WFS','ROLE_AUTHENTICATED')
insert into roles (name,parent) values ('ROLE_WMS','ROLE_AUTHENTICATED')

insert into roleprops(id, rolename,propname,propvalue) values ('ROLE_AUTHENTICATED','employee',null)
insert into roleprops(id, rolename,propname,propvalue) values ('ROLE_AUTHENTICATED','bbox','lookupAtRuntime')
insert into roleprops(id, rolename,propname,propvalue) values ('ROLE_AUTHENTICATED',null,'none')

insert into userroles(rolename,username) values ('ROLE_ADMINISTRATOR','admin')
insert into userroles(rolename,username) values ('ROLE_WFS','user1')
insert into userroles(rolename,username) values ('ROLE_WMS','user1')

insert into grouproles(rolename,groupname) values ('ROLE_WFS','g_wfs')
insert into grouproles(rolename,groupname) values ('ROLE_WMS','g_wms')
insert into grouproles(rolename,groupname) values ('ROLE_WFS','g_all')
insert into grouproles(rolename,groupname) values ('ROLE_WMS','g_all')


