; GEOSERVER INSTALLER SCRIPT FOR NSIS
;
;   NOTE: This script requires the following NSIS plugins:
;      -> AccessControl (https://nsis.sourceforge.io/AccessControl_plug-in)
;      -> ShellLink (https://nsis.sourceforge.io/ShellLink_plug-in)
;
; ----------------------------------------------------------------------------
; (c) 2021 Open Source Geospatial Foundation - all rights reserved
; This code is licensed under the GPL 2.0 license, available at the root
; application directory.
; ----------------------------------------------------------------------------

; Treat all warnings as errors, except warnings 6010 and 6020
!pragma warning error all
!pragma warning disable 6010 ; disable warning about functions not being referenced
!pragma warning disable 6020 ; disable warning about unused uninstaller script code

; Constants (VERSION should be updated by a script!)
!define APPNAME "GeoServer"                           ; application name
!define VERSION "2.19.0"                              ; application version
!define FULLVERSION "${VERSION}.0"                    ; full version (includes subversion)
!define FULLNAME "${APPNAME} ${VERSION}"              ; app name and version combined
!define FULLKEY "${APPNAME}-${VERSION}"               ; app name and version combined (delimited)
!define INSTNAME "${APPNAME}-install-${VERSION}.exe"  ; installer exe name
!define UNINNAME "${APPNAME}-uninstall.exe"           ; uninstaller exe name
!define HOMEPAGE "http://geoserver.org"               ; resource URL
!define TIMESTAMPURL "http://timestamp.digicert.com"  ; URL used to timestamp certificates
!define REQJREVERSION "1.8.0"                         ; required Java runtime version (i.e. 1.8.0)
!define REQJREVERSIONNAME "8"                         ; required Java runtime display version (i.e. 8)
!define ALTJREVERSION "11.0"                          ; alternative Java runtime version (i.e. 11.0)
!define ALTJREVERSIONNAME "11"                        ; alternative Java runtime display version (i.e. 11)
!define JDKNAME "AdoptOpenJDK"                        ; Name of the OpenJDK provider (e.g. AdoptOpenJDK)
!define JDKURL "https://adoptopenjdk.net"             ; OpenJDK URL
!define EMAIL "geoserver-users@lists.sourceforge.net" ; support email address
!define COPYRIGHT "Copyright (c) 1999-2021 Open Source Geospatial Foundation"

; CODE SIGNING
; ----------------------------------------------------------------------------
; Signing Windows executables on Linux can be done using Mono's Signcode tool.
; This tool is included in the "mono-devel" package.
; Signcode manual: https://www.mankier.com/1/signcode
; About signing:   https://developer.mozilla.org/en-US/docs/Mozilla/Developer_guide/Build_Instructions/Signing_an_executable_with_Authenticode
;
!define SIGNCOMMAND "signcode -spc $%OSGEO_PUBLISHER_CERT% \
                              -v $%OSGEO_PRIVATE_KEY% \
                              -a sha256 \
                              -n ${APPNAME} \
                              -i ${HOMEPAGE} \
                              -t ${TIMESTAMPURL} \
                              -tr 10"

; The sign command will be called after the (un)installer.exe files were build successfully.
!finalize "sleep 5"     ; Delay next step by 5 seconds to ensure file isn't locked by compiler process
!ifndef INNER
  !finalize "${SIGNCOMMAND} ..\target\${INSTNAME}"  ; Sign installer
!endif
; ----------------------------------------------------------------------------

; Global variables
Var AppKey
Var MenuFolder
Var ProgramData
Var JavaHome
Var JavaExe
Var JavaHomeTemp
Var JavaHomeHWND
Var BrowseJavaHWND
Var JavaPathCheck
Var LinkHWND
Var IsManual
Var Manual
Var Service
Var StartPort
Var StopPort
Var PortHWND
Var LocalUrl
Var DataDir
Var DataDirTemp
Var DataDirHWND
Var BrowseDataDirHWND
Var DataDirType
Var DefaultDataDir
Var DataDirPathCheck
Var GSUser
Var GSPass
Var GSUserHWND
Var GSPassHWND

; Version Information (for EXE properties version tab)
; In order for this to work, the VIProductVersion only accepts the format "1.2.3.4".
; A version string like "1.2.3-SNAPSHOT" or "1.2.3" will raise an error!
; ----------------------------------------------------------------------------
VIProductVersion "${FULLVERSION}"
VIAddVersionKey ProductName "${APPNAME}"
VIAddVersionKey LegalCopyright "${COPYRIGHT}"
VIAddVersionKey FileDescription "${APPNAME} Installer"
VIAddVersionKey FileVersion "${VERSION}"
VIAddVersionKey ProductVersion "${FULLVERSION}"
VIAddVersionKey Comments "${HOMEPAGE}"

; ----------------------------------------------------------------------------
; General settings

  ; Properly display all languages (if any)
  Unicode true

  ; Project name
  Name "${FULLNAME}"

  ; Default installation folder
  InstallDir "$PROGRAMFILES64\${APPNAME}"

  ; Get installation folder from registry if available
  InstallDirRegKey HKLM "Software\${APPNAME}" ""

  ; Request application privileges for Windows
  RequestExecutionLevel admin

  ; Setup script to run in 2 phases (to be able to sign the uninstaller)
  !ifdef INNER
    !echo "Inner invocation"

    ; Initially write temporary output
    OutFile "\tmp\tempinstaller.exe"
    SetCompress off

  !else
    !echo "Outer invocation"

    ; Call makensis again against current file, defining INNER.  
    ; This writes an installer for us which, when invoked, 
    ; will just write the uninstaller to some location, and then exit.
    !makensis '-V4 -DINNER "${__FILE__}"' = 0
  
    ; Now run that installer we just created as %TEMP%\tempinstaller.exe.
    ; Since it calls quit the return value isn't zero. 
    ; !system 'set __COMPAT_LAYER=RunAsInvoker&"$%TEMP%\tempinstaller.exe"' = 2
    ; TODO: RUN THIS USING WINE??
    !system "chmod a+rwx \tmp\tempinstaller.exe"
    !system "wine start /unix \tmp\tempinstaller.exe" = 2

    ; That will have written an uninstaller binary for us.
    ; Now we will digitally sign it. 
    !system "${SIGNCOMMAND} \tmp\${UNINNAME}" = 0

    ; Write the actual installer
    OutFile "..\target\${INSTNAME}"

    ; Compression options
    SetCompress auto
  !endif

; ----------------------------------------------------------------------------
; Modern interface Settings

  !include "MUI2.nsh"         ; Modern interface (v2)
  !include "StrFunc.nsh"      ; String functions
  !include "LogicLib.nsh"     ; ${If} ${Case} etc.
  !include "nsDialogs.nsh"    ; For Custom page layouts (radio buttons etc.)
  ${StrStr}
  ${StrCase}

  ; "Are you sure you wish to cancel" popup
  !define MUI_ABORTWARNING

  ; Images
  !define MUI_ICON img\installer.ico
  !define MUI_UNICON img\installer.ico
  !define MUI_WELCOMEFINISHPAGE_BITMAP img\installer.bmp
  !define MUI_UNWELCOMEFINISHPAGE_BITMAP img\installer.bmp

  ; Welcome text
  !define MUI_WELCOMEPAGE_TEXT "This wizard will guide you through the installation of ${FULLNAME}. \
                                $\r$\n$\r$\nIt is recommended that you close all other applications before running this setup. \
                                This will make it possible to update relevant system files without having to reboot your computer. \
                                $\r$\n$\r$\nClick Next to continue."

; ----------------------------------------------------------------------------
; Registry Settings

  ; Start Menu folder configuration
  !define MUI_STARTMENUPAGE_DEFAULTFOLDER "${APPNAME}"
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKLM" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${APPNAME}" 
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"

; ----------------------------------------------------------------------------
; Installer pages

  !insertmacro MUI_PAGE_WELCOME                             ; Hello
  !insertmacro MUI_PAGE_LICENSE license.rtf                 ; Show license
  Page custom SetJRE                                        ; Set the JRE
  !insertmacro MUI_PAGE_DIRECTORY                           ; Install location
  !insertmacro MUI_PAGE_STARTMENU Application $MenuFolder   ; Start menu location
  Page custom SetDataDir                                    ; Set the data directory
  Page custom SetCredentials                                ; Set admin/password (if new data_dir)
  Page custom SetPorts                                      ; Set Jetty web server ports
  Page custom SetInstallType InstallTypeLeave               ; Manual/Service
  Page custom ShowSummary                                   ; Summary page
  !insertmacro MUI_PAGE_INSTFILES                           ; Actually do the install
  !insertmacro MUI_PAGE_FINISH                              ; Done - link to readme
  
; Uninstaller pages (always uninstall everything)
  !ifdef INNER
    !define MUI_UNCONFIRMPAGE_TEXT_TOP "${APPNAME} will be completely removed from the following folder. Click Uninstall to start."
    !insertmacro MUI_UNPAGE_CONFIRM                         ; Are you sure?
    !insertmacro MUI_UNPAGE_INSTFILES                       ; Do the uninstall
  !endif

; ----------------------------------------------------------------------------
; Languages

  !insertmacro MUI_LANGUAGE "English" ; The first language is the default language

  ; Install options page headers
  LangString TEXT_JRE_TITLE ${LANG_ENGLISH} "Java Runtime Environment"
  LangString TEXT_JRE_SUBTITLE ${LANG_ENGLISH} "Set a valid JRE path."
  LangString TEXT_DATADIR_TITLE ${LANG_ENGLISH} "${APPNAME} Data Directory"
  LangString TEXT_DATADIR_SUBTITLE ${LANG_ENGLISH} "Set a valid ${APPNAME} data directory."
  LangString TEXT_CREDS_TITLE ${LANG_ENGLISH} "GeoServer Administrator"
  LangString TEXT_CREDS_SUBTITLE ${LANG_ENGLISH} "Set administrator credentials."
  LangString TEXT_TYPE_TITLE ${LANG_ENGLISH} "Execution Type"
  LangString TEXT_TYPE_SUBTITLE ${LANG_ENGLISH} "Run ${APPNAME} manually or as a service."
  LangString TEXT_PORT_TITLE ${LANG_ENGLISH} "${APPNAME} Web Server Port"
  LangString TEXT_PORT_SUBTITLE ${LANG_ENGLISH} "Set the port on which to run the ${APPNAME} web application."
  LangString TEXT_READY_TITLE ${LANG_ENGLISH} "Installation Summary"
  LangString TEXT_READY_SUBTITLE ${LANG_ENGLISH} "${APPNAME} is ready to be installed."

; ----------------------------------------------------------------------------
; Reserve Files
  
  ; If using solid compression, files that are required before
  ; the actual installation should be stored first in the data block,
  ; because this will make the installer start faster.
  !insertmacro MUI_RESERVEFILE_LANGDLL

; ----------------------------------------------------------------------------
; FUNCTIONS

; Called on initialization of the installer
Function .onInit

  !ifdef INNER
    ; If INNER is defined, then we aren't supposed to do anything except write out
    ; the uninstaller.  This is better than processing a command line option as it means
    ; this entire code path is not present in the final (real) installer.
    SetSilent silent
    WriteUninstaller "\tmp\${UNINNAME}"

    ; Bail out quickly when running the "inner" installer
    Quit
  !endif

  Call CheckIfInstalled                             ; Quit the installer if the application has been installed already
  ${StrCase} $AppKey "${APPNAME}" "L"               ; Set AppKey to lowercase application name
  ReadEnvStr $ProgramData PROGRAMDATA               ; Read %PROGRAMDATA% environment variable
  StrCpy $DefaultDataDir "$ProgramData\${APPNAME}"  ; Set default data directory
  StrCpy $INSTDIR "$PROGRAMFILES64\${APPNAME}"      ; Set default installation directory
  StrCpy $IsManual 0                                ; Set to run as a Windows service by default
  Call CheckUserType                                ; Verifies that the user has administrative privileges
  Call FindJavaHome                                 ; Set the $JavaHome variable (if any)
  Call FindDataDir                                  ; Find existing data directory from %GEOSERVER_DATA_DIR%

FunctionEnd

; Checks if GeoServer has already been installed (also previous versions)
Function CheckIfInstalled

  ClearErrors
  ReadRegStr $0 HKLM "Software\${APPNAME}" ""
  ${IfNot} ${Errors}    
    MessageBox MB_ICONSTOP "${APPNAME} has already been installed on your system.$\r$\n \
                            Please remove that version if you wish to update or re-install."
    Quit
  ${EndIf}

FunctionEnd

; Check the user type, and quit if it's not an administrator.
Function CheckUserType
  ClearErrors
  UserInfo::GetName
  IfErrors Unsupported
  Pop $0
  UserInfo::GetAccountType
  Pop $1
  StrCmp $1 "Admin" IsAdmin NoAdmin

  NoAdmin:
    MessageBox MB_ICONSTOP "Sorry, you must have administrative privileges in order to install ${APPNAME}."
    Quit

  Unsupported:
    MessageBox MB_ICONSTOP "Sorry, this installer cannot be run on this Windows version."
    Quit
		
  IsAdmin:
    StrCpy $1 "" ; zero out variable
	
FunctionEnd

; Remove trailing slash from path
Function RemoveTrailingSlash
  ClearErrors
  Pop $6

  StrCpy $5 "$6" "" -1  ; Read the last char
  StrCmp $5 "\" 0 +2    ; Check if last char is a backslash
  StrCpy $6 "$6" -1     ; Last char was '\', so remove it
  StrCpy $5 ""          ; Cleanup
  Push $6

FunctionEnd

; Find the %JAVA_HOME% used on the system and assign it to $JavaHome global
; Will check environment variables and the registry (both JRE and JDK)
; Will set an empty string if the path cannot be determined
Function FindJavaHome

  ClearErrors

  ReadEnvStr $1 JAVA_HOME
  IfErrors Cleanup End    ; found in the environment variable

  ; No env var set
  ClearErrors
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "JavaHome"

  IfErrors Cleanup End    ; found in the registry (JRE)

  ; No JRE regkey set
  ClearErrors
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$2" "JavaHome"

  IfErrors Cleanup End    ; found in the registry (JDK)

  End:
    ${If} ${FileExists} "$1\*.*"
      ; Set JavaHome variable
      Push $1
      Call RemoveTrailingSlash
      Pop $JavaHome
    ${Else}
      StrCpy $JavaHome ""
    ${EndIf}   
    Goto Cleanup

  Cleanup:
    StrCpy $1 ""
    StrCpy $2 ""   

FunctionEnd

; JRE page display
Function SetJRE

  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"

  StrCpy $JavaHomeTemp $JavaHome

  nsDialogs::Create 1018

  ; ${NSD_Create*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 56u "Please select the path to your Java Runtime Environment (JRE). \
                                   $\r$\n$\r$\n${APPNAME} requires a 64-Bit Java JRE or JDK (${REQJREVERSIONNAME} \
                                   or ${ALTJREVERSIONNAME}). \
                                   $\r$\n$\r$\nIf you don't have a (valid) JRE installed, you can click on \
                                   the link below to download and install the correct JRE for your system."

  ${NSD_CreateLink} 0 60u 100% 12u "Visit ${JDKNAME} website"
  Pop $LinkHWND
  ${NSD_OnClick} $LinkHWND OpenDownloadLink

  ${NSD_CreateDirRequest} 0 90u 240u 13u $JavaHomeTemp
  Pop $JavaHomeHWND
  ${NSD_OnChange} $JavaHomeHWND ValidateJava

  ${NSD_CreateBrowseButton} 242u 90u 50u 13u "Browse..."
  Pop $BrowseJavaHWND
  ${NSD_OnClick} $BrowseJavaHWND BrowseJava

  ${NSD_CreateLabel} 0 106u 100% 12u " "
  Pop $JavaPathCheck

  Push $JavaHomeHWND
  Call ValidateJava

  nsDialogs::Show

FunctionEnd

; Go to Open JDK download page
Function OpenDownloadLink
  ExecShell "open" "${JDKURL}"
FunctionEnd

; Checks if the 64-bits Java executable can be found in the given JAVA_HOME
; Prevents the installer from continuing if the JRE is invalid
Function ValidateJava

  Pop $8
  ${NSD_GetText} $8 $JavaHomeTemp

  StrCpy $JavaExe "$JavaHomeTemp\bin\java.exe"
  IfFileExists $JavaExe ReadVersionInfo Errors

  ReadVersionInfo:
    ; Call java.exe with -version param
    nsExec::ExecToStack '"$JavaExe" -version'
    Pop $R0         ; Return code
    Pop $R1         ; Read stdout 

    ${StrStr} $R2 $R1 "64-Bit"              ; Find "64-Bit" substring in Java version info
    ${StrStr} $R3 $R1 "${REQJREVERSION}"    ; Find "1.8.0" substring in Java version info
    ${StrStr} $R4 $R1 "${ALTJREVERSION}"    ; Find "11.0" substring in Java version info

    ${If} $R2 != ""
    ${AndIf} $R3 != ""
      Goto ReqVersionFound      ; "64-Bit" and "1.8.0" substring found
    ${EndIf}

    ${If} $R2 != ""
    ${AndIf} $R4 != ""
      Goto AltVersionFound      ; "64-Bit" and "11.0" substring found
    ${EndIf}

  Errors:
    ${NSD_SetText} $JavaPathCheck "This path does not contain a valid 64-bit JRE (${REQJREVERSIONNAME} \
                                   or ${ALTJREVERSIONNAME})"
    GetDlgItem $0 $HWNDPARENT 1   ; Next button
    EnableWindow $0 0             ; Disable
    StrCpy $JavaHome ""
    Goto End

  ReqVersionFound:
    ${NSD_SetText} $JavaPathCheck "This path contains a valid 64-bit JRE ${REQJREVERSIONNAME}"
    GetDlgItem $0 $HWNDPARENT 1 ; Next button
    EnableWindow $0 1           ; Enable
    StrCpy $JavaHome $JavaHomeTemp
    Goto End

  AltVersionFound:
    ${NSD_SetText} $JavaPathCheck "This path contains a valid 64-bit JRE ${ALTJREVERSIONNAME}"
    GetDlgItem $0 $HWNDPARENT 1 ; Next button
    EnableWindow $0 1           ; Enable
    StrCpy $JavaHome $JavaHomeTemp
    Goto End

  End:
    ClearErrors
    StrCpy $JavaExe ""

FunctionEnd

; Brings up folder dialog for the JRE
Function BrowseJava

  nsDialogs::SelectFolderDialog "Please select the location of your 64-bit JRE (${REQJREVERSIONNAME} \
                                 or ${ALTJREVERSIONNAME})..." $PROGRAMFILES64
  Pop $1

  ${If} $1 != "error"   ; i.e. didn't hit cancel
    ${NSD_SetText} $JavaHomeHWND $1
  ${EndIf}

FunctionEnd

; Find the %GEOSERVER_DATA_DIR% used on the system and set $DataDir variable
Function FindDataDir

  ClearErrors
  ReadEnvStr $1 GEOSERVER_DATA_DIR
  Push $1
  Call RemoveTrailingSlash
  Pop $1

  ${If} ${Errors}
  ${OrIfNot} ${FileExists} "$1\*.*"
    Goto DefaultDataDir
  ${Else}
    Goto ExistingDataDir
  ${EndIf}

  ExistingDataDir:
    ClearErrors
    StrCpy $DataDir $1
    StrCpy $DataDirType 1
    Goto Cleanup

  DefaultDataDir:
    ; Existing GeoServer data dir not found: set to %PROGRAMDATA%\GeoServer dir
    ClearErrors
    StrCpy $DataDir $DefaultDataDir
    StrCpy $DataDirType 0
    Goto Cleanup

  Cleanup:
    StrCpy $1 ""

FunctionEnd

; Data directory config page
Function SetDataDir

  !insertmacro MUI_HEADER_TEXT "$(TEXT_DATADIR_TITLE)" "$(TEXT_DATADIR_SUBTITLE)"

  StrCpy $DataDirTemp $DataDir

  nsDialogs::Create 1018

  ; ${NSD_Create*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 56u "If a valid existing ${APPNAME} data directory path was found on your system, \
                                   it should be displayed below. \
                                   $\r$\nIf it was not found, a default directory is suggested. \
                                   Alternatively, you can specify another (existing) data directory."

  ${NSD_CreateDirRequest} 0 90u 240u 13u $DataDirTemp
  Pop $DataDirHWND
  ${NSD_OnChange} $DataDirHWND ValidateDataDir

  ${NSD_CreateBrowseButton} 242u 90u 50u 13u "Browse..."
  Pop $BrowseDataDirHWND
  ${NSD_OnClick} $BrowseDataDirHWND BrowseDataDir

  ${NSD_CreateLabel} 0 106u 100% 12u " "
  Pop $DataDirPathCheck

  Push $DataDirHWND
  Call ValidateDataDir

  nsDialogs::Show

FunctionEnd

; Checks if the specified data directory is valid.
; Prevents the installer from continuing if it's invalid.
Function ValidateDataDir

    Pop $7
    ${NSD_GetText} $7 $DataDirTemp

    ${IfNot} ${FileExists} "$DataDirTemp\*.*"
      ; The directory does not exist: this is valid (it will be created)
      ${NSD_SetText} $DataDirPathCheck "The data directory will be created at this path"
      StrCpy $DataDirType 0
      Goto IsValid
    ${EndIf}

    ${If} ${FileExists} "$DataDirTemp\global.xml"       ; 2.0+ data dir
    ${OrIf} ${FileExists} "$DataDirTemp\catalog.xml"    ; 1.7 data dir
      ${NSD_SetText} $DataDirPathCheck "This path contains a valid existing data directory"
      StrCpy $DataDirType 1
      Goto IsValid
    ${EndIf}

    ${NSD_SetText} $DataDirPathCheck "This path does not contain a valid data directory"
    GetDlgItem $0 $HWNDPARENT 1     ; Next
    EnableWindow $0 0               ; Disable
    StrCpy $DataDir ""

    IsValid:
      GetDlgItem $0 $HWNDPARENT 1   ; Next
      EnableWindow $0 1             ; Enable
      StrCpy $DataDir $DataDirTemp  ; Set $DataDir variable to valid path
      ClearErrors

FunctionEnd

; Brings up folder dialog for the data directory
Function BrowseDataDir

  nsDialogs::SelectFolderDialog "Please select a valid data directory..." $ProgramData
  Pop $1

  ${If} $1 != "error"   ; i.e. didn't hit cancel
    ${NSD_SetText} $DataDirHWND $1
  ${EndIf}

FunctionEnd

; Input GS admin credentials for existing data directory
Function SetCredentials

  ; Check if the data dir already exists
  StrCmp $DataDirType 1 SkipCreds

  !insertmacro MUI_HEADER_TEXT "$(TEXT_CREDS_TITLE)" "$(TEXT_CREDS_SUBTITLE)"
  nsDialogs::Create 1018

  ; Populates defaults on first display, and resets to default user blanked any of the values
  StrCmp $GSUser "" 0 +3
    StrCpy $GSUser "admin"
    StrCpy $GSPass "geoserver"
  StrCmp $GSPass "" 0 +3
    StrCpy $GSUser "admin"
    StrCpy $GSPass "geoserver"

  ;Syntax: ${NSD_*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 36u "Please set the administrator username and password for GeoServer:"

  ${NSD_CreateLabel} 20u 40u 40u 14u "Username"
  ${NSD_CreateText} 70u 38u 50u 14u $GSUser
  Pop $GSUserHWND
  ${NSD_OnChange} $GSUserHWND UsernameCheck

  ${NSD_CreateLabel} 20u 60u 40u 14u "Password"
  ${NSD_CreateText} 70u 58u 50u 14u $GSPass
  Pop $GSPassHWND
  ${NSD_OnChange} $GSPassHWND PasswordCheck

  nsDialogs::Show

  SkipCreds:
    ; If data dir exists, we don't want to change credentials

FunctionEnd

; When username value is changed (realtime)
Function UsernameCheck

  ; Check for illegal values of $GSUser and fix immediately
  ${NSD_GetText} $GSUserHWND $GSUser
  StrCmp $GSUser "" NoContinue Continue

  NoContinue:
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 0 ; Disable
    Goto End
  Continue:
  StrCmp $GSPass "" +3 0 ; must make sure neither is blank
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 1 ; Enable
  End:


FunctionEnd

; When password value is changed (realtime)
Function PasswordCheck

  ; Check for illegal values of $GSPass and fix immediately
  ${NSD_GetText} $GSPassHWND $GSPass
  StrCmp $GSPass "" NoContinue Continue

  NoContinue:
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 0 ; Disable
    Goto End
  Continue:
  StrCmp $GSUser "" +3 0 ; must make sure neither is blank
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 1 ; Enable
  End:

FunctionEnd

; Find Marlin JAR file and write service wrapper configuration to enable the Marlin renderer
Function SetMarlinRendererService

  ClearErrors
  FindFirst $0 $1 "$INSTDIR\webapps\geoserver\WEB-INF\lib\marlin*.jar"
  IfErrors End

  FileOpen $9 "$INSTDIR\wrapper\marlin.conf" w ; Opens a Empty File and fills it
  FileWrite $9 '# Marlin Renderer$\r$\n'
  FileWrite $9 'set.default.MARLIN_JAR=$1$\r$\n'
  FileWrite $9 'set.default.GEOSERVER_HOME=$INSTDIR$\r$\n'
  FileWrite $9 'wrapper.java.additional.4=-Xbootclasspath/a:"%GEOSERVER_HOME%\webapps\geoserver\WEB-INF\lib\%MARLIN_JAR%"$\r$\n'
  FileWrite $9 'wrapper.java.additional.5=-Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine'
  FileClose $9 ; Closes the file

  End:
    FindClose $0
    ClearErrors

FunctionEnd

; Set the web server port
Function SetPorts

  !insertmacro MUI_HEADER_TEXT "$(TEXT_PORT_TITLE)" "$(TEXT_PORT_SUBTITLE)"
  nsDialogs::Create 1018

  ; Populates defaults on first display, and resets to default user blanked any of the values
  StrCmp $StartPort "" 0 +2
  StrCpy $StartPort "8080"
  
  Call SetStopPort

  ;Syntax: ${NSD_*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 26u "Specify the ${APPNAME} web server port. When in doubt, use the default ($StartPort)."

  ${NSD_CreateLabel} 0 30u 20u 14u "Port"  
  ${NSD_CreateNumber} 25u 28u 50u 14u $StartPort
  Pop $PortHWND
  ${NSD_OnChange} $PortHWND PortCheck

  ${NSD_CreateLabel} 85u 30u 120u 14u "Valid range is 80, 1024-65535." 

  nsDialogs::Show

FunctionEnd

; Set $StopPort variable based on $StartPort
; Also sets localized URL based on start port (while we're here)
Function SetStopPort

  IntOp $StopPort $StartPort - 1
  StrCpy $LocalUrl "http://localhost:$StartPort/$AppKey"

FunctionEnd

; When port value is changed (realtime)
Function PortCheck

  ; Check for illegal values of $StartPort and fix immediately
  ${NSD_GetText} $PortHWND $StartPort

  ; Check for illegal values of $StartPort
  ${If} $StartPort = 80
    GetDlgItem $0 $HWNDPARENT 1     ; Next
    EnableWindow $0 1               ; Enable
  ${Else}  
     ${If} $StartPort < 1024        ; Too low
     ${OrIf} $StartPort > 65535     ; Too high
      GetDlgItem $0 $HWNDPARENT 1   ; Next
      EnableWindow $0 0             ; Disable
     ${Else}
      GetDlgItem $0 $HWNDPARENT 1   ; Next
      EnableWindow $0 1             ; Enable
     ${EndIf}
   ${EndIf}

   Call SetStopPort

FunctionEnd

; Manual vs service selection
Function SetInstallType

  nsDialogs::Create 1018

  !insertmacro MUI_HEADER_TEXT "$(TEXT_TYPE_TITLE)" "$(TEXT_TYPE_SUBTITLE)"

  ;Syntax: ${NSD_*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 24u "Select how you wish to run ${APPNAME}.$\r$\nIf you don't know which option to choose, select the $\"Install as a service$\" option."
  ${NSD_CreateRadioButton} 10u 28u 50% 12u "Install as a service (recommended)"
  Pop $Service
  ${NSD_CreateLabel} 10u 44u 90% 24u "Installed for all users. Runs as a Windows Service for greater security."

  ${NSD_CreateRadioButton} 10u 72u 50% 12u "Run manually"
  Pop $Manual
  ${NSD_CreateLabel} 10u 88u 90% 24u "Installed for the current user. Must be manually started and stopped."

  ${If} $IsManual == 1
    ${NSD_Check} $Manual ; Default
  ${Else}
    ${NSD_Check} $Service
  ${EndIf}

  nsDialogs::Show

FunctionEnd

; Records the final state of manual vs service
Function InstallTypeLeave

  ${NSD_GetState} $Manual $IsManual
  ; $IsManual = 1 -> Run manually
  ; $IsManual = 0 -> Run as service
FunctionEnd

; Summary page before install
Function ShowSummary

  nsDialogs::Create 1018
  !insertmacro MUI_HEADER_TEXT "$(TEXT_READY_TITLE)" "$(TEXT_READY_SUBTITLE)"

  ;Syntax: ${NSD_*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 24u "Please review the settings below and click the Back button if \
                                   changes need to be made. Click the Install button to continue."

  ; Directory
  ${NSD_CreateLabel} 10u 25u 35% 24u "Installation directory:"
  ${NSD_CreateLabel} 40% 25u 60% 24u "$INSTDIR"

  ; Install type
  ${NSD_CreateLabel} 10u 45u 35% 24u "Installation type:"
  ${If} $IsManual == 1
    ${NSD_CreateLabel} 40% 45u 60% 24u "Run manually"
  ${Else}
    ${NSD_CreateLabel} 40% 45u 60% 24u "Installed as a service"
  ${EndIf}
 
  ; JRE
  ${NSD_CreateLabel} 10u 65u 35% 24u "Java Runtime Environment:"
  ${NSD_CreateLabel} 40% 65u 60% 24u "$JavaHome"

  ; Server Port
  ${NSD_CreateLabel} 10u 85u 35% 24u "Web server port:"
  ${NSD_CreateLabel} 40% 85u 60% 24u "$StartPort"

  nsDialogs::Show

FunctionEnd

; Write an environment variable
Function WriteEnvVar

  Pop $4
  Pop $3

  WriteRegExpandStr HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" $3 $4
  SendMessage ${HWND_BROADCAST} ${WM_WININICHANGE} 0 "STR:Environment" /TIMEOUT=5000

  StrCpy $3 ""
  StrCpy $4 ""

FunctionEnd

; Remove an environment variable
Function un.DeleteEnvVar

  Pop $3
  DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" $3
  StrCpy $3 ""

FunctionEnd

; ----------------------------------------------------------------------------
; SECTIONS

; The main install section
Section "GeoServer" SectionMain

  SectionIn RO ; Makes this install mandatory
  SetOverwrite on

  DetailPrint "Copying program files..."

  ; Create program folder and copy files only from unzipped binary distribution
  CreateDirectory "$INSTDIR"
  CopyFiles /SILENT /FILESONLY "source\*.*" "$INSTDIR"

  ; Copy relevant folders from unzipped binary distribution (recursively)
  SetOutPath "$INSTDIR"
  File /r "source\etc"
  File /r "source\modules"
  File /r "source\lib"
  File /r "source\logs"
  File /r "source\resources"
  File /r "source\webapps"

  ; Copy icon files
  CreateDirectory "$INSTDIR\ico"
  SetOutPath "$INSTDIR\ico"
  File "/oname=gs.ico" "img\installer.ico"
  File "img\start.ico"
  File "img\stop.ico"
  File "img\info.ico"

  ; Special handling of the 'data_dir'
  ${If} $DataDirType == 0
    ; Only place files, when NOT using an existing folder
    DetailPrint "Creating data directory..."
    CreateDirectory $DataDir
    ${If} ${Errors}
    ${AndIfNot} ${FileExists} "$DataDir\*.*"
      DetailPrint "Failed to create data directory!"
      MessageBox mb_IconStop|mb_TopMost|mb_SetForeground "Cannot create data directory $DataDir"
      Abort
    ${EndIf}
    SetOutPath $DataDir
    File /r data_dir
  ${EndIf}

  DetailPrint "Writing environment variables..."

  ; Write environment variables
  Push "JAVA_HOME"
  Push "$JavaHome"
  Call WriteEnvVar

  Push "GEOSERVER_HOME"
  Push "$INSTDIR"
  Call WriteEnvVar

  Push "GEOSERVER_DATA_DIR"
  Push "$DataDir"
  Call WriteEnvVar

  ; Always create the scripts folder
  CreateDirectory "$INSTDIR\scripts"

  ${If} $IsManual == 0 ; install as service

    DetailPrint "Setting up Windows service..."

    ; Copy YAJSW java service wrapper folder (recursively)
    SetOutPath "$INSTDIR"
    File /r wrapper

    ; Copy geoserver.bat file
    SetOutPath "$INSTDIR\scripts"
    File geoserver.bat

    ; Create Java IO temp dir
    CreateDirectory "$INSTDIR\work"

    Call SetMarlinRendererService
    
    ; Install the service using YAJSW (but don't start yet)
    nsExec::Exec "$INSTDIR\scripts\geoserver.bat -i wrapper.app.parameter.4=jetty.port=$StartPort"

  ${Else}   ; manual install

    ; Copy start/stop batch files to scripts folder
    SetOutPath "$INSTDIR\scripts"
    File /r "source\bin\*.bat"

  ${EndIf}

  ; Grant full access to directories that require them:
  ; - Logging directory
  ; - Data directory
  ; This requires the NSIS AccessControl plugin at https://nsis.sourceforge.io/AccessControl_plug-in
  ; For more info, see https://nsis.sourceforge.io/How_can_I_install_a_plugin#NSIS_plugin_installation
  DetailPrint "Granting access..."
  ${If} $IsManual == 1      ; manual run -> grant to Users group
    AccessControl::GrantOnFile "$INSTDIR\logs" "(S-1-5-32-545)" "FullAccess"
    AccessControl::GrantOnFile "$DataDir" "(S-1-5-32-545)" "FullAccess"
  ${ElseIf} $IsManual == 0  ; run as service -> grant to NT AUTHORITY\Network Service
    AccessControl::GrantOnFile "$INSTDIR\logs" "(S-1-5-20)" "FullAccess"
    AccessControl::GrantOnFile "$DataDir" "(S-1-5-20)" "FullAccess"
  ${EndIf}

SectionEnd

!ifndef INNER

  ; What happens at the end of the install
  Section -FinishSection

    ${If} $IsManual == 0  ; service

      DetailPrint "Starting Windows service..."

      ; Start the installed service
      nsExec::Exec 'net start "${FULLNAME}"'

      ; Add simple start/stop service batch files
      SetOutPath "$INSTDIR\scripts"

      FileOpen $9 startup.bat w   ; Creates a new bat file and opens it
      FileWrite $9 'net start "${FULLNAME}"'
      FileClose $9                ; Closes the file

      FileOpen $9 shutdown.bat w  ; Creates a new bat file and opens it
      FileWrite $9 'net stop "${FULLNAME}"'
      FileClose $9                ; Closes the file

    ${EndIf}
    
    DetailPrint "Creating Start Menu shortcuts..."

    ; Begin writing Start Menu
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application

      ; Create Start Menu folder
      CreateDirectory "$SMPROGRAMS\$MenuFolder"

      ; Create shortcut to official homepage    
      CreateShortCut "$SMPROGRAMS\$MenuFolder\About ${APPNAME}.lnk" "${HOMEPAGE}" "" "$INSTDIR\ico\info.ico" 0
      
      ; Create shortcut to application webpage
      CreateShortCut "$SMPROGRAMS\$MenuFolder\${APPNAME} Web Portal.lnk" "$LocalUrl" "" "$INSTDIR\ico\gs.ico" 0

      ; Create batch file shortcuts in appropriate target directory and move them to Start Menu folder
      ; Also make sure that they are executed as administrator (else they won't work)
      SetOutPath "$INSTDIR\scripts"
      CreateShortCut "$INSTDIR\scripts\Start ${APPNAME}.lnk" "$INSTDIR\scripts\startup.bat" "" "$INSTDIR\ico\start.ico" 0
      CreateShortCut "$INSTDIR\scripts\Stop ${APPNAME}.lnk" "$INSTDIR\scripts\shutdown.bat" "" "$INSTDIR\ico\stop.ico" 0
      Rename "$INSTDIR\scripts\Start ${APPNAME}.lnk" "$SMPROGRAMS\$MenuFolder\Start ${APPNAME}.lnk"
      Rename "$INSTDIR\scripts\Stop ${APPNAME}.lnk" "$SMPROGRAMS\$MenuFolder\Stop ${APPNAME}.lnk"
      ShellLink::SetRunAsAdministrator "$SMPROGRAMS\$MenuFolder\Start ${APPNAME}.lnk"
      ShellLink::SetRunAsAdministrator "$SMPROGRAMS\$MenuFolder\Stop ${APPNAME}.lnk"

    !insertmacro MUI_STARTMENU_WRITE_END

    DetailPrint "Writing registry keys..."

    ; Registry
    WriteRegStr HKLM "Software\${APPNAME}" "" "$INSTDIR"

    ; For the Add/Remove programs area
    !define UNINSTALLREGPATH "Software\Microsoft\Windows\CurrentVersion\Uninstall"
    WriteRegStr HKLM "${UNINSTALLREGPATH}\${FULLKEY}" "DisplayName" "${FULLNAME}"
    WriteRegStr HKLM "${UNINSTALLREGPATH}\${FULLKEY}" "UninstallString" "$INSTDIR\${UNINNAME}"
    WriteRegStr HKLM "${UNINSTALLREGPATH}\${FULLKEY}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "${UNINSTALLREGPATH}\${FULLKEY}" "DisplayIcon" "$INSTDIR\ico\gs.ico"
    WriteRegStr HKLM "${UNINSTALLREGPATH}\${FULLKEY}" "HelpLink" "${HOMEPAGE}"
    WriteRegDWORD HKLM "${UNINSTALLREGPATH}\${FULLKEY}" "NoModify" "1"
    WriteRegDWORD HKLM "${UNINSTALLREGPATH}\${FULLKEY}" "NoRepair" "1"

    ; Package the signed uninstaller
    SetOutPath "$INSTDIR"
    File "\tmp\${UNINNAME}"

  SectionEnd

!else

  ; Uninstall section
  Section Uninstall

    ; Call FindDataDir

    ; Stop GeoServer
    DetailPrint "Stopping ${FULLNAME}..."
    nsExec::Exec "$INSTDIR\scripts\shutdown.bat"
    Sleep 4000    ; make sure it's fully stopped

    IfFileExists "$INSTDIR\wrapper\*.*" 0 +5
      DetailPrint "Removing ${FULLNAME} service..."
      nsExec::Exec "$INSTDIR\scripts\geoserver.bat -r"
      Sleep 4000  ; make sure it's fully removed
      RMDir /r "$INSTDIR\wrapper" ; while we're here

    DetailPrint "Removing environment variables and registry keys..."

    ; Remove GEOSERVER_HOME environment var (but keep JAVA_HOME)
    Push GEOSERVER_HOME
    Call un.DeleteEnvVar

    ; Remove from registry...
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${FULLKEY}"
    DeleteRegKey HKLM "SOFTWARE\${APPNAME}"

    DetailPrint "Removing shortcuts..."
    
    ; Delete Shortcuts
    RMDir /r "$SMPROGRAMS\${APPNAME}"

    DetailPrint "Removing program files..."

    ; Delete files/folders
    RMDir /r "$INSTDIR\scripts"
    RMDir /r "$INSTDIR\etc"
    RMDir /r "$INSTDIR\modules"
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\logs"
    RMDir /r "$INSTDIR\resources"
    RMDir /r "$INSTDIR\work"        ; working data
    RMDir /r "$INSTDIR\webapps"
    RMDir /r "$INSTDIR\v*"          ; EPSG DB
    Delete "$INSTDIR\*.*"

    RMDir "$INSTDIR" ; no /r!

    IfFileExists "$INSTDIR\*.*" 0 +2
      MessageBox MB_OK|MB_ICONEXCLAMATION "WARNING - Some files or folders could not be removed from:$\r$\n$INSTDIR"

    IfFileExists "$DataDir\*.*" 0 Finish
      ; Ask to also remove the data directory. For silent uninstalls, we will KEEP the data directory.
      MessageBox MB_YESNO "Would you like to remove all ${APPNAME} data in $DataDir?$\r$\n\
                           If you ever wish to reinstall ${APPNAME}, this is not recommended." /SD IDNO IDYES RemoveData
      RemoveData:
        RMDir /r "$DataDir\*.*"
        RMDir "$DataDir"
        IfFileExists "$DataDir\*.*" 0 RemoveEnvVar
          MessageBox MB_OK|MB_ICONEXCLAMATION "WARNING - Some files or folders could not be removed from:$\r$\n$DataDir"

      RemoveEnvVar:
        ; Delete environment var if data dir was successfully removed
        Push GEOSERVER_DATA_DIR
        Call un.DeleteEnvVar
        Goto Finish

    Finish:
      DetailPrint "Successfully uninstalled ${FULLNAME}"

  SectionEnd

!endif
