#!/usr/bin/env bash
osascript -e "do shell script \"java -Dlogs=./log -jar dns-server-0.0.1-shadow.jar\" with administrator privileges"
