The load test in bulktest.jmx can be run as follows:

1) You need a Moodle installation that is set up to support MathJax, but with
   default being the legacy filter.

2) Put equation.php temporarily into the root of that installation.

3) Run the JMeter file and set up your hostname e.g. whatever.open.ac.uk, and
   your root path within that server e.g. /moodle

4) You can also change the equation options: 'legacy' or 'mathjax', and
   'short' or 'long'. The 'long' one is designed to time out in current
   MathJax.

Note if you want to test it simultaneously running MathJax and legacy etc you
can do this by running two copies of JMeter.