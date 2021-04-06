This is the minimal set of libraries required by YAJSW

It includes the following core functionalities:

All platforms
	* runConsole
	* filter triggers
	* logging
	* restart on exit code
	* network start
	
Windows
	* installService, uninstallService, startService, stopService
	
It does not include the following functions:

	* installDaemon, uninstallDaemon, startDaemon, stopDaemon
	* system tray icon
	* timers
	* services manager
	* network start per webdav or http or ftp
	* groovy scripts in configuration file
	* triggers with regular expressions