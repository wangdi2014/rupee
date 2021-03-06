#! /bin/bash

ver=$1

# bundle definitions
./bundle.sh > bundle.txt

# delete output directory if it already exist
[ -d ./chopped ] && rm -r chopped

# create output directory
mkdir ./chopped

# message
echo "To the chopper! takes about 45 minutes"

# run the chopper
xargs -a bundle.txt -P8 -L1 ./chopper.sh

# delete empty chopped files
find ./chopped -size 0 -delete

# message
echo "Zipping the chopped files: takes about 30 minutes"

# gzip the chopped files 
find ./chopped -name "*.pdb" -exec gzip {} \;

# move to app directory
cd ../../rupee-search/target

# message
echo "Writing chain definitions: takes about 4 hours"

# write chain definitions
java -jar rupee-search-0.0.1-SNAPSHOT-jar-with-dependencies.jar -c $ver 

# done
echo "Done"

