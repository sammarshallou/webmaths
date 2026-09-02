#!/bin/bash

# Test script to make an SVG based on input file. You need to put this in the same location as
# the Node app.

# Run like makesvg.sh "1+1" > output.svg

EQUATION=$1

# I couldn't get it to work with directly running the sed pipeline below on the node output, so I
# save to a file instead.
TEMPFILE=`mktemp` 
#node ou-mathjax.mjs TeX >$TEMPFILE 2>/dev/null <<EOF 
node ou-mathjax.mjs TeX >$TEMPFILE <<EOF 
TeX
$EQUATION

QUIT
EOF

# Output the svg now.
cat $TEMPFILE | sed '1,/^<<BEGIN:SVG*$/d' | sed '/^<<END:SVG$/,$d'

# Delete temp file.
rm $TEMPFILE
