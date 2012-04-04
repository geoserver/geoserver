insert into  users(name ,password,enabled) values ('admin' ,'geoserver','Y')
insert into  users(name ,password,enabled) values ('user1' ,'11111','Y')
insert into  users(name ,password,enabled) values ('user2' ,'22222','Y')
insert into  users(name ,password,enabled) values ('disableduser' ,'','N')

insert into userprops(username,propname,propvalue) values ('user2','mail','user2@gmx.com')
insert into userprops(username,propname,propvalue) values ('user2','tel','12-34-38')


insert into  groups(name ,enabled) values ('group1','y')
insert into  groups(name ,enabled) values ('admins','y');
insert into  groups(name ,enabled) values ('disabledgroup','n');

insert into groupmembers(groupname,username) values ('group1','user1')
insert into groupmembers(groupname,username) values ('group1','user2')
insert into groupmembers(groupname,username) values ('admins','admin')
insert into groupmembers(groupname,username) values ('disabledgroup','disableduser')










