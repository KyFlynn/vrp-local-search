#!/bin/bash

########################################
############# CSCI 2951-O ##############
########################################

# Update this file with instructions on how to compile your code
#javac ./src/solver/ls/*.java

### DEPARTMENT MACHINES
javac -classpath /local/projects/cplex/CPLEX_Studio221/cplex/lib/cplex.jar ./src/solver/*.java ./src/solver/complete/*.java ./src/solver/incomplete/*.java ./src/solver/initial/savings/*.java ./src/solver/initial/sweep/*.java ./src/solver/util/*.java