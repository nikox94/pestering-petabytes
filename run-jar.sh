#!/bin/bash

java -jar target/sentiment-app-0.0.1-SNAPSHOT.jar | tee run-log.txt

read -p "Exit? " answer

