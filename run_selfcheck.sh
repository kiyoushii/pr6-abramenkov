#!/bin/sh
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java") || exit 1
java -Dfile.encoding=UTF-8 -cp out ru.edu.pr03.SelfCheck
