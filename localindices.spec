Summary: Harvestering Service
Name: masterkey-harvester
Version: 1.9.5
Release: 1.indexdata
License: IndexData
Group: Applications/Internet
Vendor: Index Data ApS <info@indexdata.dk>
Source: masterkey-harvester-%{version}.tar.gz
BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-root
BuildRequires: maven2
Packager: Dennis Schafroth <dennis@indexdata.com>
URL: http://www.indexdata.dk/masterkey
Requires: jpackage-utils-compat-el5
%description
The LUI Harvester is a harvesting platform. It can harvest OAI-PMH, XML bulk and web crawl into a local unified index. It support splitting and XSL transformations. It supports Solr and Zebra as storages. 

%package -n masterkey-harvester-admin
Summary: Harvester Admin 
Group: Applications/Internet

%package -n masterkey-harvester-tomcat
Summary: Harvester Tomcat context
Requires: masterkey-harvester
Group: Applications/Internet

%package -n masterkey-harvester-admin-tomcat
Summary: Harvester Admin Tomcat context
Requires: masterkey-harvester-admin
Group: Applications/Internet

%package -n masterkey-harvester-admin-tomcat6
Summary: Harvester Admin Tomcat 6 integration
Requires: tomcat6 masterkey-harvester-admin-tomcat
Group: Applications/Internet

%package -n masterkey-harvester-tomcat6
Summary: Harvester Tomcat 6 integration
Requires: tomcat6 masterkey-harvester-tomcat
Group: Applications/Internet

%description -n masterkey-harvester
The Harvester is part of the MasterKey suite. This package provides the Harvester Service.

%description -n masterkey-harvester-admin
The Harvester admin is part of the MasterKey suite. This package provides the Harvester Administration software

%description -n masterkey-harvester-admin-tomcat
The Harvester Admin is part of the MasterKey suite. This package provides Tomcat context files.

%description -n masterkey-harvester-tomcat
The Harvester is part of the MasterKey suite. This package provides Tomcat context files.

%description -n masterkey-harvester-admin-tomcat6
The Harvester is part of the MasterKey suite. This package provides Tomcat 6 integration.

%description -n masterkey-harvester-tomcat6
The Harvester is part of the MasterKey suite. This package provides Tomcat 6 integration.

%prep
%setup
%build
mvn package -Pproduction

%define harvester harvester/target/harvester
%define admin harvester-admin/target/harvester-admin
%install
mkdir -p ${RPM_BUILD_ROOT}/%{_datadir}/masterkey/harvester/
cp -a %{harvester}/* ${RPM_BUILD_ROOT}/%{_datadir}/masterkey/harvester/

mkdir -p ${RPM_BUILD_ROOT}/%{_sysconfdir}/masterkey/harvester/
# cp -a etc/harvester.properties ${RPM_BUILD_ROOT}/%{_sysconfdir}/masterkey/harvester/

mkdir -p ${RPM_BUILD_ROOT}/%{_localstatedir}/log/masterkey/harvester

#tomcat context
sed -e 's@docBase=".*"@docBase="%{_datadir}\/masterkey\/harvester"@g' etc/harvester-context.xml > ${RPM_BUILD_ROOT}/%{_sysconfdir}/masterkey/harvester/harvester-context.xml

mkdir -p ${RPM_BUILD_ROOT}/%{_datadir}/masterkey/harvester-admin/
cp -a %{admin}/* ${RPM_BUILD_ROOT}/%{_datadir}/masterkey/harvester-admin/

mkdir -p ${RPM_BUILD_ROOT}/%{_sysconfdir}/masterkey/harvester-admin/
#cp -a etc/harvester.properties ${RPM_BUILD_ROOT}/%{_sysconfdir}/masterkey/harvester-admin/

mkdir -p ${RPM_BUILD_ROOT}/%{_localstatedir}/log/masterkey/harvester

#tomcat context
sed -e 's@docBase=".*"@docBase="%{_datadir}\/masterkey\/harvester"@g' etc/harvester-admin-context.xml > ${RPM_BUILD_ROOT}/%{_sysconfdir}/masterkey/harvester/harvester-admin-context.xml


%clean
rm -fr ${RPM_BUILD_ROOT}

%post -n masterkey-harvester-tomcat6
ln -sf %{_sysconfdir}/masterkey/harvester/harvester-context.xml %{_sysconfdir}/tomcat6/Catalina/localhost/harvester.xml

%postun -n masterkey-harvester-tomcat6
if [ $1 = 0 ]; then
  rm -f %{_sysconfdir}/tomcat6/Catalina/localhost/harvester.xml
fi

%post -n masterkey-harvester-admin-tomcat6
ln -sf %{_sysconfdir}/masterkey/harvester/harvester-admin-context.xml %{_sysconfdir}/tomcat6/Catalina/localhost/harvester-admin.xml

%postun -n masterkey-harvester-admin-tomcat6
if [ $1 = 0 ]; then
  rm -f %{_sysconfdir}/tomcat6/Catalina/localhost/harvester-admin.xml
fi

%files
%defattr(-,root,root)
%{_datadir}/masterkey/harvester
%attr(750,tomcat,tomcat) %dir %{_localstatedir}/log/masterkey/harvester

%files -n masterkey-harvester-admin
%defattr(-,root,root)
%{_datadir}/masterkey/harvester-admin
%attr(750,tomcat,tomcat) %dir %{_localstatedir}/log/masterkey/harvester

%files -n masterkey-harvester-tomcat
%defattr(-,root,root)
%config %{_sysconfdir}/masterkey/harvester/harvester-context.xml

%files -n masterkey-harvester-admin-tomcat
%defattr(-,root,root)
%config %{_sysconfdir}/masterkey/harvester/harvester-admin-context.xml

%files -n masterkey-harvester-tomcat6
%files -n masterkey-harvester-admin-tomcat6
