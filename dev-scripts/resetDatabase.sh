#!/usr/bin/env bash
cd ..

echo "======"
echo "====== STEP 1/2: start to delete database"
echo "======"
./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.Neo4jDelete"
echo "======"
echo "====== STEP 1/2: finished deleting database"
echo "======"

echo "======"
echo "====== STEP 2/2: create constraints and indices"
echo "======"
./sbt "cmdtools/runMain com.ubirch.keyservice.cmd.InitData"
echo "======"
echo "====== STEP 2/2: finished creating constraints and indices"
echo "======"
