#!/bin/sh
# Compila y ejecuta el simulador (requiere JDK 8 o superior)
mkdir -p out
javac -encoding UTF-8 -d out src/*.java && java -cp out SimuladorChomsky
