@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
wscript.exe "%SCRIPT_DIR%start-mbote-capture-sync-hidden.vbs"
