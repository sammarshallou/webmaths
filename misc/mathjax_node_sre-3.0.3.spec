#
# Spec file for mathjax node sre
#
Summary: Server-side implementation of MathJax equation system extended with speech rule engine
Name: mathjax_node_sre
Version: 3.0.3
Release: 1
License: Apache License
Group: System Environment/Libraries
Source: https://github.com/pkra/mathjax-node-sre/archive/3.0.3.tar.gz
URL: https://github.com/pkra/mathjax-node-sre/
Vendor: MathJax Consortium
Packager: sam marshall
Requires: nodejs >= 4.0, http-parser, libuv

%description
mathjax-node-sre package.

%prep
%setup -n mathjax-node-sre-3.0.3
rm -rf .git
rm -f .git*
rm -rf batik
rm -rf test-files

%build
npm install

%install
# remove this file because rpmbuild can't handle files with spaces
rm "node_modules/tape/test/has spaces.js"
rm -rf $RPM_BUILD_ROOT/opt/mathjax_node_sre
mkdir -p $RPM_BUILD_ROOT/opt/mathjax_node_sre
cp -r * $RPM_BUILD_ROOT/opt/mathjax_node_sre
find $RPM_BUILD_ROOT '(' -type d -o -type l -o -type f ')' -print | sed "s@^$RPM_BUILD_ROOT@@g" > /tmp/tmp-filelist

%files -f /tmp/tmp-filelist
%defattr(-,root,root,-)

