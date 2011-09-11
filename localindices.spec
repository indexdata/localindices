Summary: Harvestering Service
Name: lui-harvester
Version: 0.1.2
Release: 1.indexdata
License: IndexData
Group: Applications/Internet
Vendor: Index Data ApS <info@indexdata.dk>
Source: lui-harvester-%{version}.tar.gz
BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-root
BuildRequires: maven2
Packager: Dennis Schafroth <dennis@indexdata.com>
URL: http://www.indexdata.dk/masterkey

%description
The LUI Harvester is a harvesting platform. It can harvest OAI-PMH, XML bulk and web crawl into a local unified index. It support splitting and XSL transformations. It supports Solr and Zebra as storages. 

%package -n lui-harvester-admin
Summary: Harvester Admin 
Requires: harvester
Group: Applications/Internet

%package -n lui-harvester-admin-tomcat
Summary: Harvester Admin Tomcat context
Requires: harvester-admin
Requires: harvester
Group: Applications/Internet

%package -n lui-harvester-tomcat
Summary: Harvester Tomcat context
Requires: harvester-admin
Requires: harvester
Group: Applications/Internet

%package -n lui-harvester-admin-tomcat5
Summary: Harvester Tomcat 5.5 integration
Requires: tomcat5 harvester-tomcat
Group: Applications/Internet

%package -n lui-harvester-tomcat5
Summary: Harvester Tomcat 5.5 integration
Requires: tomcat5 harvester-tomcat
Group: Applications/Internet

%package -n lui-harvester-admin-tomcat6
Summary: Harvester Admin Tomcat 6 integration
Requires: tomcat6 harvester-tomcat
Group: Applications/Internet

%package -n lui-harvester-tomcat6
Summary: Harvester Tomcat 6 integration
Requires: tomcat6 harvester-tomcat
Group: Applications/Internet


%description -n lui-harvester
The Harvester is part of the MasterKey suite. This package provides the Harvester Service.

%description -n lui-harvester-admin
The Harvester admin is part of the MasterKey suite. This package provides the Harvester Administration software

%description -n lui-harvester-admin-tomcat
The Harvester Admin is part of the MasterKey suite. This package provides Tomcat context files.

%description -n lui-harvester-tomcat
The Harvester is part of the MasterKey suite. This package provides Tomcat context files.

%description -n lui-harvester-tomcat5
The Harvester is part of the MasterKey suite. This package provides Tomcat 5.5 integration.

%description -n lui-harvester-admin-tomcat5
The Harvester is part of the MasterKey suite. This package provides Tomcat 5.5 integration.

%description -n lui-harvester-admin-tomcat6
The Harvester is part of the MasterKey suite. This package provides Tomcat 6 integration.

%description -n lui-harvester-tomcat6
The Harvester is part of the MasterKey suite. This package provides Tomcat 6 integration.

%prep
%setup
%build
mvn package -Pproduction

%define harvester harvester/target/localindices-harvester
%define admin harvester-admin/target/localindices-admin
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

mkdir -p ${RPM_BUILD_ROOT}/%{_localstatedir}/log/masterkey/harvester-admin

#tomcat context
sed -e 's@docBase=".*"@docBase="%{_datadir}\/masterkey\/harvester"@g' etc/harvester-admin-context.xml > ${RPM_BUILD_ROOT}/%{_sysconfdir}/masterkey/harvester-admin/harvester-admin-context.xml


%clean
rm -fr ${RPM_BUILD_ROOT}

%post -n lui-harvester-tomcat5
need_restart=false
#we force symlink to make sure app is redeployed during update
ln -sf %{_sysconfdir}/masterkey/harvester/harvester-context.xml %{_sysconfdir}/tomcat5/Catalina/localhost/harvester.xml
if $need_restart; then
  /sbin/service tomcat5 restart
fi

%postun -n lui-harvester-tomcat5
if [ $1 = 0 ]; then
  rm -f %{_sysconfdir}/tomcat5/Catalina/localhost/harvester.xml
fi

%post -n lui-harvester-tomcat6
ln -sf %{_sysconfdir}/masterkey/harvester/harvester-context.xml %{_sysconfdir}/tomcat6/Catalina/localhost/harvester.xml

%postun -n lui-harvester-tomcat6
if [ $1 = 0 ]; then
  rm -f %{_sysconfdir}/tomcat6/Catalina/localhost/harvester.xml
fi

%post -n lui-harvester-admin-tomcat6
ln -sf %{_sysconfdir}/masterkey/harvester/harvester-context.xml %{_sysconfdir}/tomcat6/Catalina/localhost/harvester-admin.xml

%postun -n lui-harvester-admin-tomcat6
if [ $1 = 0 ]; then
  rm -f %{_sysconfdir}/tomcat6/Catalina/localhost/harvester-admin.xml
fi

%files
%defattr(-,root,root)
%{_datadir}/masterkey/harvester
#%config %{_sysconfdir}/masterkey/harvester/conf.d/harvester.properties
%attr(750,tomcat,tomcat) %dir %{_localstatedir}/log/masterkey/harvester

%files -n lui-harvester-admin
%defattr(-,root,root)
%{_datadir}/masterkey/harvester-admin
%attr(750,tomcat,tomcat) %dir %{_localstatedir}/log/masterkey/harvester-admin

%files -n lui-harvester-tomcat
%defattr(-,root,root)
%config %{_sysconfdir}/masterkey/harvester/harvester-context.xml

%files -n lui-harvester-admin-tomcat
%defattr(-,root,root)
%config %{_sysconfdir}/masterkey/harvester/harvester-admin-context.xml

%files -n lui-harvester-tomcat5
%files -n lui-harvester-admin-tomcat5
%files -n lui-harvester-tomcat6
%files -n lui-harvester-admin-tomcat6
