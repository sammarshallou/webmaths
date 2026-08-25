#
# Spec file. The version tokens will be replaced by the build script.
#
Summary: OU wrapper for rendering equations using MathJax inside Node
Name: ou-mathjax
Version: %%VERSION%%
Release: 1
License: Apache License
Group: System Environment/Libraries
Source: ou-mathjax.tar.gz
URL: https://github.com/sammarshallou/webmaths/
Vendor: The Open University
Packager: sam marshall
Requires: nodejs >= 20.0, http-parser, libuv

%description
ou-mathjax package.

%prep
%setup -c -n ou-mathjax-%%VERSION%%

%build
pwd
ls -l
npm install

%install
rm -rf $RPM_BUILD_ROOT/opt/ou-mathjax
mkdir -p $RPM_BUILD_ROOT/opt/ou-mathjax
cp -r * $RPM_BUILD_ROOT/opt/ou-mathjax

%files
%defattr(-,root,root,-)
/opt/ou-mathjax
