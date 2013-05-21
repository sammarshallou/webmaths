<?php
/*
This file is part of OU webmaths

OU webmaths is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

OU webmaths is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with OU webmaths. If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 The Open University
*/
// This script needs to be run inside a Moodle installation. It is used to build
// the mathml.descriptions.txt file based on a list of files (below) that was
// copied out of the MathML DTD (and messed with).

require_once('config.php');
require_once($CFG->dirroot . '/lib/filelib.php');

header('Content-Type: text/plain');

// Note: The files are in priority order (highest priority first) for cases
// when two files define the same symbol.
$files = <<<END
mathml/mmlalias.ent
mathml/mmlextra.ent
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
END;
$split = preg_split("~\n~", $files);


$start = 'http://www.w3.org/Math/DTD/mathml2/';
$results = array();

foreach ($split as $ent) {
    $url = $start . $ent;
    
    $target = $CFG->dataroot . '/mml/' . $ent;
    if (!file_exists($target)) {
      if(!dir_exists(dirname($target))) {
        mkdir(dirname($target));
      }
			$result = download_file_content($url);
			if(!$result) {
        throw new coding_exception("Failed to download: $url");
			}
			file_put_contents($target, $result);
		} else {
			$result = file_get_contents($target);
		}
/*
    $result = preg_replace('~<!--.*?-->~s', '', $result);
    $result = preg_replace('~ {2,}~', ' ', $result);
    $result = preg_replace('~<!ENTITY % plane1D.*?>~', '', $result);
    $result = str_replace('" >', '">', $result);
*/    
    $lines = preg_split("~\n~", $result);
    foreach ($lines as $line) {
        if (trim($line) == '') {
            continue;
        }
        if (preg_match('~^<!ENTITY(?! % plane1D )~', $line)) {
            $matches = array();
            if (preg_match('~^[^ ]+ ([^ ]+) +"([^"]+)" ><!--(.*?)-->$~', $line, $matches)) {
                $ref = $matches[1];

                $value = trim($matches[2]);
                $value = str_replace('%plane1D;', '&#38;#38;#x1D', $value);
                $value = html_entity_decode($value, ENT_QUOTES, 'UTF-8');
                $value = str_replace('&#38;', '&', $value);
                
                $desc = trim($matches[3]);
                $desc = preg_replace('~^/[^:=-]+[:=-]\s*~', '', $desc);
                $desc = preg_replace('~^/[^:= -]+ \s*~', '', $desc);
                $desc = preg_replace('~\bdbl\b~', 'double', $desc);
                $desc = preg_replace('~\bdn\b~', 'down', $desc);
//                    $desc = preg_replace('~\bl\b~', 'left', $desc);
                $desc = preg_replace('~\brt\b~', 'right', $desc);
                $desc = preg_replace('~\bl&r\b~', 'left and right', $desc);
                $desc = preg_replace('~\barr\b~', 'arrow', $desc);
                $desc = preg_replace('~\bharp\b~', 'harpoon', $desc);
                $desc = preg_replace('~ & ~', ' and ', $desc);
                $desc = preg_replace('~\bNW\b~', 'northwest', $desc);
                $desc = preg_replace('~\bNE\b~', 'northeast', $desc);
                $desc = preg_replace('~\bSW\b~', 'southwest', $desc);
                $desc = preg_replace('~\bSE\b~', 'southeast', $desc);
                $desc = preg_replace('~\bgt-or-eq\b~', 'greater-than-or-equal', $desc);
                $desc = preg_replace('~\bless-or-eq\b~', 'less-than-or-equal', $desc);
                $desc = preg_replace('~\bgtr\b~', 'greater', $desc);
                $desc = preg_replace('~\beq\b~', 'equal', $desc);
                $desc = preg_replace('~, Greek$~', '', $desc);
                $desc = preg_replace('~^=~', '', $desc);
                
                $results[$ref] = array('d'=>$desc, 'v'=>$value);
                //print $ref . " - $desc - $ent - $value - " . strlen($value) . "\n";
            } else {
                print "ARGH! Invalid line $line"; exit;
            }
        }
    }
}
require_once('utf8.inc');
$outfile = "# Automatically generated. Do not edit; use override.descriptions.txt instead.\n";
$done = array();
foreach ($results as $key=>$value) {
    if (preg_match('~^((alias\s+[^ ]+\s+)|(ISO[A-Z]+\s+))([^ ]+)~', $value['d'], $matches)) {
        $from = $matches[4];
        $from = preg_replace('~^&(.*);$~', '$1', $from);
        if (array_key_exists($from, $results)) {
            $value['d'] = $results[$from]['d'];
          } else {
//            print "\n\nUNKNOWN REF: $from ({$value['d']})\n";
        }
     }
     $matches = array();
     if (preg_match('~^&#x([A-Za-z0-9]*);$~', $value['v'], $matches)) {
         $ent = ltrim(strtolower($matches[1]), '0');
     } else {
         $what = utf8ToUnicode($value['v']);
         $ent = '';
         foreach ($what as $number) {
           $ent .= dechex($number);
           $ent .= ',';
         }
         $ent = rtrim($ent, ',');
     }
     $line = $ent . '=' . $value['d'] . "\n";
     if (!empty($done[$ent])) {
         // Comment out duplicates.
         $line = '#' . $line;
     }
     $done[$ent] = true;
     print $line;
     $outfile .= $line;
}
file_put_contents($CFG->dataroot . '/mml/mathml.descriptions.txt', $outfile);