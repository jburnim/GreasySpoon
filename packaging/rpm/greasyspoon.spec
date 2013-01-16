
# TODO:
# need to review integration with JVM - was tested with Sun's JVM,
#  not the stuff that comes with Fedora and no data no how well
#  failure is dealt with in this case
# make it run as it's own user
# review and tweak all file ownership and permissions
# need to verify that log rotation works correctly

%define		name		greasyspoon
%define		pkgrelease	1
%define		microrelease	1

Name:		greasyspoon
Version:	0.5.5
Release:	%{pkgrelease}.%{microrelease}
License:	GPL
Group:		System Environment/Daemons
Source0:	http://prdownloads.sourceforge.net/greasyspoon/greasyspoon-release-%{version}-sources.zip
URL:		http://greasyspoon.sourceforge.net/
Requires:	logrotate
BuildArch:	i386
BuildRoot:	%{_tmppath}/%{name}-root

Summary:	greasyspoon - ICAP server, written in Java, for dynamic HTTP content modification

%description
GreasySpoon is a scripting engine running on top of an ICAP server.
Inspired by the Firefox GreaseMonkey extension, it allows developers
to modify HTTP traffic and web pages in a simple way by transparently
managing traffic interception and communication with a Proxy server.
Since it runs as a network service, it does not require any installation
on end-user devices, and provides therefore multi-OS and cross-browser
support.  It designed to work with any HTTP proxy server which includes
an ICAP client, such as Squid, Shweby or commercial appliances like
BlueCoat.

GreasySpoon supports Java/javascript language by default, but can
be easily extended with other scripting languages: Ruby, Python, AWK,
Groovy, etc. Providing a web based interface, it tries to be as
user-friendly as possible, while keeping an eye on performance that
makes it usable from early prototyping stages up to production environment. 

%prep
#%setup -q
cd ${RPM_SOURCE_DIR}
unzip -o -d ${RPM_BUILD_DIR} %{name}-release-%{version}-sources.zip
cd ${RPM_BUILD_DIR}/%{name}-release-%{version}-sources

%build
cd ${RPM_BUILD_DIR}/%{name}-release-%{version}-sources
ant


%install

# move to correct dir
cd ${RPM_BUILD_DIR}/%{name}-release-%{version}-sources

# start fresh
rm -rf ${RPM_BUILD_ROOT}

# our /etc subdir
install -d ${RPM_BUILD_ROOT}/%_sysconfdir/%{name}

# conf dir
install -d ${RPM_BUILD_ROOT}/%_sysconfdir/%{name}/conf

# install config files
install build/conf/* ${RPM_BUILD_ROOT}/%_sysconfdir/%{name}/conf/

# binary
install -d ${RPM_BUILD_ROOT}/%_bindir/
install -m a+rx,u+rwx sys/linux/%{_arch}/greasyspoon-jsvc ${RPM_BUILD_ROOT}/%_bindir/
#sleep 20s

# main application directory under /var/
install -d ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/

# admin
cp -Rav build/admin ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/

# libraries
install -d ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/lib
cp build/lib/*.jar ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/lib/

# serverscripts
install -d ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/serverscripts
cp build/serverscripts/* ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/serverscripts

# logs
install -d ${RPM_BUILD_ROOT}/%_localstatedir/log/%{name}

# docs
install -d ${RPM_BUILD_ROOT}/%_defaultdocdir/%{name}-%{version}/
install build/CHANGELOG.txt ${RPM_BUILD_ROOT}/%_defaultdocdir/%{name}-%{version}/
install build/CREDITS.txt ${RPM_BUILD_ROOT}/%_defaultdocdir/%{name}-%{version}/
install build/INSTALL.txt ${RPM_BUILD_ROOT}/%_defaultdocdir/%{name}-%{version}/
install build/LICENSE.txt ${RPM_BUILD_ROOT}/%_defaultdocdir/%{name}-%{version}/

# init script and related
install -d ${RPM_BUILD_ROOT}/%_initrddir
install -m a+rx,u+rwx sys/linux/noarch/fedora/init-greasyspoon ${RPM_BUILD_ROOT}/%_initrddir/greasyspoon
install -d ${RPM_BUILD_ROOT}/%_sysconfdir/sysconfig
install sys/linux/noarch/fedora/sysconfig-greasyspoon ${RPM_BUILD_ROOT}/%_sysconfdir/sysconfig/greasyspoon
install -d ${RPM_BUILD_ROOT}/%_sysconfdir/logrotate.d
install sys/linux/noarch/fedora/logrotate-greasyspoon ${RPM_BUILD_ROOT}/%_sysconfdir/logrotate.d/greasyspoon

# create symlinks
ln -s ../log/greasyspoon ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/log
ln -s ../../etc/%{name}/conf ${RPM_BUILD_ROOT}/%_localstatedir/%{name}/conf

%clean
[ "${RPM_BUILD_ROOT}" != "/" ] && [ -d "${RPM_BUILD_ROOT}" ] && rm -rf "${RPM_BUILD_ROOT}"


%files
%config(noreplace) %verify(not size mtime md5) %_sysconfdir/%{name}
%_bindir/greasyspoon-jsvc
%_initrddir/greasyspoon
%_sysconfdir/sysconfig/greasyspoon
%_sysconfdir/logrotate.d/greasyspoon
%_localstatedir/%{name}/admin
%_localstatedir/%{name}/log
%_localstatedir/%{name}/lib
%_localstatedir/%{name}/conf
%_localstatedir/log/%{name}
%config(noreplace) %verify(not size mtime md5) %_localstatedir/%{name}/serverscripts/*
%doc %_defaultdocdir/%{name}-%{version}/


%changelog
* Wed Apr 29 2009 Brad
- Initial version
