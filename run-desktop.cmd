@echo off
setlocal

set "APP_EXE=%~dp0dist\MSR AMIS\MSR AMIS.exe"

if not exist "%APP_EXE%" (
  echo Desktop app image not found.
  echo Build it first with:
  echo   build-desktop.cmd
  exit /b 1
)

start "" "%APP_EXE%"
