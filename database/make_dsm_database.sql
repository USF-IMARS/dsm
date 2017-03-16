/* Use this script as database root to create the DSM database and give
	access to the dsm user from all the NISGS hosts.
*/

drop database if exists DSM;
create database DSM;

/* 	These two lines create a user 'dsm' with password 'b28c935'
	which can do anything to the DSM database, from any host.
	BOTH entries are required for the any-host access, as of
	MySQL 5.0.

	The dsm/b28c935 user/password is a hard-coded default in
	the system - it can be overridden with the properties
	DSM_DATABASE_USER and DSM_DATABASE_PASSWORD in the
	nisgs.properties file.
*/
	

grant all privileges on DSM.* to 'dsm'@'localhost' identified by 'b28c935';
grant all privileges on DSM.* to 'dsm'@'%' identified by 'b28c935';

