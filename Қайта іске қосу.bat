@echo off
chcp 65001 >nul
setlocal EnableExtensions
title NEXORA - қайта іске қосу
cd /d "%~dp0"
echo.
echo  [..] NEXORA қайта іске қосылуда: алдымен тоқтатылады, кейін қайта жиналып іске қосылады.
echo.
where docker >nul 2>&1
if errorlevel 1 if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" set "PATH=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin;%PATH%"
docker info >nul 2>&1
if not errorlevel 1 docker compose stop
call "%~dp0Старт.bat"
