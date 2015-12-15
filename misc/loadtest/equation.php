<?php
require_once(dirname(__FILE__) . '/config.php');

$length = required_param('length', PARAM_ALPHA);
$mode = required_param('mode', PARAM_ALPHA);

if ($mode === 'mathjax') {
    $shortname = get_config('filter_oumaths', 'pilotwebsite');
    if (!$shortname) {
        throw new moodle_exception('error_nomathjaxcourse', 'oucontent');
    }
    $pilotcourseid = $DB->get_field('course', 'id', array('shortname' => $shortname),
            MUST_EXIST);
    $PAGE->set_context(context_course::instance($pilotcourseid));
} else if ($mode === 'legacy') {
    $PAGE->set_context(context_system::instance());
} else {
    throw new coding_exception('Wrong mode');
}

$PAGE->set_url(new moodle_url('/equation.php'));

echo $OUTPUT->header();
$equation = mt_rand() . '.' . mt_rand();
if ($length === 'long') {
    for ($i = 0; $i < 10; $i++) {
        $equation .= ' + \left( ';
        for ($j = 0; $j < 10; $j++) {
            if ($j !== 0) {
                $equation .= ' + ';
            }
            $equation .= '4 + \frac{\sqrt{3}}{\sqrt{7}}';
        }
        $equation .= ' \right)';
    }
} else if ($length === 'short') {
    $equation .= ' + i^2';
} else {
    throw new coding_exception('Wrong length');
}

$result = format_text('$$' . $equation . '$$');
echo $result;
if ($mode === 'mathjax') {
    if ($length === 'short' && strpos($result, '</svg>') === false) {
        throw new coding_exception('MathJax not working (expected success)?');
    }
    if ($length === 'long' && strpos($result, 'Timeout reading line') === false) {
        throw new coding_exception('MathJax not working (expected timeout)?');
    }
}
if ($mode === 'legacy' && strpos($result, '<img ') === false) {
    throw new coding_exception('Legacy not working?');
}
echo '<p>Page end OK</p>';
echo $OUTPUT->footer();
