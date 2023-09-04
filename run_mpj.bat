@echo off
cd C:\mpj\mpj-v0_44\MPI-HeatSimulator\out\production\MPI-HeatSimulator
set MPJ_HOME=C:\mpj\mpj-v0_44
set PATH=%MPJ_HOME%\bin;%PATH%
mpjrun.bat -np 4 Main
