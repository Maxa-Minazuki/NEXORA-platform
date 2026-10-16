@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
title NEXORA - іске қосу
rem ----------------------------------------------------------------------
rem  NEXORA жүйесін Docker Compose арқылы іске қосу.
rem  Дерекқор мен Docker томдары ешқашан жойылмайды.
rem ----------------------------------------------------------------------
cd /d "%~dp0"

echo.
echo  ==============================================================
echo    NEXORA - Біртұтас басқару платформасы: іске қосу
echo  ==============================================================
echo    Жоба бумасы: %CD%
echo.

rem ---------- 1. Docker командасы бар ма?
where docker >nul 2>&1
if not errorlevel 1 goto docker_cli_ok
if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" set "PATH=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin;%PATH%"
where docker >nul 2>&1
if errorlevel 1 goto no_docker
:docker_cli_ok

rem ---------- 2. Docker Engine жұмыс істеп тұр ма?
docker info >nul 2>&1
if not errorlevel 1 goto engine_ready

echo  [..] Docker Engine әлі іске қосылмаған. Docker Desktop іздеуде...
set "DD="
if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" set "DD=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
if exist "%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe" set "DD=%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe"
if not defined DD goto no_docker
echo  [..] Docker Desktop іске қосылуда. Бұл 1-3 минут алуы мүмкін.
start "" "%DD%"
set /a WAITED=0
:wait_engine
ping -n 4 127.0.0.1 >nul
docker info >nul 2>&1
if not errorlevel 1 goto engine_ready
set /a WAITED+=3
<nul set /p "=."
if !WAITED! GEQ 240 goto engine_timeout
goto wait_engine

:engine_ready
echo.
echo  [OK] Docker Engine дайын.
docker compose version >nul 2>&1
if errorlevel 1 goto no_compose

rem ---------- 3. .env файлы
if exist ".env" goto env_exists
if not exist ".env.example" goto no_env_example
docker volume inspect nexora_mysql-data >nul 2>&1
if not errorlevel 1 goto env_missing_but_data
echo  [..] .env файлы жоқ - .env.example негізінде жасалып, кездейсоқ құпиясөздер қойылуда...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; function NewPw { $b = New-Object byte[] 18; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); ([Convert]::ToBase64String($b)) -replace '[+/=]','x' }; $t = [IO.File]::ReadAllText((Join-Path (Get-Location) '.env.example')); $t = $t.Replace('change_me_db_password', (NewPw)).Replace('change_me_root_password', (NewPw)); [IO.File]::WriteAllText((Join-Path (Get-Location) '.env'), $t, (New-Object Text.UTF8Encoding $false))"
if errorlevel 1 goto env_create_failed
echo  [OK] .env жасалды. Құпиясөздер тек сол файлда сақталады.
:env_exists
findstr /r /b /c:"DB_PASSWORD=..*" ".env" >nul
if errorlevel 1 goto env_incomplete
findstr /r /b /c:"MYSQL_ROOT_PASSWORD=..*" ".env" >nul
if errorlevel 1 goto env_incomplete
findstr /c:"change_me" ".env" >nul
if not errorlevel 1 echo  [!] Ескерту: .env файлында әлі «change_me...» үлгі құпиясөздері тұр. Оларды өзгерту ұсынылады.
echo  [OK] .env файлы табылды.

set "PORT=8088"
for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"FRONTEND_PORT=" ".env"') do if not "%%b"=="" set "PORT=%%b"

rem ---------- 4. Конфигурацияны тексеру
docker compose config -q >nul 2>"%TEMP%\nexora-config.log"
if errorlevel 1 goto config_error
echo  [OK] docker-compose.yml конфигурациясы дұрыс.

rem ---------- 5. Жинау және іске қосу
echo.
echo  [..] Контейнерлер жиналып, іске қосылуда.
echo       Алғашқы рет бірнеше минут алуы мүмкін - терезені жаппаңыз.
echo.
docker compose up -d --build
if errorlevel 1 goto compose_error

rem ---------- 6. Сайттың дайын болуын күту
echo.
echo  [..] Сайт іске қосылуын күту: http://localhost:!PORT!
set /a WAITED=0
:wait_http
set "CODE="
for /f %%c in ('curl.exe -s -o nul -w "%%{http_code}" "http://localhost:!PORT!/api/auth/csrf" 2^>nul') do set "CODE=%%c"
if "!CODE!"=="200" goto site_ready
ping -n 4 127.0.0.1 >nul
set /a WAITED+=3
<nul set /p "=."
if !WAITED! GEQ 300 goto http_timeout
goto wait_http

:site_ready
echo.
echo.
docker compose ps
echo.
echo  ==============================================================
echo    [OK] NEXORA іске қосылды.
echo    Сайт:      http://localhost:!PORT!
echo    Swagger:   http://localhost:!PORT!/swagger-ui.html
echo.
echo    Демо-аккаунттар (логин / құпиясөз):
echo      Әкімші       admin    / Admin#2026
echo      Менеджер     manager  / Manager#2026
echo      Дәрігер      doctor   / Doctor#2026
echo      Қызметкер    employee / Employee#2026
echo      Пайдаланушы  user     / User#2026
echo.
echo    Тоқтату үшін: Стоп.bat
echo  ==============================================================
if not defined NEXORA_NO_BROWSER start "" "http://localhost:!PORT!"
echo.
pause
exit /b 0

rem ---------------------------------------------------------------- қателер
:no_docker
echo.
echo  [X] Docker табылмады.
echo      NEXORA жұмыс істеуі үшін Docker Desktop for Windows қажет.
echo      1. https://www.docker.com/products/docker-desktop/ сайтынан жүктеп, орнатыңыз.
echo      2. Компьютерді қайта іске қосып, Docker Desktop-ты бір рет ашыңыз.
echo      3. Осы файлды қайта іске қосыңыз.
goto fail

:engine_timeout
echo.
echo  [X] Docker Engine 4 минут ішінде іске қосылмады.
echo      Docker Desktop терезесін ашып, қате хабарламасын тексеріңіз
echo      (мысалы, WSL 2 немесе виртуализация өшірулі болуы мүмкін), содан кейін қайталаңыз.
goto fail

:no_compose
echo.
echo  [X] «docker compose» командасы жоқ. Docker Desktop-ты соңғы нұсқаға жаңартыңыз.
goto fail

:no_env_example
echo.
echo  [X] .env және .env.example файлдары табылмады. Жоба толық көшірілгенін тексеріңіз.
goto fail

:env_missing_but_data
echo.
echo  [X] .env файлы жоқ, бірақ NEXORA дерекқоры (Docker томы) бұрыннан бар.
echo      Жаңа құпиясөздер ескі дерекқорға сәйкес келмейді, сондықтан .env автоматты жасалмады.
echo      Бұрынғы .env файлын жоба бумасына қайтарыңыз немесе .env.example-ді .env деп көшіріп,
echo      бұрынғы DB_PASSWORD және MYSQL_ROOT_PASSWORD мәндерін жазыңыз.
goto fail

:env_create_failed
echo.
echo  [X] .env файлын жасау мүмкін болмады. .env.example-ді .env деп қолмен көшіріп,
echo      DB_PASSWORD және MYSQL_ROOT_PASSWORD мәндерін толтырыңыз.
goto fail

:env_incomplete
echo.
echo  [X] .env файлында құпиясөздер толтырылмаған.
echo      .env файлын Блокнотпен ашып, келесі жолдарды толтырыңыз:
echo        DB_PASSWORD=өз_құпиясөзіңіз
echo        MYSQL_ROOT_PASSWORD=басқа_құпиясөз
echo      Содан кейін Старт.bat файлын қайта іске қосыңыз.
goto fail

:config_error
echo.
echo  [X] docker-compose.yml немесе .env конфигурациясында қате бар:
type "%TEMP%\nexora-config.log"
goto fail

:compose_error
echo.
echo  [X] Docker Compose контейнерлерді іске қоса алмады - сайт іске қосылған ЖОҚ.
echo      Логтарды көру үшін жоба бумасында мына командаларды орындаңыз:
echo        docker compose ps
echo        docker compose logs --tail 100 backend
echo        docker compose logs --tail 100 mysql
goto fail

:http_timeout
echo.
echo  [X] Контейнерлер іске қосылды, бірақ сайт 5 минут ішінде жауап бермеді.
docker compose ps
echo      Логтарды тексеріңіз:
echo        docker compose logs --tail 100 backend
echo        docker compose logs --tail 50 frontend
goto fail

:fail
echo.
pause
exit /b 1
