<?php
// This script needs to be run inside a Moodle installation. It is used to build
// the mathml.entities.txt file based on a list of files (below) that was copied
// out of the MathML DTD (and messed with).

require_once('config.php');
require_once($CFG->dirroot . '/lib/filelib.php');

header('Content-Type: text/plain');

$files = <<<END
iso9573-13/isoamsa.ent
iso9573-13/isoamsb.ent
iso9573-13/isoamsc.ent
iso9573-13/isoamsn.ent
iso9573-13/isoamso.ent
iso9573-13/isoamsr.ent
iso9573-13/isogrk3.ent
iso9573-13/isomfrk.ent
iso9573-13/isomopf.ent
iso9573-13/isomscr.ent
iso9573-13/isotech.ent
iso8879/isobox.ent
iso8879/isocyr1.ent
iso8879/isocyr2.ent
iso8879/isodia.ent
iso8879/isolat1.ent
iso8879/isolat2.ent
iso8879/isonum.ent
iso8879/isopub.ent
mathml/mmlextra.ent
mathml/mmlalias.ent
END;
$split = split("\n", $files);
$start = 'http://www.w3.org/Math/DTD/mathml2/';

foreach ($split as $ent) {
    $url = $start . $ent;
    
    $result = download_file_content($url);
    $result = preg_replace('~<!--.*?-->~s', '', $result);
    $result = preg_replace('~ {2,}~', ' ', $result);
    $result = preg_replace('~<!ENTITY % plane1D.*?>~', '', $result);
    $result = str_replace('" >', '">', $result);
    
    $lines = split("\n", $result);
    foreach ($lines as $line) {
        if (trim($line) == '') {
            continue;
        }
        $matches = array();
//        <!ENTITY UpEquilibrium "&#x0296E;">
        if (preg_match('~^<!ENTITY ([^ ]+) \"(.*?)\">$~', $line, $matches)) {
            $value = $matches[2];
            $value = str_replace('%plane1D;', '&#38;#38;#x1D', $value);
            $value = html_entity_decode($value, ENT_QUOTES, 'UTF-8');
            $value = str_replace('&#38;', '&', $value);
            print $matches[1] . '=' . $value . "\n";
        } else {
            print "ARGH! Invalid line [$line]";
            exit;
        }
    }
}
