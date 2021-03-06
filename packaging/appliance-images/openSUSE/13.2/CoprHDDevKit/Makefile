#
# Copyright 2015-2016 EMC Corporation
# All Rights Reserved
#

##################################################
#  Setup
##################################################
WORKSPACE := $(shell dirname $(MAKEFILE_LIST))
NAME := CoprHDDevKit
VERSION := 3.6.0.0
JOB := 999
VENDOR := EMC
ISO_FILE := openSUSE-13.2-DVD-x86_64.iso
ISO_URL := http://download.opensuse.org/distribution/13.2/iso/$(ISO_FILE)
RAM := 8192
MAC := $(shell printf "00%02X%02X%02X%02X%02X" $$[RANDOM%256] $$[RANDOM%256] $$[RANDOM%256] $$[RANDOM%256] $$[RANDOM%256])

##################################################
#  Target files/folders
##################################################
USE_CACHE := $(shell [ -d /disks/adgbuild/OPENSUSE13.2/repo ] && echo true || echo false)
IMAGE_DIR := $(WORKSPACE)/image/$(JOB)
OUTPUT_DIR := $(WORKSPACE)/output/$(JOB)
BUILD_DIR := $(WORKSPACE)/build/$(NAME)-$(VERSION).$(JOB)
OPENSTACK_DIR := $(WORKSPACE)/openstack/$(JOB)
VMDK := $(OUTPUT_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmdk
OVF := $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).ovf
VMX := $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmx
TBZ := $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).tbz
BOX := $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).box
VHD := $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vhd
VBOX := $(BUILD_DIR)/$(NAME)-$(VERSION).$(JOB).vbox
QCOW2 := $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).qcow2
ZYPPER_PARAMS := --reposd-dir $(WORKSPACE)/zypper/reposd --cache-dir $(WORKSPACE)/zypper/cache --solv-cache-dir $(WORKSPACE)/zypper/solv --pkg-cache-dir $(WORKSPACE)/zypper/pkg

.DEFAULT: all
.PHONY: downloadISO downloadRPM createREPO ovf vmx tbz box vhd vbox qcow2 appliance devkit clean destroy
all: appliance

##################################################
#  Functions
##################################################
define targetLog
	@echo "###############################################";
	@echo "$(1)[$@] on $$( date )";
	@echo "###############################################";
endef

##################################################
#  Targets
##################################################
downloadISO:
	$(call targetLog,+)
	mkdir -p $(WORKSPACE)/iso/mount
	[ ! -f /disks/adgbuild/OPENSUSE13.2/$(ISO_FILE) ] || ln -fs /disks/adgbuild/OPENSUSE13.2/$(ISO_FILE) $(WORKSPACE)/iso/
	[ -s $(WORKSPACE)/iso/$(ISO_FILE) ] || wget $(ISO_URL) --continue --progress=bar --output-document=$(WORKSPACE)/iso/$(ISO_FILE)
	$(call targetLog,-)

downloadRPM:
	$(call targetLog,+)
ifeq ($(USE_CACHE),false)
	rm -fr $(WORKSPACE)/zypper/reposd
	mkdir -p $(WORKSPACE)/zypper/{reposd,cache,solv,pkg}
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-oss --no-gpgcheck http://download.opensuse.org/distribution/13.2/repo/oss/suse suse-13.2-oss
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-monitoring --no-gpgcheck http://download.opensuse.org/repositories/server:/monitoring/openSUSE_13.2 suse-13.2-monitoring
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-http --no-gpgcheck http://download.opensuse.org/repositories/server:/http/openSUSE_13.2 suse-13.2-http
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-seife --no-gpgcheck http://download.opensuse.org/repositories/home:/seife:/testing/openSUSE_13.2 suse-13.2-seife
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-python --no-gpgcheck http://download.opensuse.org/repositories/devel:/languages:/python/openSUSE_13.2 suse-13.2-python
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-network --no-gpgcheck http://download.opensuse.org/repositories/network:/utilities/openSUSE_13.2 suse-13.2-network
	zypper $(ZYPPER_PARAMS) addrepo --name suse-42.1-filesystems-ceph --no-gpgcheck http://download.opensuse.org/repositories/filesystems:/ceph/openSUSE_Leap_42.1 suse-42.1-filesystems-ceph
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-electronics --no-gpgcheck http://download.opensuse.org/repositories/electronics/openSUSE_13.2 suse-13.2-electronics
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-13.2-oss
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  2 suse-13.2-monitoring
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  2 suse-13.2-http
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  2 suse-13.2-seife
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  4 suse-13.2-python
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  4 suse-13.2-network
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  4 suse-42.1-filesystems-ceph
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  4 suse-13.2-electronics
	zypper $(ZYPPER_PARAMS) refresh
	zypper $(ZYPPER_PARAMS) download atop ca-certificates-cacert docker fbiterm fribidi GeoIP GeoIP-data java-1_8_0-openjdk-devel keepalived libGeoIP1 libserf-devel ndisc6 nginx openssh-fips perl-Tk python-cjson python-gpgme python-iniparse python-py python-requests python-setools setools-libs sipcalc sshpass strongswan strongswan-ipsec strongswan-libs0 sysstat boost-license1_58_0 libboost_system1_58_0 libboost_thread1_58_0 librados2 librbd1
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-oss-update --no-gpgcheck http://download.opensuse.org/repositories/openSUSE:/13.2:/Update/standard suse-13.2-oss-update
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  1 suse-13.2-oss-update
	zypper $(ZYPPER_PARAMS) refresh
	zypper $(ZYPPER_PARAMS) download libudev1 lvm2 udev grub2 grub2-x86_64-efi grub2-i386-pc kernel-default kernel-default-devel kernel-devel kernel-source kernel-macros virtualbox virtualbox-host-kmp-default
endif
	$(call targetLog,-)

createREPO: | downloadRPM
	$(call targetLog,+)
	rm -fr $(WORKSPACE)/zypper/repo
	mkdir -p $(WORKSPACE)/zypper/repo
	make --directory=$(WORKSPACE)/../configure-network
	cp -f $(WORKSPACE)/../configure-network/RPMS/noarch/* $(WORKSPACE)/zypper/repo/
ifeq ($(USE_CACHE),false)
	find $(WORKSPACE)/zypper/pkg -iname \*.rpm -exec cp -f {} $(WORKSPACE)/zypper/repo/ \;
endif
	createrepo $(WORKSPACE)/zypper/repo
	$(call targetLog,-)

clean:
	@rm -fr $(WORKSPACE)/build/$(NAME)-$(VERSION).$(JOB)
	@rm -fr $(WORKSPACE)/image/$(JOB)
	@rm -fr $(WORKSPACE)/output
	@rm -fr $(WORKSPACE)/zypper/repo
	@rm -fr $(WORKSPACE)/zypper/reposd

destroy:
	@rm -fr $(WORKSPACE)/iso
	@rm -fr $(WORKSPACE)/build
	@rm -fr $(WORKSPACE)/image
	@rm -fr $(WORKSPACE)/output
	@rm -fr $(WORKSPACE)/zypper

appliance: | ovf vmx box qcow2 tbz

ovf: | $(OVF)
	$(call targetLog,+)
	chmod a+r $(BUILD_DIR)/*
	$(call targetLog,-)

vmx: | $(VMX)
	$(call targetLog,+)
	chmod a+r $(BUILD_DIR)/*
	$(call targetLog,-)

tbz: | $(TBZ)
	$(call targetLog,+)
	chmod a+r $(BUILD_DIR)/*
	$(call targetLog,-)

box: | $(BOX)
	$(call targetLog,+)
	chmod a+r $(BUILD_DIR)/*
	$(call targetLog,-)

vhd: | $(VHD)
	$(call targetLog,+)
	chmod a+r $(BUILD_DIR)/*
	$(call targetLog,-)

vbox: | $(VBOX)
	$(call targetLog,+)
	chmod a+r $(BUILD_DIR)/*
	$(call targetLog,-)

qcow2: | $(QCOW2)
	$(call targetLog,+)
	chmod a+r $(BUILD_DIR)/*
	$(call targetLog,-)

devkit:
	$(call targetLog,+)
	bash $(WORKSPACE)/configure.sh installRepositories
	bash $(WORKSPACE)/configure.sh installPackages
	bash $(WORKSPACE)/configure.sh installDockerStorage
	bash $(WORKSPACE)/configure.sh installJava
	bash $(WORKSPACE)/configure.sh installStorageOS
	bash $(WORKSPACE)/configure.sh installNetworkConfigurationFile
	bash $(WORKSPACE)/configure.sh installXorg
	$(call targetLog,-)

$(VMDK): | downloadISO createREPO
	$(call targetLog,+)
	bash $(WORKSPACE)/patch.sh
	mkdir -p $(WORKSPACE)/templates/root/opt/ADG/conf
	cp -f $(WORKSPACE)/templates/config.orig.xml $(WORKSPACE)/templates/config.xml
ifeq ($(USE_CACHE),true)
	sed -i "/__REPO__/ i\        <source path=\"/disks/adgbuild/OPENSUSE13.2/repo\"/>\n    </repository>\n    <repository type=\"rpm-md\" priority=\"1\">" $(WORKSPACE)/templates/config.xml
endif
	cp -f $(WORKSPACE)/configure.sh $(WORKSPACE)/templates/root/opt/ADG/conf/
	chmod u+x $(WORKSPACE)/templates/root/opt/ADG/conf/configure.sh
	sed -i "s|__NAME__|$(NAME)|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__ISO__|$(WORKSPACE)/iso/mount|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__REPO__|$(WORKSPACE)/zypper/repo|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__VERSION__|$(VERSION).$(JOB)|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__UUID__|$$RANDOM|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__RAM__|$(RAM)|g" $(WORKSPACE)/templates/config.xml
	-! mountpoint -q $(IMAGE_DIR)/var/cache/kiwi || umount $(IMAGE_DIR)/var/cache/kiwi
	-mountpoint -q $(WORKSPACE)/iso/mount || mount $(WORKSPACE)/iso/$(ISO_FILE) $(WORKSPACE)/iso/mount
	mkdir -p $(WORKSPACE)/build/$(NAME)-$(VERSION).$(JOB)
	mkdir -p $(WORKSPACE)/image
	mkdir -p $(WORKSPACE)/build
	mkdir -p $(WORKSPACE)/output
	kiwi --verbose 3 --yes --prepare $(WORKSPACE)/templates --add-profile appliance --force-new-root --root $(IMAGE_DIR)
	kiwi --verbose 3 --yes --create $(IMAGE_DIR) --add-profile appliance --type vmx --destdir $(OUTPUT_DIR)
	-umount $(WORKSPACE)/iso/mount
	$(call targetLog,-)

$(OVF): | $(VMDK)
	$(call targetLog,+)
	ln -fs $(NAME).x86_64-$(VERSION).$(JOB).vmdk $(OUTPUT_DIR)/disk.vmdk
	VBoxManage createvm --name "$(NAME)-$(VERSION).$(JOB)" --basefolder $(OUTPUT_DIR) --register
	VBoxManage modifyvm "$(NAME)-$(VERSION).$(JOB)" --memory $(RAM) --cpus 1 --acpi on --boot1 dvd --nic1 bridged --bridgeadapter1 eth0 --ostype Linux_64
	VBoxManage storagectl "$(NAME)-$(VERSION).$(JOB)" --name "IDE Controller" --add ide
	VBoxManage storageattach "$(NAME)-$(VERSION).$(JOB)" --storagectl "IDE Controller" --port 0 --device 0 --type hdd --medium $(OUTPUT_DIR)/disk.vmdk
	VBoxManage export "$(NAME)-$(VERSION).$(JOB)" --vsys 0 --product $(NAME) --version $(VERSION).$(JOB) --vendor $(VENDOR) --output $(OVF)
	VBoxManage unregistervm "$(NAME)-$(VERSION).$(JOB)" --delete
	bash $(WORKSPACE)/configure.sh updateOVF $(OVF)
	$(call targetLog,-)

$(VMX): | $(VMDK)
	$(call targetLog,+)
	[ -f $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmdk ] || cp $(VMDK) $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmdk
	cp $(OUTPUT_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmx $(VMX)
	$(call targetLog,-)

$(TBZ): | $(VMDK)
	$(call targetLog,+)
	echo '' > $(IMAGE_DIR)/etc/fstab
	echo 'LOADER_TYPE="none"' > $(IMAGE_DIR)/etc/sysconfig/bootloader
	echo 'LOADER_LOCATION="none"' >> $(IMAGE_DIR)/etc/sysconfig/bootloader
	echo 'ROOTFS_FSCK="0"' > $(IMAGE_DIR)/etc/sysconfig/boot
	echo 'ROOTFS_BLKDEV="/dev/null"' >> $(IMAGE_DIR)/etc/sysconfig/boot
	echo 'console"' >> $(IMAGE_DIR)/etc/securetty
	rm -fr $(IMAGE_DIR)/run/*
	rm -fr $(IMAGE_DIR)/.kconfig
	rm -fr $(IMAGE_DIR)/.profile
	rm -fr $(IMAGE_DIR)/kiwi-hooks
	rm -fr $(IMAGE_DIR)/image
	rm -fr $(IMAGE_DIR)/Swap
	sed -i s/"udev_sync = 1"/"udev_sync = 0"/g $(IMAGE_DIR)/etc/lvm/lvm.conf
	sed -i s/"udev_rules = 1"/"udev_rules = 0"/g $(IMAGE_DIR)/etc/lvm/lvm.conf
	[ ! -x /usr/bin/pigz ] || tar -c -f $(TBZ) -C $(IMAGE_DIR) -I pigz .
	[ -x /usr/bin/pigz ] || tar -c -f $(TBZ) -C $(IMAGE_DIR) -J .
	$(call targetLog,-)

$(BOX): | $(VMDK)
	$(call targetLog,+)
	chroot $(IMAGE_DIR) /opt/ADG/conf/configure.sh installVagrant
	-! mountpoint -q $(IMAGE_DIR)/var/cache/kiwi || umount $(IMAGE_DIR)/var/cache/kiwi
	-mountpoint -q $(WORKSPACE)/iso/mount || mount $(WORKSPACE)/iso/$(ISO_FILE) $(WORKSPACE)/iso/mount
	kiwi --verbose 3 --yes --create $(IMAGE_DIR) --add-profile appliance --type vmx --destdir $(OUTPUT_DIR)/box
	-umount $(WORKSPACE)/iso/mount
	chroot $(IMAGE_DIR) userdel -r vagrant
	chroot $(IMAGE_DIR) groupdel vagrant
	sed -i "/^vagrant/d" $(IMAGE_DIR)/etc/sudoers
	VBoxManage createvm --name "$(NAME)-$(VERSION).$(JOB)" --basefolder $(OUTPUT_DIR)/box --register
	VBoxManage modifyvm "$(NAME)-$(VERSION).$(JOB)" --memory $(RAM) --cpus 1 --acpi on --boot1 dvd --nic1 bridged --bridgeadapter1 eth0 --ostype Linux_64
	VBoxManage storagectl "$(NAME)-$(VERSION).$(JOB)" --name "IDE Controller" --add ide
	VBoxManage storageattach "$(NAME)-$(VERSION).$(JOB)" --storagectl "IDE Controller" --port 0 --device 0 --type hdd --medium $(OUTPUT_DIR)/box/$(NAME).x86_64-$(VERSION).$(JOB).vmdk
	VBoxManage export "$(NAME)-$(VERSION).$(JOB)" --vsys 0 --product $(NAME) --version $(VERSION).$(JOB) --vendor $(VENDOR) --output $(OUTPUT_DIR)/box/box.ovf
	VBoxManage unregistervm "$(NAME)-$(VERSION).$(JOB)" --delete
	bash $(WORKSPACE)/configure.sh updateOVF $(OUTPUT_DIR)/box/box.ovf
	@echo '{ "provider" : "virtualbox", "format" : "vmdk" }' > $(OUTPUT_DIR)/box/metadata.json
	tar -czf $(BOX) -C $(OUTPUT_DIR)/box box.ovf metadata.json box-disk1.vmdk
	cp -fr $(WORKSPACE)/templates/Vagrantfile.orig $(BUILD_DIR)/Vagrantfile
	sed -i "s|__NAME__|$(NAME)|g" $(BUILD_DIR)/Vagrantfile
	sed -i "s|__VERSION__|$(VERSION).$(JOB)|g" $(BUILD_DIR)/Vagrantfile
	sed -i "s|__MAC__|$(MAC)|g" $(BUILD_DIR)/Vagrantfile
	sed -i "s|__RAM__|$(RAM)|g" $(BUILD_DIR)/Vagrantfile
	$(call targetLog,-)

$(VHD): | $(VMDK)
	$(call targetLog,+)
	VBoxManage clonehd $(VMDK) $(VHD) -format VHD
	$(call targetLog,-)

$(VBOX): | $(VMDK)
	$(call targetLog,+)
	[ -f $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmdk ] || cp $(VMDK) $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmdk
	VBoxManage createvm --name "$(NAME)-$(VERSION).$(JOB)" --basefolder $(WORKSPACE)/build  --register
	VBoxManage modifyvm "$(NAME)-$(VERSION).$(JOB)" --memory $(RAM) --cpus 1 --acpi on --boot1 dvd --nic1 bridged --bridgeadapter1 eth0 --ostype Linux_64
	VBoxManage storagectl "$(NAME)-$(VERSION).$(JOB)" --name "IDE Controller" --add ide
	VBoxManage storageattach "$(NAME)-$(VERSION).$(JOB)" --storagectl "IDE Controller" --port 0 --device 0 --type hdd --medium $(BUILD_DIR)/$(NAME).x86_64-$(VERSION).$(JOB).vmdk
	VBoxManage unregistervm "$(NAME)-$(VERSION).$(JOB)"
	rm -fr "$${HOME}/.config/VirtualBox/$(OUTPUT_DIR)
	sed -i 's|$(BUILD_DIR)/||g' $(VBOX)
	rm $(VBOX)-prev
	$(call targetLog,-)

$(QCOW2): | downloadISO createREPO
	$(call targetLog,+)
	bash $(WORKSPACE)/patch.sh
	mkdir -p $(WORKSPACE)/templates/root/opt/ADG/conf
	cp -f $(WORKSPACE)/templates/config.orig.xml $(WORKSPACE)/templates/config.xml
ifeq ($(USE_CACHE),true)
	sed -i "/__REPO__/ i\        <source path=\"/disks/adgbuild/OPENSUSE13.2/repo\"/>\n    </repository>\n    <repository type=\"rpm-md\" priority=\"1\">" $(WORKSPACE)/templates/config.xml
endif
	cp -f $(WORKSPACE)/configure.sh $(WORKSPACE)/templates/root/opt/ADG/conf/
	chmod u+x $(WORKSPACE)/templates/root/opt/ADG/conf/configure.sh
	sed -i "s|__NAME__|$(NAME)|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__ISO__|$(WORKSPACE)/iso/mount|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__REPO__|$(WORKSPACE)/zypper/repo|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__VERSION__|$(VERSION).$(JOB)|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__UUID__|$$RANDOM|g" $(WORKSPACE)/templates/config.xml
	sed -i "s|__RAM__|$(RAM)|g" $(WORKSPACE)/templates/config.xml
	-! mountpoint -q $(OPENSTACK_DIR)/image/var/cache/kiwi || umount $(OPENSTACK_DIR)/image/var/cache/kiwi
	-mountpoint -q $(WORKSPACE)/iso/mount || mount $(WORKSPACE)/iso/$(ISO_FILE) $(WORKSPACE)/iso/mount
	mkdir -p $(WORKSPACE)/build/$(NAME)-$(VERSION).$(JOB)
	mkdir -p $(OPENSTACK_DIR)/image
	mkdir -p $(OPENSTACK_DIR)/output
	kiwi --verbose 3 --yes --prepare $(WORKSPACE)/templates --add-profile openstack --force-new-root --root $(OPENSTACK_DIR)/image
	touch $(OPENSTACK_DIR)/image/var/lib/docker-storage.btrfs
	kiwi --verbose 3 --yes --create $(OPENSTACK_DIR)/image --add-profile openstack --type vmx --destdir $(OPENSTACK_DIR)/output
	-umount $(WORKSPACE)/iso/mount
	qemu-img convert -O qcow2 $(OPENSTACK_DIR)/output/$(NAME).x86_64-$(VERSION).$(JOB).raw $(QCOW2)
	rm -fr $(OPENSTACK_DIR)
	$(call targetLog,-)
