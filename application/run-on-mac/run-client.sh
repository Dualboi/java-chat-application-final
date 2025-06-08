#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/../target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$JAR" ]; then
  echo "ERROR: JAR file not found at $JAR"
  exit 1
fi

java -version

java -jar "$JAR" client