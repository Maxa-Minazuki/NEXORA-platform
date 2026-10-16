@echo off
chcp 65001 >nul
setlocal EnableExtensions
title NEXORA - тоқтату
rem ----------------------------------------------------------------------
rem  NEXORA контейнерлерін қауіпсіз тоқтату («docker compose stop»).
rem  Дерекқор мен Docker томдары сақталады; «down -v» қолданылмайды.
rem ----------------------------------------------------------------------
cd /d "%~dp0"

echo.
echo  ==============================================================
echo    NEXORA - жүйені тоқтату
echo  ==============================================================
echo.

where docker >nul 2>&1
if not errorlevel 1 goto docker_cli_ok
if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" set "PATH=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin;%PATH%"
where docker >nul 2>&1
if errorlevel 1 goto no_docker
:docker_cli_ok

docker info >nul 2>&1
if errorlevel 1 goto engine_off

echo  [..] NEXORA контейнерлері тоқтатылуда (тек осы жобаның контейнерлері)...
docker compose stop
if errorlevel 1 goto stop_error

echo.
echo  [OK] NEXORA тоқтатылды.
echo       Дерекқордағы барлық деректер сақталды. Қайта іске қосу үшін: Старт.bat
echo.
pause
exit /b 0

:no_docker
echo  [!] Docker табылмады - NEXORA контейнерлері жұмыс істеп тұрған жоқ, тоқтатудың қажеті жоқ.
echo.
pause
exit /b 0

:engine_off
echo  [!] Docker Engine жұмыс істеп тұрған жоқ - NEXORA контейнерлері қазір іске қосылмаған.
echo      Деректер Docker томында сақтаулы.
echo.
pause
exit /b 0

:stop_error
echo.
echo  [X] Контейнерлерді тоқтату кезінде қате шықты.
echo      Жағдайын тексеру: docker compose ps
echo      Docker Desktop-ты ашып, NEXORA контейнерлерін сол жерден тоқтатуға болады.
echo.
pause
exit /b 1
