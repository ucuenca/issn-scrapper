#!/bin/bash

while IFS= read -r line
do
    curl -d "$line" -X POST http://201.159.222.25:8180/repositories/data/statements
    echo $line
done < ta
