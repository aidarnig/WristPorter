@echo off
setlocal
if "%~1"=="" (
  echo Usage: Start-GatewayProbe.cmd 192.168.1.123
  echo Use only your own trusted PRIVATE Wi-Fi. No VPN, guest, hotel, or public network.
  exit /b 2
)
if not exist "%~dp0runtime\bin\java.exe" (
  echo Missing bundled Java runtime. Extract the entire ZIP before running this script.
  exit /b 3
)
"%~dp0runtime\bin\java.exe" -cp "%~dp0classes" io.github.aidarnig.wgs.web.GatewayProbeMain "%~1"
exit /b %ERRORLEVEL%
