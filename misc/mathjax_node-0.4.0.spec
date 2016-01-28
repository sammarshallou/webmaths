#
# Spec file for MathJax.node
#
Summary: Server-side implementation of MathJax equation system
Name: mathjax_node
Version: 0.4.0
Release: 1
License: Apache License
Group: System Environment/Libraries
Source: https://github.com/mathjax/MathJax-node/archive/0.4.0.tar.gz
URL: https://github.com/mathjax/MathJax-node/
Vendor: MathJax Consortium
Packager: sam marshall
Requires: nodejs >= 4.0, http-parser, libuv

%description
MathJax.node package.

%prep
%setup -n MathJax-node-0.4.0
rm -rf .git
rm -f .git*
rm -rf batik
rm -rf test-files

%build
npm install

%install
rm -rf $RPM_BUILD_ROOT/opt/mathjax_node
mkdir -p $RPM_BUILD_ROOT/opt/mathjax_node
cp -r * $RPM_BUILD_ROOT/opt/mathjax_node
find $RPM_BUILD_ROOT '(' -type d -o -type l -o -type f ')' -print | sed "s@^$RPM_BUILD_ROOT@@g" > /tmp/tmp-filelist

%files -f /tmp/tmp-filelist
%defattr(-,root,root,-)

