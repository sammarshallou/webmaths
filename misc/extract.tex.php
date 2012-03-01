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

Copyright 2012 The Open University
*/
// This script analyses text files (dumps of forum posts, in my case)
// to find $$...$$ TeX equations. The source files were created from
// our two systems using this SQL:

// select message from mdl_forumng_posts where message like '%$$%' and oldversion=0 and deleted=0

header('Content-Type: text/plain; encoding=UTF-8');

$messages = file_get_contents('tex.messages.l1.csv');
$messages .= file_get_contents('tex.messages.l2.csv');

preg_match_all('~\$\$(.*?)\$\$~', $messages, $matches);

$alltex = array();
foreach ($matches[1] as $tex)
{
    $tex = trim(html_entity_decode(strip_tags($tex), ENT_QUOTES, 'UTF-8'));
    $alltex[$tex] = true;
}

ksort($alltex);

foreach ($alltex as $tex => $ignore)
{
    print "$tex\n";
}
