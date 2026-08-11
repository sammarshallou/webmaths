#!/bin/bash

# Test script to make an SVG based on input file. You need to put this in the same location as
# the Node app.

# Run like makesvg.sh "1+1" > output.svg
# Or makesvg.sh --inline "1+1" > output.svg
# You can use MathML or TeX, it will detect MathML if the equation starts with a <
EQUATION=$1

if [[ "$EQUATION" =~ ^--inline$ ]]; then
  EQUATION=$2
  FORMAT=inline-TeX
elif [[ "$EQUATION" =~ ^\<.* ]]; then
  FORMAT=MathML
else
  FORMAT=TeX
fi

# I couldn't get it to work with directly running the sed pipeline below on the node output, so I
# save to a file instead.
TEMPFILE=`mktemp`
node ou-mathjax.mjs TeX >$TEMPFILE <<EOF
$FORMAT
$EQUATION

QUIT
EOF

# Output the svg now.
RESULT=`cat $TEMPFILE | sed '1,/^<<BEGIN:SVG*$/d' | sed '/^<<END:SVG$/,$d'`
ERRORS=`cat $TEMPFILE | sed '1,/^<<BEGIN:ERRORS*$/d' | sed '/^<<END:ERRORS$/,$d'`
if [[ ! $RESULT ]]; then
  echo '' 1>&2
  echo 'ERRORS:' 1>&2
  echo $ERRORS 1>&2
else
  echo '' 1>&2
  echo $RESULT
fi

# Delete temp file.
rm $TEMPFILE
