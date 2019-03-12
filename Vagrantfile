# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  # Target platform is Debian/jessie
  config.vm.box = "debian/contrib-jessie64"

  # Set up a forwarded port for testing
  config.vm.network "forwarded_port", guest: 8080, host: 8080
  config.vm.network "forwarded_port", guest: 8983, host: 8983

  config.vm.provision "ansible" do |ansible|
    ansible.playbook = "dev-deploy.yml"

  config.vm.synced_folder "./shared", "/vagrant_shared"
  end
end