; GEOSERVER WINDOWS INSTALLER SCRIPT FOR NSIS
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
!define APPNAME "GeoServer"                                     ; application name
!ifndef VERSION
  !searchparse /file ..\source\VERSION.txt `version = ` VERSION   ; Read version from VERSION.txt
!endif
!define FULLNAME "${APPNAME} ${VERSION}"                        ; app name and version combined
!define FULLKEY "${APPNAME}-${VERSION}"                         ; app name and version combined (delimited)
!define INSTNAME "${APPNAME}-install-${VERSION}.exe"            ; installer exe name
!define UNINNAME "${APPNAME}-uninstall.exe"                     ; uninstaller exe name
!define HOMEPAGE "http://geoserver.org"                         ; resource URL
!define TIMESTAMPURL "http://timestamp.comodoca.com/rfc3161"    ; URL used to timestamp certificates
!define REQJREVERSION "1.8.0"                                   ; required Java runtime version (i.e. 1.8.0)
!define REQJREVERSIONNAME "8"                                   ; required Java runtime display version (i.e. 8)
!define ALTJREVERSION "11.0"                                    ; alternative Java runtime version (i.e. 11.0)
!define ALTJREVERSIONNAME "11"                                  ; alternative Java runtime display version (i.e. 11)
!define JDKNAME "AdoptOpenJDK"                                  ; Name of the OpenJDK provider (e.g. AdoptOpenJDK)
!define JDKURL "https://adoptopenjdk.net"                       ; OpenJDK URL
!define EMAIL "geoserver-users@lists.sourceforge.net"           ; support email address
!define COPYRIGHT "Copyright (c) 1999-2021 Open Source Geospatial Foundation"
!define CERT_SUBJECT "The Open Source Geospatial Foundation"    ; Certificate subject

; CODE SIGNING
; ----------------------------------------------------------------------------
; Signing requires Windows SIGNTOOL.EXE (32-bits, because NSIS is 32-bits)
; This can be installed as a feature of MS Visual Studio (ClickOnce) or as part of the Windows SDK.
;
; Common directories where the signtool.exe can be found are:
; C:\Program Files (x86)\Microsoft SDKs\ClickOnce\SignTool
; C:\Program Files (x86)\Windows Kits\10\bin\<version>\<architecture>
; C:\Program Files (x86)\Windows Kits\10\App Certification Kit
;
; IMPORTANT: the signtool.exe directory MUST be added to the Windows PATH environment variable!!
!define SIGNCOMMAND "signtool sign  /v /n $\"${CERT_SUBJECT}$\" /sm /d ${APPNAME} /du https://www.osgeo.org /tr http://timestamp.comodoca.com/rfc3161 /td sha256"

; The sign command will be called after the (un)installer.exe files were build successfully.
; Make sure that the private certificate (*.pfx) is installed in the certificate store (for user, not machine).
; Note: installing the certificate might require a password.
; Once the certificate is installed, the finalize steps below should run without issues.
!finalize "PING -n 5 127.0.0.1 >nul"                ; Delay next step to ensure file isn't locked by compiler process
!ifndef INNER
  !finalize "${SIGNCOMMAND} ..\target\${INSTNAME}"  ; Sign installer with SHA1
  !finalize 'DEL /F /Q "wrapper\${APPNAME}.exe"'    ; Remove the signed wrapper
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

  ; Compression options
  !ifdef INNER
    !echo "Inner invocation"

    ; Initially write temporary output
    OutFile "$%TEMP%\tempinstaller.exe"
    SetCompress off

  !else
    !echo "Outer invocation"

    ; Call makensis again against current file, defining INNER.  
    ; This writes an installer for us which, when invoked, 
    ; will just write the uninstaller to some location, and then exit.
    !makensis '/DINNER /DVERSION=${VERSION} "${__FILE__}"' = 0
  
    ; Now run that installer we just created as %TEMP%\tempinstaller.exe.
    ; Since it calls quit the return value isn't zero. 
    !system 'set __COMPAT_LAYER=RunAsInvoker&"$%TEMP%\tempinstaller.exe"' = 2
  
    ; That will have written an uninstaller binary for us.
    ; Now we will digitally sign it. 
    !system "${SIGNCOMMAND} $%TEMP%\${UNINNAME}" = 0

    ; Write the real installer
    OutFile "..\target\${INSTNAME}"
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
  ${StrLoc}

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
    WriteUninstaller "$%TEMP%\${UNINNAME}"

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
!macro GlobalRemoveTrailingSlash un
Function ${un}RemoveTrailingSlash
  ClearErrors
  Pop $6

  StrCpy $5 "$6" "" -1  ; Read the last char
  StrCmp $5 "\" 0 +2    ; Check if last char is a backslash
  StrCpy $6 "$6" -1     ; Last char was '\', so remove it
  StrCpy $5 ""          ; Cleanup
  Push $6

FunctionEnd
!macroend

; make RemoveTrailingSlash function available both for installer and uninstaller
!insertmacro GlobalRemoveTrailingSlash ""
!insertmacro GlobalRemoveTrailingSlash "un."

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
  ${NSD_CreateLabel} 0 0 100% 56u "Please select a Java Runtime Environment (JRE) if the path below is invalid or unwanted. \
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
    ${NSD_SetText} $JavaPathCheck "This path is INVALID: no 64-bit JRE (${REQJREVERSIONNAME} \
                                   or ${ALTJREVERSIONNAME}) found."
    GetDlgItem $0 $HWNDPARENT 1   ; Next button
    EnableWindow $0 0             ; Disable
    StrCpy $JavaHome ""
    Goto End

  ReqVersionFound:
    ${NSD_SetText} $JavaPathCheck "This path is VALID: 64-bit JRE ${REQJREVERSIONNAME} detected."
    GetDlgItem $0 $HWNDPARENT 1 ; Next button
    EnableWindow $0 1           ; Enable
    StrCpy $JavaHome $JavaHomeTemp
    Goto End

  AltVersionFound:
    ${NSD_SetText} $JavaPathCheck "This path is VALID: 64-bit JRE ${ALTJREVERSIONNAME} detected."
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
!macro GlobalFindDataDir un
Function ${un}FindDataDir

  ClearErrors
  ReadEnvStr $1 GEOSERVER_DATA_DIR
  ${IfNot} ${Errors}
    Push $1
    Call ${un}RemoveTrailingSlash
    Pop $1
  ${EndIf}

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
!macroend

; make FindDataDir function available both for installer and uninstaller
!insertmacro GlobalFindDataDir ""
!insertmacro GlobalFindDataDir "un."

; Taken from https://nsis.sourceforge.io/Check_if_dir_is_empty
Function IsEmptyDir
  # Stack ->                    # Stack: <directory>
  Exch $0                       # Stack: $0
  Push $1                       # Stack: $1, $0
  FindFirst $0 $1 "$0\*.*"
  strcmp $1 "." 0 _notempty
    FindNext $0 $1
    strcmp $1 ".." 0 _notempty
      ClearErrors
      FindNext $0 $1
      IfErrors 0 _notempty
        FindClose $0
        Pop $1                  # Stack: $0
        StrCpy $0 1
        Exch $0                 # Stack: 1 (true)
        goto _end
     _notempty:
       FindClose $0
       ClearErrors
       Pop $1                   # Stack: $0
       StrCpy $0 0
       Exch $0                  # Stack: 0 (false)
  _end:
FunctionEnd

; Data directory config page
Function SetDataDir

  !insertmacro MUI_HEADER_TEXT "$(TEXT_DATADIR_TITLE)" "$(TEXT_DATADIR_SUBTITLE)"

  ${If} $DataDir == ""
    StrCpy $DataDir $DefaultDataDir
  ${EndIf}
  StrCpy $DataDirTemp $DataDir

  nsDialogs::Create 1018

  ; ${NSD_Create*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 56u "If a valid existing ${APPNAME} data directory was found on your system, \
                                   the path should be displayed below. Otherwise, a default directory is suggested. \
                                   $\r$\nYou can also set another directory, as long as it's empty or non-existing."

  ${NSD_CreateDirRequest} 0 90u 240u 13u $DataDirTemp
  Pop $DataDirHWND
  ${NSD_OnChange} $DataDirHWND ValidateDataDir

  ${NSD_CreateBrowseButton} 242u 90u 50u 13u "Browse..."
  Pop $BrowseDataDirHWND
  ${NSD_OnClick} $BrowseDataDirHWND BrowseDataDir

  ${NSD_CreateLabel} 0 106u 100% 12u " "
  Pop $DataDirPathCheck

  ${NSD_SetText} $DataDirHWND $DataDirTemp

  nsDialogs::Show

FunctionEnd

; Checks if the specified data directory is valid.
; Prevents the installer from continuing if it's invalid.
Function ValidateDataDir

    Pop $7
    ${NSD_GetText} $7 $DataDirTemp

    ${If} ${FileExists} "$DataDirTemp\global.xml"       ; 2.0+ data dir
    ${OrIf} ${FileExists} "$DataDirTemp\catalog.xml"    ; 1.7 data dir
      ${NSD_SetText} $DataDirPathCheck "This path contains a valid existing data directory."
      StrCpy $DataDirType 1
      Goto IsValid
    ${EndIf}

    ; Directory is not an existing one: check if the path is an empty dir
    Push "$DataDirTemp\"
    Call IsEmptyDir
    Pop $9

    ${If} $9 == 0
    ${AndIf} ${FileExists} "$DataDirTemp\*.*"
      ; The directory exists but is not empty: we don't accept this (might raise errors later)
      ${NSD_SetText} $DataDirPathCheck "This path does not point to a valid empty data directory."
      GetDlgItem $0 $HWNDPARENT 1     ; Get Next button
      EnableWindow $0 0               ; Disable the button
      Goto End        
    ${EndIf}

    ; The directory is empty or does not exist yet: this is valid (it will be created)
    ${NSD_SetText} $DataDirPathCheck "The data directory will be created at the given path."
    StrCpy $DataDirType 0
    Goto IsValid

    IsValid:
      GetDlgItem $0 $HWNDPARENT 1   ; Get Next button
      EnableWindow $0 1             ; Enable the button
      StrCpy $DataDir $DataDirTemp  ; Set $DataDir variable to valid path
      Goto End
    
    End:
      ClearErrors

FunctionEnd

; Brings up folder dialog for the data directory
Function BrowseDataDir

  nsDialogs::SelectFolderDialog "Please select a valid data directory:" $ProgramData
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

; Find Marlin JAR file and write service wrapper configuration
Function WriteWrapperConfiguration

  ClearErrors
  FileOpen $9 "$INSTDIR\wrapper\jsl64.ini" w  ; Opens a empty file in (over)write mode

  ; Service section
  FileWrite $9 ";GeoServer Java Service Launcher (JSL) configuration$\r$\n$\r$\n"
  FileWrite $9 "[service]$\r$\n"
  FileWrite $9 "appname=${APPNAME}$\r$\n"
  FileWrite $9 "servicename=${APPNAME}$\r$\n"
  FileWrite $9 "displayname=${FULLNAME}$\r$\n"
  FileWrite $9 "servicedescription=${APPNAME} is an open source software server written in Java that allows users to share and edit geospatial data.$\r$\n"
  FileWrite $9 "account=NT Authority\Network Service$\r$\n$\r$\n"  ; (S-1-5-20) can not be used here
  FileWrite $9 ";Console handling$\r$\n"
  FileWrite $9 "useconsolehandler=true$\r$\n"
  FileWrite $9 "stopclass=java/lang/System$\r$\n"
  FileWrite $9 "stopmethod=exit$\r$\n"
  FileWrite $9 "stopsignature=(I)V$\r$\n$\r$\n"
  FileWrite $9 ";Logging$\r$\n"
  FileWrite $9 'logtimestamp="%%Y-%%m-%%d"$\r$\n'
  FileWrite $9 "systemout=%GEOSERVER_HOME%\logs\$AppKey.log$\r$\n"
  FileWrite $9 "systemoutappend=yes$\r$\n"
  FileWrite $9 "systemerr=%GEOSERVER_HOME%\logs\$AppKey.log$\r$\n"
  FileWrite $9 "systemerrappend=yes$\r$\n$\r$\n"
  FileWrite $9 ";Failure handling$\r$\n"
  FileWrite $9 "failureactions_resetperiod=300000$\r$\n"
  FileWrite $9 "failureactions_actions=4$\r$\n"
  FileWrite $9 "action0_type=1$\r$\n"
  FileWrite $9 "action0_delay=10000$\r$\n"
  FileWrite $9 "action1_type=1$\r$\n"
  FileWrite $9 "action1_delay=10000$\r$\n"
  FileWrite $9 "action2_type=1$\r$\n"
  FileWrite $9 "action2_delay=10000$\r$\n"
  FileWrite $9 "action3_type=0$\r$\n"
  FileWrite $9 "action3_delay=10000$\r$\n$\r$\n"

  ; Java section (part 1)
  FileWrite $9 "[java]$\r$\n"
  FileWrite $9 "jrepath=%JAVA_HOME%$\r$\n"
  FileWrite $9 "jvmsearch=path$\r$\n"
  FileWrite $9 "wrkdir=%GEOSERVER_HOME%$\r$\n"

  ; Build command line
  StrCpy $2 "-Djava.awt.headless=true -Djetty.http.port=$StartPort -DSTOP.PORT=$StopPort -DSTOP.KEY=$AppKey"

  ; Find the Marlin renderer JAR
  FindFirst $0 $1 "$INSTDIR\webapps\$AppKey\WEB-INF\lib\marlin*.jar"
  IfErrors End

  ; Add Marlin parameters to the command line
  StrCpy $2 "$2 -Xbootclasspath/a:./webapps/$AppKey/WEB-INF/lib/$1"
  StrCpy $2 "$2 -Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine"
  Goto End

  End:
    ; Finish Java section by writing the command line
    ClearErrors
    FindClose $0
    FileWrite $9 "cmdline=$2 -jar start.jar$\r$\n"
    FileClose $9  ; Closes the file    

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

  ; Directories
  ${NSD_CreateLabel} 10u 25u 35% 24u "Installation directory:"
  ${NSD_CreateLabel} 40% 25u 60% 24u "$INSTDIR"

  ${NSD_CreateLabel} 10u 45u 35% 24u "Data directory:"
  ${NSD_CreateLabel} 40% 45u 60% 24u "$DataDir"

  ; Install type
  ${NSD_CreateLabel} 10u 65u 35% 24u "Installation type:"
  ${If} $IsManual == 1
    ${NSD_CreateLabel} 40% 65u 60% 24u "Run manually"
  ${Else}
    ${NSD_CreateLabel} 40% 65u 60% 24u "Installed as a service"
  ${EndIf}
 
  ; JRE
  ${NSD_CreateLabel} 10u 85u 35% 24u "Java Runtime Environment:"
  ${NSD_CreateLabel} 40% 85u 60% 24u "$JavaHome"

  ; Server Port
  ${NSD_CreateLabel} 10u 105u 35% 24u "Web server port:"
  ${NSD_CreateLabel} 40% 105u 60% 24u "$StartPort"

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

; Copy Jetty INI file line by line, but modify start port
Function CreateNewJettyIni

  FileOpen $0 $INSTDIR\start.default.ini r    ; Open the default ini
  FileOpen $1 $INSTDIR\start.ini w            ; Create the user ini
  
  LOOP:
    IfErrors exit_loop
    FileRead $0 $2
    ${StrLoc} $3 $2 "jetty.http.port=8080" ">"
    ${If} $3 == "0"
      FileWrite $1 "jetty.http.port=$StartPort"
    ${Else}
      FileWrite $1 $2
    ${EndIf}
    Goto LOOP
  
  exit_loop:
    FileClose $0
    FileClose $1

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
  ;CopyFiles /SILENT /FILESONLY "..\source\*.*" "$INSTDIR"

  ; Copy relevant folders from unzipped binary distribution (recursively)
  SetOutPath "$INSTDIR"
  File /r /x "*.sh" "..\source\bin"  ; Copy manual start/stop *.bat files (skip *.sh)
  File /r "..\source\etc"
  File /r "..\source\modules"
  File /r "..\source\lib"
  File /r "..\source\logs"
  File /r "..\source\resources"
  File /r "..\source\webapps"
  File "..\source\*.txt"
  File "..\source\*.md"

  ; Copy Jetty files, manipulate .ini
  File "..\source\start.jar"
  File "/oname=start.default.ini" "..\source\start.ini"
  Call CreateNewJettyIni

  ; Copy icon files
  CreateDirectory "$INSTDIR\ico"
  SetOutPath "$INSTDIR\ico"
  File "/oname=gs.ico" "img\installer.ico"
  File "img\start.ico"
  File "img\stop.ico"
  File "img\info.ico"

  ; Create Java IO temp dir
  CreateDirectory "$INSTDIR\work"

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
    File /r "..\source\data_dir\*.*"
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

  ${If} $IsManual == 0 ; install as service

    DetailPrint "Setting up Windows service..."

    ; Create a directory for the wrapper
    CreateDirectory "$INSTDIR\wrapper"
    SetOutPath "$INSTDIR\wrapper"
    File "wrapper\LICENSE.txt"
    File "wrapper\README.md"
    
    ; Copy JSL, rename and sign with SHA1, add to output dir
    !system 'copy /y /b wrapper\jsl64.exe wrapper\${APPNAME}.exe'
    !system '${SIGNCOMMAND} wrapper\${APPNAME}.exe'
    File "wrapper\${APPNAME}.exe"

    ; Generate JSL configuration ini file
    Call WriteWrapperConfiguration

    ; Install the service using Java Service Launcher and start it
    ; jsl64.exe -install|configure|remove|run|runapp (<ini file>) (-console hide|attach|new)
    DetailPrint "Installing Windows service..."
    nsExec::ExecToLog '"$INSTDIR\wrapper\${APPNAME}.exe" -install "$INSTDIR\wrapper\jsl64.ini" -console hide'
    Sleep 4000    ; make sure install has finished
    
  ${EndIf}

  ; Grant full access to directories that require them:
  ; - Logging directory
  ; - Data directory
  ; - Working directory
  ; This requires the NSIS AccessControl plugin at https://nsis.sourceforge.io/AccessControl_plug-in
  ; For more info, see https://nsis.sourceforge.io/How_can_I_install_a_plugin#NSIS_plugin_installation
  DetailPrint "Granting access..."
  ${If} $IsManual == 1      ; manual run -> grant to Users group
    AccessControl::GrantOnFile "$INSTDIR\work" "(S-1-5-32-545)" "FullAccess"
    AccessControl::GrantOnFile "$INSTDIR\logs" "(S-1-5-32-545)" "FullAccess"
    AccessControl::GrantOnFile "$DataDir" "(S-1-5-32-545)" "FullAccess"
  ${ElseIf} $IsManual == 0  ; run as service -> grant to NT AUTHORITY\Network Service
    AccessControl::GrantOnFile "$INSTDIR\work" "(S-1-5-20)" "FullAccess"
    AccessControl::GrantOnFile "$INSTDIR\logs" "(S-1-5-20)" "FullAccess"
    AccessControl::GrantOnFile "$DataDir" "(S-1-5-20)" "FullAccess"
  ${EndIf}

SectionEnd

!ifndef INNER

  ; What happens at the end of the install
  Section -FinishSection

    ${If} $IsManual == 0  ; service

      DetailPrint "Starting Windows service..."

      ; Start the installed service using Windows "net" command
      nsExec::ExecToLog 'net start ${APPNAME}'
      Sleep 2000

      DetailPrint "Writing batch files..."

      ; Add simple start/stop service batch files
      SetOutPath "$INSTDIR\bin"

      FileOpen $9 startService.bat w  ; Creates a new bat file and opens it
      FileWrite $9 'net start ${APPNAME}$\r$\n'
      FileClose $9                    ; Closes the file

      FileOpen $9 stopService.bat w   ; Creates a new bat file and opens it
      FileWrite $9 'net stop ${APPNAME}$\r$\n'
      FileClose $9                    ; Closes the file

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
      SetOutPath "$INSTDIR\bin"
      ${If} $IsManual == 0
        ; Link to service batch files
        CreateShortCut "$INSTDIR\bin\Start ${APPNAME}.lnk" "$INSTDIR\bin\startService.bat" "" "$INSTDIR\ico\start.ico" 0
        CreateShortCut "$INSTDIR\bin\Stop ${APPNAME}.lnk" "$INSTDIR\bin\stopService.bat" "" "$INSTDIR\ico\stop.ico" 0
      ${Else}
        ; Link to manual batch files
        CreateShortCut "$INSTDIR\bin\Start ${APPNAME}.lnk" "$INSTDIR\bin\startup.bat" "" "$INSTDIR\ico\start.ico" 0
        CreateShortCut "$INSTDIR\bin\Stop ${APPNAME}.lnk" "$INSTDIR\bin\shutdown.bat" "" "$INSTDIR\ico\stop.ico" 0      
      ${EndIf}
      Rename "$INSTDIR\bin\Start ${APPNAME}.lnk" "$SMPROGRAMS\$MenuFolder\Start ${APPNAME}.lnk"
      Rename "$INSTDIR\bin\Stop ${APPNAME}.lnk" "$SMPROGRAMS\$MenuFolder\Stop ${APPNAME}.lnk"
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
    File "$%TEMP%\${UNINNAME}"

  SectionEnd

!else

  ; Uninstall section
  Section Uninstall

    Call un.FindDataDir

    ; Stop GeoServer
    DetailPrint "Stopping ${FULLNAME}..."

    IfFileExists "$INSTDIR\wrapper\*.*" 0 ManualStop
      nsExec::ExecToLog '"$INSTDIR\bin\stopService.bat"'
      Sleep 4000    ; make sure it's fully stopped
      
      DetailPrint "Removing ${FULLNAME} service..."
      nsExec::ExecToLog '"$INSTDIR\wrapper\${APPNAME}.exe" -remove "$INSTDIR\wrapper\jsl64.ini" -console hide'
      Sleep 4000                  ; make sure it's fully removed
      
      RMDir /r "$INSTDIR\wrapper" ; remove the wrapper files

    ManualStop:
      nsExec::ExecToLog '"$INSTDIR\bin\shutdown.bat"'
      Sleep 4000    ; make sure it's fully stopped    

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
    RMDir /r "$INSTDIR\bin"
    RMDir /r "$INSTDIR\etc"
    RMDir /r "$INSTDIR\modules"
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\logs"
    RMDir /r "$INSTDIR\resources"
    RMDir /r "$INSTDIR\ico"
    RMDir /r "$INSTDIR\work"        ; working data
    RMDir /r "$INSTDIR\webapps"
    RMDir /r "$INSTDIR\v*"          ; EPSG DB
    Delete "$INSTDIR\*.*"           ; delete root files

    RMDir "$INSTDIR" ; no /r!

    IfFileExists "$INSTDIR\*.*" 0 +3
      DetailPrint "WARNING: could not completely remove $INSTDIR"
      MessageBox MB_OK|MB_ICONEXCLAMATION "Some files or folders could not be removed from:$\r$\n$INSTDIR"

    IfFileExists "$DataDir\*.*" 0 NoDataDir
      ; Ask to also remove the data directory. For silent uninstalls (/SD IDNO), we will KEEP the data directory.
      MessageBox MB_YESNO|MB_DEFBUTTON2 "Would you like to remove all ${APPNAME} data in $DataDir?$\r$\n\
                                         If you ever plan to reinstall ${APPNAME}, this is not recommended." /SD IDNO IDYES RemoveData IDNO KeepData

      KeepData:
        DetailPrint "Keeping ${APPNAME} data directory at $DataDir"
        Goto Finish

      RemoveData:
        DetailPrint "Removing ${APPNAME} data directory..."
        RMDir /r "$DataDir\*.*"
        RMDir "$DataDir"
        IfFileExists "$DataDir\*.*" 0 RemoveEnvVar
          DetailPrint "WARNING: could not completely remove $DataDir"
          MessageBox MB_OK|MB_ICONEXCLAMATION "WARNING - Some files or folders could not be removed from:$\r$\n$DataDir"
          Goto Finish

      RemoveEnvVar:
        ; Delete environment var if data dir was successfully removed
        DetailPrint "Removed ${APPNAME} data directory $DataDir"
        Push GEOSERVER_DATA_DIR
        Call un.DeleteEnvVar
        DetailPrint "Removed GEOSERVER_DATA_DIR system environment variable"
        Goto Finish

    NoDataDir:
      DetailPrint "WARNING: no data directory found"

    Finish:
      ; "Completed" is printed automatically

  SectionEnd

!endif
