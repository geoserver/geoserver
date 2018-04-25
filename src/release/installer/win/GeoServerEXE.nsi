/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

; GeoServer Windows installer creation file.

; Define your application name
!define APPNAME "GeoServer"
!define VERSION "2.14-SNAPSHOT"
;!define LONGVERSION "2.0.0.0"
!define APPNAMEANDVERSION "${APPNAME} ${VERSION}"

; Main Install settings
Name "${APPNAMEANDVERSION}"
InstallDir "$PROGRAMFILES\${APPNAMEANDVERSION}"
InstallDirRegKey HKLM "Software\${APPNAME}" ""
OutFile "geoserver-${VERSION}.exe"

;Compression options
CRCCheck on

; For Vista
RequestExecutionLevel admin

; Modern interface settings
!include "MUI.nsh" ; Modern interface
!include "StrFunc.nsh" ; String functions
!include "LogicLib.nsh" ; ${If} ${Case} etc.
!include "nsDialogs.nsh" ; For Custom page layouts (Radio buttons etc)

; Might be the same as !define
Var STARTMENU_FOLDER
Var JavaHome
Var JavaHomeTemp
Var JavaHomeHWND
Var BrowseJavaHWND
Var JavaPathCheck
Var LinkHWND
Var DataDir
Var DataDirTemp
Var DataDirHWND
Var BrowseDataDirHWND
Var DataDirPathCheck
Var DataDirType
Var DefaultDataDir
Var ExistingDataDir
Var IsExistingDataDir
Var GSUser
Var GSPass
Var GSUserHWND
Var GSPassHWND
Var IsManual
Var Manual
Var Service
Var Port
Var PortHWND

;Version Information (Version tab for EXE properties)
;VIProductVersion ${LONGVERSION}
;VIAddVersionKey ProductName "${APPNAME}"
;VIAddVersionKey LegalCopyright "Copyright (c) 1999-2016 Open Source Geospatial Foundation"
;VIAddVersionKey FileDescription "GeoServer Installer"
;VIAddVersionKey ProductVersion "${VERSION}"
;VIAddVersionKey FileVersion "${VERSION}"
;VIAddVersionKey Comments "http://geoserver.org"

;Interface Settings
!define MUI_ICON "gs.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\win-uninstall.ico"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP header.bmp
!define MUI_WELCOMEFINISHPAGE_BITMAP side_left.bmp

;Start Menu Folder Page Configuration
!define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKLM" 
!define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${APPNAME}" 
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"

; "Are you sure you wish to cancel" popup.
!define MUI_ABORTWARNING

; Optional welcome text here
  !define MUI_WELCOMEPAGE_TEXT "This wizard will guide you through the installation of ${APPNAMEANDVERSION}. \r\n\r\n\
	It is recommended that you close all other applications before starting Setup.\
	This will make it possible to update relevant system files without having to reboot your computer.\r\n\r\n\
	Please report any problems or suggestions to the GeoServer Users mailing list: geoserver-users@lists.sourceforge.net. \r\n\r\n\
	Click Next to continue."

; Install Page order
; This is the main list of installer things to do 
!insertmacro MUI_PAGE_WELCOME                                 ; Hello
Page custom CheckUserType                                     ; Die if not admin
!insertmacro MUI_PAGE_LICENSE "LICENSE.txt"                   ; Show license
!insertmacro MUI_PAGE_DIRECTORY                               ; Where to install
!insertmacro MUI_PAGE_STARTMENU Application $STARTMENU_FOLDER ; Start menu location
Page custom GetJRE                                            ; Look for exisitng JRE
Page custom JRE JRELeave                                      ; Set the JRE
Page custom GetDataDir                                        ; Look for existing data_dir
Page custom DataDir DataDirLeave                              ; Set the data directory
Page custom Creds                                             ; Set admin/password (if new data_dir)
Page custom Port                                              ; Set Jetty web server port
Page custom InstallType InstallTypeLeave                      ; Manual/Service
Page custom Ready                                             ; Summary page
!insertmacro MUI_PAGE_INSTFILES                               ; Actually do the install
!insertmacro MUI_PAGE_FINISH                                  ; Done

; Uninstall Page order
!insertmacro MUI_UNPAGE_CONFIRM   ; Are you sure you wish to uninstall?
!insertmacro MUI_UNPAGE_INSTFILES ; Do the uninstall

; Set languages (first is default language)
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_RESERVEFILE_LANGDLL

; Install options page headers
LangString TEXT_JRE_TITLE ${LANG_ENGLISH} "Java Runtime Environment"
LangString TEXT_JRE_SUBTITLE ${LANG_ENGLISH} "Java Runtime Environment path selection"
LangString TEXT_DATADIR_TITLE ${LANG_ENGLISH} "GeoServer Data Directory"
LangString TEXT_DATADIR_SUBTITLE ${LANG_ENGLISH} "GeoServer Data Directory path selection"
LangString TEXT_TYPE_TITLE ${LANG_ENGLISH} "Type of Installation"
LangString TEXT_TYPE_SUBTITLE ${LANG_ENGLISH} "Select the type of installation"
LangString TEXT_READY_TITLE ${LANG_ENGLISH} "Ready to Install"
LangString TEXT_READY_SUBTITLE ${LANG_ENGLISH} "GeoServer is ready to be installed"
LangString TEXT_CREDS_TITLE ${LANG_ENGLISH} "GeoServer Administrator"
LangString TEXT_CREDS_SUBTITLE ${LANG_ENGLISH} "Set administrator credentials"
LangString TEXT_PORT_TITLE ${LANG_ENGLISH} "GeoServer Web Server Port"
LangString TEXT_PORT_SUBTITLE ${LANG_ENGLISH} "Set the port that GeoServer will respond on"

; Startup tasks
Function .onInit
	
; Splash screen
  SetOutPath $TEMP
  File /oname=spltmp.bmp "splash.bmp"
  advsplash::show 1500 500 0 -1 $TEMP\spltmp
	;advsplash::show Delay FadeIn FadeOut KeyColor FileName
  Pop $0 ; $0 has '1' if the user closed the splash screen early,
         ;    has '0' if everything closed normally, and '-1' if some error occurred.
  Delete $TEMP\spltmp.bmp
	
  StrCpy $IsManual 1  ; Set to run manually by default
		
FunctionEnd

; Check the user type, and quit if it's not an administrator.
; Taken from Examples/UserInfo that ships with NSIS.
Function CheckUserType
  ClearErrors
  UserInfo::GetName
  IfErrors Win9x
  Pop $0
  UserInfo::GetAccountType
  Pop $1
  StrCmp $1 "Admin" Admin NoAdmin

  NoAdmin:
    MessageBox MB_ICONSTOP "Sorry, you must have administrative rights in order to install GeoServer."
    Quit

  Win9x:
    MessageBox MB_ICONSTOP "This installer is not supported on Windows 9x/ME."
    Quit
		
  Admin:
  StrCpy $1 "" ; zero out variable
	
FunctionEnd

; Find the %JAVA_HOME% used on the system, and put the result on the top of the stack
; Will check environment variables and the registry
; Will return an empty string if the path cannot be determined
Function FindJavaPath

  ClearErrors

  ReadEnvStr $1 JAVA_HOME
  IfErrors 0 End ; found in the environment variable

  ; No env var set
  ClearErrors
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "JavaHome"

  IfErrors 0 End ; or found in the registry  

  StrCpy $1 "" ; not found, zeroing out
  Goto End

  End:
  ; Put the result in the stack
  Push $1
  StrCpy $2 "" ; zero out

FunctionEnd

; Runs before the page is loaded to ensure that the better value (if any) is always reset.
Function GetJRE

    ${If} $JavaHome == ""
      Call FindJavaPath
      Pop $JavaHome
    ${EndIf}

FunctionEnd

; JRE page display
Function JRE

  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"

  StrCpy $JavaHomeTemp $JavaHome 

  Call JREPathValidInit
  Pop $8
  ;MessageBox MB_OK "$8"

  nsDialogs::Create 1018
  ; ${NSD_Create*} x y width height text

  ${NSD_CreateLabel} 0 0 100% 36u "Please select the path to your Java Runtime Environment (JRE).\
                                   $\r$\n$\r$\nIf you don't have a JRE installed, you can use the \
                                   link below to go to Oracle's website to download and install the \
                                   correct JRE for your system."

  ${NSD_CreateLink} 12u 40u 100% 12u "http://www.oracle.com/technetwork/java/javase/downloads/index.html"
  Pop $LinkHWND
  ${NSD_OnClick} $LinkHWND Link

  ${NSD_CreateDirRequest} 0 70u 240u 13u $JavaHomeTemp
  Pop $JavaHomeHWND
  ${NSD_OnChange} $JavaHomeHWND JREPathValid
  Pop $9

  ${NSD_CreateBrowseButton} 242u 70u 50u 13u "Browse..."
  Pop $BrowseJavaHWND
  ${NSD_OnClick} $BrowseJavaHWND BrowseJava

  ${NSD_CreateLabel} 0 86u 100% 12u " "
  Pop $JavaPathCheck

  ${If} $8 == "validJRE"
    ${NSD_SetText} $JavaPathCheck "This path contains a valid JRE"
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 1 ; Turns on
  ${EndIf}
  ${If} $8 == "novalidJRE"
    ${NSD_SetText} $JavaPathCheck "This path does not contain a valid JRE"
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 0 ; Turns off
  ${EndIf}
   
  nsDialogs::Show

FunctionEnd

; When the link is clicked...
Function Link

  Pop $0
  ExecShell "open" "http://www.oracle.com/technetwork/java/javase/downloads/index.html"

FunctionEnd

; Runs when page is initialized
Function JREPathValidInit

    IfFileExists "$JavaHome\bin\java.exe" NoErrors Errors

    NoErrors:
    StrCpy $8 "validJRE"
    Goto End

    Errors:
    StrCpy $8 "novalidJRE"
    
    End:
    Push $8

FunctionEnd

; Runs in real time
Function JREPathValid

  Pop $8
  ${NSD_GetText} $8 $JavaHomeTemp

  IfFileExists "$JavaHomeTemp\bin\java.exe" NoErrors Errors

  NoErrors:
    ${NSD_SetText} $JavaPathCheck "This path contains a valid JRE"
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 1 ; Enable
  Goto End

  Errors:
    ${NSD_SetText} $JavaPathCheck "This path does not contain a valid JRE"
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 0 ; Disable

  End:
    StrCpy $8 ""
    ClearErrors

FunctionEnd

; Brings up folder dialog
Function BrowseJava

  nsDialogs::SelectFolderDialog "Please select the location of your JRE..." $PROGRAMFILES
  Pop $1
  ${NSD_SetText} $JavaHomeHWND $1
    
FunctionEnd

; When done, set variable permanently
Function JRELeave

  StrCpy $JavaHome $JavaHomeTemp

FunctionEnd

; Find the %GEOSERVER_DATA_DIR% used on the system, and put the result on the top of the stack
Function FindDataDirPath

  ClearErrors
  ReadEnvStr $1 GEOSERVER_DATA_DIR
  IfFileExists $1 NoErrors Errors

  NoErrors:
    ClearErrors
    StrCpy $DataDirType 1
    Goto End

  Errors:
    ClearErrors
    StrCpy $1 "" ; not found
    StrCpy $DataDirType 0
    Goto End

  End:
    ClearErrors
    Push $1

FunctionEnd

; Find Marlin JAR file and write service wrapper configuration to enable the Marlin renderer
Function SetServiceMarlinRenderer

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

; Runs before the page is loaded to ensure that the better value (if any) is always reset
Function GetDataDir

  ${If} $DataDir == ""
    Call FindDataDirPath
    Pop $DataDir
  ${EndIf}

FunctionEnd

; Data_dir page display
Function DataDir
	
  !insertmacro MUI_HEADER_TEXT "$(TEXT_DATADIR_TITLE)" "$(TEXT_DATADIR_SUBTITLE)"

  StrCpy $DataDirTemp $DataDir

  Call DataDirPathValidInit
  Pop $8

  nsDialogs::Create 1018
  ; ${NSD_Create*} x y width height text

  ${NSD_CreateLabel} 0 0 100% 24u "If you have an existing data directory, please select its path.  \
                                   Otherwise, the default data directory will be used."

  ${NSD_CreateRadioButton} 10u 36u 10u 10u
  Pop $DefaultDataDir

  ${NSD_CreateLabel} 25u 37u 250u 24u "Default data directory. Will be located at: \
                                       $\r$\n$INSTDIR\data_dir"

  ${NSD_CreateRadioButton} 10u 80u 10u 10u
  Pop $ExistingDataDir

  ${NSD_CreateLabel} 25u 81u 250u 12u "Existing data directory:"

  ${NSD_CreateDirRequest} 25u 94u 215u 13u $DataDirTemp
  Pop $DataDirHWND
  ${NSD_OnChange} $DataDirHWND DataDirPathValid
  Pop $9

  ${NSD_CreateBrowseButton} 242u 94u 50u 13u "Browse..."
  Pop $BrowseDataDirHWND
  ${NSD_OnClick} $BrowseDataDirHWND BrowseDataDir

  ${NSD_CreateLabel} 26u 108u 100% 12u " "
  Pop $DataDirPathCheck

  ${If} $8 == "validDataDir"
    ${NSD_SetText} $DataDirPathCheck "This path contains a valid data directory"
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 1 ; Turns on
  ${EndIf}
  ${If} $8 == "novalidDataDir"
    ${If} $DataDirType == 1  ; Dont' turn off unless the radio box is checked!
      ${NSD_SetText} $DataDirPathCheck "This path does not contain a valid data directory"
      GetDlgItem $0 $HWNDPARENT 1 ; Next
      EnableWindow $0 0 ; Turns off
    ${EndIf}
  ${EndIf}

  ; default check box
  ${If} $DataDirType == 1
    ${NSD_Check} $ExistingDataDir
  ${Else}
    ${NSD_Check} $DefaultDataDir
  ${EndIf}

  ${NSD_OnClick} $ExistingDataDir CheckBoxDataDirExisting
  ${NSD_OnClick} $DefaultDataDir CheckBoxDataDirDefault
   
  nsDialogs::Show

	
FunctionEnd

; Runs when page is initialized
Function DataDirPathValidInit

    IfFileExists "$DataDir\global.xml" NoErrors 0 ; 2.0 datadir
    IfFileExists "$DataDir\catalog.xml" NoErrors Errors ; 1.7 datadir

    NoErrors:
    StrCpy $8 "validDataDir"
    Goto End

    Errors:
    StrCpy $8 "novalidDataDir"
    
    End:
    Push $8

FunctionEnd

; Runs in real time
Function DataDirPathValid

    Pop $8
    ${NSD_GetText} $8 $DataDirTemp

    IfFileExists "$DataDirTemp\global.xml" NoErrors 0 ; 2.0 datadir
    IfFileExists "$DataDirTemp\catalog.xml" NoErrors Errors ; 1.7 datadir

    NoErrors:
      ${NSD_SetText} $DataDirPathCheck "This path contains a valid data directory"
      GetDlgItem $0 $HWNDPARENT 1 ; Next
      EnableWindow $0 1 ; Enable
      Goto End

    Errors:
      ${NSD_SetText} $DataDirPathCheck "This path does not contain a valid data directory"
      GetDlgItem $0 $HWNDPARENT 1 ; Next
      EnableWindow $0 0 ; Disable

    End:
      StrCpy $8 ""
      ClearErrors

FunctionEnd

; When Existing check box is checked
Function CheckBoxDataDirExisting

  ${NSD_GetText} $DataDirHWND $DataDirTemp
  IfFileExists "$DataDirTemp\*.xml" NoErrors Errors

  NoErrors:
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 1 ; Enable
    Goto End

  Errors:
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 0 ; Disable

  End:
    ClearErrors
    StrCpy $DataDirType 1

FunctionEnd

; When Default check box is checked
Function CheckBoxDataDirDefault

  GetDlgItem $0 $HWNDPARENT 1 ; Next
  EnableWindow $0 1 ; Turns on
  StrCpy $DataDirType 0

FunctionEnd

; Brings up folder dialog
Function BrowseDataDir

  nsDialogs::SelectFolderDialog "Please select the location of your data directory..." $PROGRAMFILES
  Pop $1

  ${If} $1 != "error" ; i.e. didn't hit cancel
    ${NSD_SetText} $DataDirHWND $1 ; populate the field
    ${NSD_Check} $ExistingDataDir ; change the check box
    ${NSD_UnCheck} $DefaultDataDir ; change the check box
    StrCpy $DataDirType 1 ; now using existing datadir
  ${EndIf}  

FunctionEnd

; When done, set variable permanently
Function DataDirLeave

  ${If} $DataDirType == 0 ; use the default
    StrCpy $DataDir "$INSTDIR\data_dir"
  ${ElseIf} $DataDirType == 1
    StrCpy $DataDir $DataDirTemp
  ${EndIf}

  ; check to see wether the data_dir exist
  IfFileExists $DataDir DataDirExists

  ; does NOT exists
  StrCpy $IsExistingDataDir 0
  Goto End

  DataDirExists:
  StrCpy $IsExistingDataDir 1

  End:

FunctionEnd

; Will build a page to input default GS admin creds
Function Creds

  StrCmp $IsExistingDataDir 1 SkipCreds
  
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
  ${NSD_CreateLabel} 0 0 100% 36u "Set the username and password for administration of GeoServer."

  ${NSD_CreateLabel} 20u 40u 40u 14u "Username"  
  ${NSD_CreateText} 70u 38u 50u 14u $GSUser
  Pop $GSUserHWND
  ${NSD_OnChange} $GSUserHWND UsernameCheck

  ${NSD_CreateLabel} 20u 60u 40u 14u "Password" 
  ${NSD_CreateText} 70u 58u 50u 14u $GSPass
  Pop $GSPassHWND
  ${NSD_OnChange} $GSPassHWND PasswordCheck

  nsDialogs::Show

  SkipCreds: ; if data dir exists, we wouldn't want to change creds
   
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

; Set the web server port
Function Port

  !insertmacro MUI_HEADER_TEXT "$(TEXT_PORT_TITLE)" "$(TEXT_PORT_SUBTITLE)"
  nsDialogs::Create 1018

  ; Populates defaults on first display, and resets to default user blanked any of the values
  StrCmp $Port "" 0 +2
    StrCpy $Port "8080"

  ;Syntax: ${NSD_*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 36u "Set the web server port that GeoServer will respond on."

  ${NSD_CreateLabel} 20u 40u 20u 14u "Port"  
  ${NSD_CreateNumber} 50u 38u 50u 14u $Port
  Pop $PortHWND
  ${NSD_OnChange} $PortHWND PortCheck

  ${NSD_CreateLabel} 110u 40u 120u 14u "Valid range is 80, 1024-65535." 

  nsDialogs::Show

FunctionEnd

; When port value is changed (realtime)
Function PortCheck

  ; Check for illegal values of $Port and fix immediately

  ${NSD_GetText} $PortHWND $Port


  ; Check for illegal values of $Port
  ${If} $Port = 80
    GetDlgItem $0 $HWNDPARENT 1 ; Next
    EnableWindow $0 1 ; Enable
  ${Else}  
     ${If} $Port < 1024        ; Too low
     ${OrIf} $Port > 65535     ; Too high
      GetDlgItem $0 $HWNDPARENT 1 ; Next
      EnableWindow $0 0 ; Disable
     ${Else}
      GetDlgItem $0 $HWNDPARENT 1 ; Next
      EnableWindow $0 1 ; Enable
     ${EndIf}
   ${EndIf}
   
FunctionEnd

; Manual vs service selection
Function InstallType

  nsDialogs::Create 1018
  !insertmacro MUI_HEADER_TEXT "$(TEXT_TYPE_TITLE)" "$(TEXT_TYPE_SUBTITLE)"

  ;Syntax: ${NSD_*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 24u 'Select the type of installation for GeoServer.  If you are unsure of which option to choose, select the "Run manually" option.'
  ${NSD_CreateRadioButton} 10u 28u 50% 12u "Run manually"
  Pop $Manual

  ${NSD_CreateLabel} 10u 44u 90% 24u "Installed for the current user.  Must be manually started and stopped."
  ${NSD_CreateRadioButton} 10u 72u 50% 12u "Install as a service"
  Pop $Service

  ${If} $IsManual == 1
    ${NSD_Check} $Manual ; Default
  ${Else}
    ${NSD_Check} $Service
  ${EndIf}

  ${NSD_CreateLabel} 10u 88u 90% 24u "Installed for all users.  Will run as as a Windows Service for greater security.  Requires a 32 bit JRE."

  nsDialogs::Show

FunctionEnd

; Records the final state of manual vs service
Function InstallTypeLeave

  ${NSD_GetState} $Manual $IsManual
  ; $IsManual = 1 -> Run manually
  ; $IsManual = 0 -> Run as service

FunctionEnd

; Summary page before install
Function Ready

  nsDialogs::Create 1018
  !insertmacro MUI_HEADER_TEXT "$(TEXT_READY_TITLE)" "$(TEXT_READY_SUBTITLE)"

  ;Syntax: ${NSD_*} x y width height text
  ${NSD_CreateLabel} 0 0 100% 24u "Please review the settings below and click the Back button if \
                                   changes need to be made.  Click the Install button to continue."

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


  ; Data dir
  ${NSD_CreateLabel} 10u 85u 35% 24u "Data Directory:"
  ${If} $IsExistingDataDir == 1
    ${NSD_CreateLabel} 40% 85u 60% 24u "Using existing data directory:$\r$\n$DataDir"
  ${Else}
    ${NSD_CreateLabel} 40% 85u 60% 24u "Using default data directory:$\r$\n$DataDir"
  ${EndIf}

  ; Creds
  ${If} $IsExistingDataDir == 1
    ${NSD_CreateLabel} 10u 112u 35% 24u "Port:"
    ${NSD_CreateLabel} 40% 112u 60% 24u "$Port"
  ${Else}
    ${NSD_CreateLabel} 10u 112u 35% 24u "Username / Password / Port:"
    ${NSD_CreateLabel} 40% 112u 60% 24u "$GSUser / $GSPass / $Port"
  ${EndIf}

  nsDialogs::Show

FunctionEnd

; Write an environment variable
Function WriteEnvVar

  Pop $4
  Pop $3

  ;This writes a SYSTEM variable, not a User variable
  ;WriteRegExpandStr HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" variable value
  
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


; The main install section
Section "Main" SectionMain
	
  SectionIn RO ; Makes this install mandatory
  SetOverwrite on

  ; Section Files and Shortcuts
  CreateDirectory "$INSTDIR"
  SetOutPath "$INSTDIR"
  File /a start.jar
  File /a start.ini
  File /a GPL.txt
  File /a LICENSE.txt
  File /a README.txt
  File /a RUNNING.txt
  File /r etc
  File /r modules
  File /r lib
  File /r logs
  File /r resources
  File /r webapps
  File /r gs.ico

  ; Special handling of the 'data_dir'
  ${If} $IsExistingDataDir == 0 ; Only place files, when NOT using an existing folder
  File /r data_dir
  ${EndIf}


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

  ${If} $IsManual == 0 ; service

    SetOutPath "$INSTDIR"
    File /a wrapper.exe
    File /a wrapper-server-license.txt

    CreateDirectory "$INSTDIR\wrapper"
    SetOutPath "$INSTDIR\wrapper"
    File /a wrapper.conf

    CreateDirectory "$INSTDIR\wrapper\lib"
    SetOutPath "$INSTDIR\wrapper\lib"
    File /a wrapper.jar
    File /a wrapper.dll

    CreateDirectory "$INSTDIR\work"

    Call SetServiceMarlinRenderer
    
    ; Install the service (and start it)
    nsExec::Exec "$INSTDIR\wrapper.exe -it ./wrapper/wrapper.conf wrapper.app.parameter.4=jetty.port=$Port"

  ${EndIf}

  ; Security (of sorts)
  ${If} $IsManual == 1 ; manual
    AccessControl::GrantOnFile "$INSTDIR\logs" "(S-1-5-32-545)" "FullAccess"
    AccessControl::GrantOnFile "$INSTDIR\data_dir" "(S-1-5-32-545)" "FullAccess"
  ${ElseIf} $IsManual == 0 ; service
    AccessControl::GrantOnFile "$INSTDIR\logs" "NT AUTHORITY\Network Service" "FullAccess"
    AccessControl::GrantOnFile "$DataDir" "NT AUTHORITY\Network Service" "FullAccess"
  ${EndIf}

SectionEnd

; What happens at the end of the install.
Section -FinishSection

  ; New users.properties file is created here
  StrCmp $IsExistingDataDir 1 NoWriteCreds WriteCreds

  WriteCreds:
    Delete "$DataDir\security\users.properties"
    FileOpen $R9 "$DataDir\security\users.properties" w
    FileWrite $R9 "$GSUser=$GSPass,ROLE_ADMINISTRATOR"
    FileClose $R9
	
  NoWriteCreds:

  ; Startup and Shutdown batch files
  CreateDirectory "$INSTDIR\bin"
  SetOutPath "$INSTDIR\bin"

  ${If} $IsManual == 0  ; service

    FileOpen $9 startup.bat w ; Opens a Empty File and fills it
    FileWrite $9 'call "$INSTDIR\wrapper.exe" -t wrapper/wrapper.conf' 
    FileClose $9 ; Closes the file

    FileOpen $9 shutdown.bat w ; Opens a Empty File and fills it
    FileWrite $9 'call "$INSTDIR\wrapper.exe" -p wrapper/wrapper.conf' 
    FileClose $9 ; Closes the file

  ${ElseIf} $IsManual == 1 ; manual

    FileOpen $9 startup.bat w ; Opens a Empty File and fills it
    FileWrite $9 'for /f "delims=" %%i in ($\'dir /b/s "%~dp0..\webapps\geoserver\WEB-INF\lib\marlin*.jar"$\') do set MARLIN_JAR=%%i$\r$\n'
    FileWrite $9 'if not "%MARLIN_JAR%" == "" set MARLIN_ENABLER=-Xbootclasspath/a:"%MARLIN_JAR%" -Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine$\r$\n$\r$\n'
    FileWrite $9 'call "$JavaHome\bin\java.exe" %MARLIN_ENABLER% -DGEOSERVER_DATA_DIR="$DataDir" -Xmx512m -DSTOP.PORT=8079 -DSTOP.KEY=geoserver -Djetty.base="$INSTDIR" -Djetty.logs="$INSTDIR\logs" -jar "$INSTDIR\start.jar" --module=http jetty.port=$Port'
    FileClose $9 ; Closes the file

    FileOpen $9 shutdown.bat w ; Opens a Empty File and fills it
    FileWrite $9 'call "$JavaHome\bin\java.exe" -DSTOP.PORT=8079 -DSTOP.KEY=geoserver -jar "$INSTDIR\start.jar" --stop'
    FileClose $9 ; Closes the file

  ${EndIf}
  
  ;Start Menu
  !insertmacro MUI_STARTMENU_WRITE_BEGIN Application

  ;Create shortcuts
  CreateDirectory "$SMPROGRAMS\$STARTMENU_FOLDER"
  CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\GeoServer Homepage.lnk" "http://geoserver.org"
  CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\GeoServer Web Admin Page.lnk" "http://localhost:$Port/geoserver/web"
  CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\GeoServer Data Directory.lnk" "$DataDir"
  CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\Uninstall.lnk" "$INSTDIR\Uninstall.exe"

  SetOutPath "$INSTDIR"
  CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\Start GeoServer.lnk" "$INSTDIR\bin\startup.bat" \
                 "" "$INSTDIR\gs.ico" 0
  CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\Stop GeoServer.lnk" "$INSTDIR\bin\shutdown.bat" \
                 "" "$INSTDIR\gs.ico" 0

  !insertmacro MUI_STARTMENU_WRITE_END

  ; Registry
  WriteRegStr HKLM "Software\${APPNAME}" "" "$INSTDIR"

  ; For the Add/Remove programs area
  !define UNINSTALLREGPATH "Software\Microsoft\Windows\CurrentVersion\Uninstall"
  WriteRegStr HKLM "${UNINSTALLREGPATH}\${APPNAMEANDVERSION}" "DisplayName" "${APPNAMEANDVERSION}"
  WriteRegStr HKLM "${UNINSTALLREGPATH}\${APPNAMEANDVERSION}" "UninstallString" "$INSTDIR\uninstall.exe"
  WriteRegStr HKLM "${UNINSTALLREGPATH}\${APPNAMEANDVERSION}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "${UNINSTALLREGPATH}\${APPNAMEANDVERSION}" "DisplayIcon" "$INSTDIR\gs.ico"
  WriteRegStr HKLM "${UNINSTALLREGPATH}\${APPNAMEANDVERSION}" "HelpLink" "http://geoserver.org"
  WriteRegDWORD HKLM "${UNINSTALLREGPATH}\${APPNAMEANDVERSION}" "NoModify" "1"
  WriteRegDWORD HKLM "${UNINSTALLREGPATH}\${APPNAMEANDVERSION}" "NoRepair" "1"

  WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

;Uninstall section
Section Uninstall

  ; Stop
  IfFileExists "$INSTDIR\wrapper.exe" StopService StopManual
  StopService:
    ExecWait "$INSTDIR\wrapper.exe -r wrapper/wrapper.conf"
    Sleep 4000 ; to make sure it's fully stopped
    RMDir /r "$INSTDIR\wrapper" ; while we're here
    Goto Continue
  StopManual:
    ExecWait "$INSTDIR\bin\shutdown.bat"
    Sleep 4000 ; to make sure it's fully stopped
  Continue:


  ; Remove env var
  Push GEOSERVER_HOME
  Call un.DeleteEnvVar

  SetOutPath $TEMP

  ;Remove from registry...
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAMEANDVERSION}"
  DeleteRegKey HKLM "SOFTWARE\${APPNAME}"

  ; Delete self
  Delete "$INSTDIR\uninstall.exe"
	
  ; Delete Shortcuts
  RMDir /r "$SMPROGRAMS\${APPNAMEANDVERSION}"

  ; Delete files/folders
;  RMDir /r "$INSTDIR\data_dir" - do not remove this, as it might be the actual application 'data_dir'
  RMDir /r "$INSTDIR\bin"
  RMDir /r "$INSTDIR\etc"
  RMDir /r "$INSTDIR\modules"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\logs"
  RMDir /r "$INSTDIR\resources"
  RMDir /r "$INSTDIR\work" ; working data
  RMDir /r "$INSTDIR\webapps"
  RMDir /r "$INSTDIR\v*" ; EPSG DB
  Delete "$INSTDIR\*.*"

  RMDir "$INSTDIR\" ; no /r!

  IfFileExists "$INSTDIR\*.*" 0 +2
    MessageBox MB_OK|MB_ICONEXCLAMATION "Warning: Some files and folders could not be removed from:$\r$\n  $INSTDIR."  

SectionEnd

; The End
