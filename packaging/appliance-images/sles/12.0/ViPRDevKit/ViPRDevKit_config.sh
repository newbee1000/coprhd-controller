
#======================================
# Functions...
#--------------------------------------
test -f /.kconfig && . /.kconfig
test -f /.profile && . /.profile

#adg_DevKitConfig

########################################################
#  Fix BuildForge Agent Issue                          #
########################################################
if [ -f "/etc/pam.d/bfagent" ]; then

  mv "/etc/pam.d/bfagent" "/etc/pam.d/bfagent-original"
  echo "#%PAM-1.0
auth       required       pam_unix.so
password   required       pam_unix.so
session    required       pam_unix.so
session    required       pam_loginuid.so
account    required       pam_permit.so" > "/etc/pam.d/bfagent"

  else
  echo "NO pam File for bfagent";
  if [ "`rpm -qa|grep bfagent`" ]; then
    echo "Possible bfagent Installation Issue";
  else
    echo "bfagent Installation UNAVAILABLE";
  fi
fi

#######################################################
#            Start xinetd at BooT                     #
#######################################################
systemctl enable xinetd
if [ -f "/etc/apache2/httpd.conf" ]; then

  cp "/etc/apache2/httpd.conf" "/etc/apache2/httpd.conf-original"
  echo '<Directory /opt/downloads>
           Require all granted
           Options Indexes FollowSymLinks
           EnableSendfile off
           AddIcon /icons/sphere2.gif ovf
           AddIcon /icons/diskimg.png vmdk
           AddIcon /icons/screw1.gif rpm
           AddIcon /icons/pie6.gif iso
           IndexOptions -HTMLTable
           Require all granted
       </Directory>
Alias /downloads "/opt/downloads"' >> "/etc/apache2/httpd.conf"

   systemctl enable apache2

else

  echo "Apache config file NOT Present";
  if [ "`rpm -qa|grep apache2`" ]; then
    echo "Possible apache2 Installation Issue";
  else
    echo "apache2 Installation UNAVAILABLE";
  fi
fi
systemctl enable sshd

######################################################################################################
# With apache 2.4, some changes have been introduced that affect apache's
# access control scheme. Refer /usr/share/doc/packages/apache2/README-access_compat.txt
# This is a workaround for the mod_perl.conf file of apache2-mod_perl package to fix the apache issue.
# Have this removed once it is fixed with apache2-mod_perl
######################################################################################################
modPerl='/etc/apache2/conf.d/mod_perl.conf';
if [ -f "$modPerl" ]; then
  cp "$modPerl" "/etc/apache2/conf.d/mod_perl.conf-original"
  sed -i 's/Order allow\,deny/\#Order allow\,deny/g' $modPerl
  sed -i 's/Deny from all/\#Deny from all/g' $modPerl
  sed -i '/Deny from all/a\    \Require all granted' $modPerl 
fi

adg_zypperRepos
adg_DevKitHomeEndKeys

#fix_autofs
# #######################################################
# #            Configure AutoFS NIS Mount               #
# #######################################################
# mkdir /disks
# cat > /etc/auto.master <<AUTOMASTER
# /usr/local -null
# +auto.master
# /net    /etc/auto.net
# /emc    auto.emc        -nosuid,rw,hard,intr
# AUTOMASTER
# echo "domain os server 10.247.17.11" > /etc/yp.conf
# echo "os" > /etc/defaultdomain
# chkconfig autofs on
# chkconfig ypbind on

#######################################################
#            Change volume group name                 #
#######################################################
if [ ! -f /opt/ADG/firstboot/scripts/45vgrename.sh ]; then
  cat > /opt/ADG/firstboot/scripts/45vgrename.sh <<'VGRENAME'
#!/bin/bash
prodVG="DevKitVG"
currentVG="systemVG"
vgrename $currentVG $prodVG
echo "Removing old mounts point from /etc/fstab"
sed -i "/LVRoot/d" /etc/fstab
vgRoot="/dev/$prodVG/LVRoot / ext3 defaults 1 1"
echo "Adding new mount point $vgRoot to /etc/fstab"
echo $vgRoot >> /etc/fstab
sed -i "s|/dev/$currentVG/LVRoot|/dev/$prodVG/LVRoot|g" /etc/sysconfig/bootloader
sed -i "s|$currentVG|$prodVG|g" /boot/grub2/grub.cfg
sed -i "s|$currentVG|$prodVG|g" /boot/grub2-efi/grub.cfg
sed -i "s|$currentVG|$prodVG|g" /boot/efi/EFI/BOOT/grub.cfg
sed -i "s|$currentVG|$prodVG|g" /etc/default/grub
echo "Updating /boot/initrd$(uname -r)"
mkinitrd -f /boot/initrd-$(uname -r) $(uname -r)
VGRENAME
  chmod 700 /opt/ADG/firstboot/scripts/45vgrename.sh
fi

#######################################################
#            Change swap /etc/fstab                   #
#######################################################

#######################################################
# Note: 50SwapConfig.sh is installed by               #
# adg-core-conf-1-2.x86_64.rpm but this package       #
# conflicts with                                      #
# VMwareTools-9.4.0.1280544-2.x86_64.rpm              #
#######################################################

if [ ! -f /opt/ADG/firstboot/scripts/50SwapConfig.sh ]; then
  cat > /opt/ADG/firstboot/scripts/50SwapConfig.sh <<'VGSWAP'
#!/bin/bash
echo "Removing old mounts point from /etc/fstab"
sed -i "/LVswap/d" /etc/fstab

vgVar=`ls /dev/mapper/*-LVswap|cut -d"-" -f1|cut -d"/" -f4`
swapVar="/dev/mapper/$vgVar-LVswap"

echo "Adding new mount point $swapVar to /etc/fstab"
echo "$swapVar none swap defaults 0 0" >> /etc/fstab

umount -f /swap
mkswap -f $swapVar
swapon $swapVar
VGSWAP
  chmod 700 /opt/ADG/firstboot/scripts/50SwapConfig.sh
fi

# #######################################################
# #            Configure SLES Media                     #
# #            HACK - ADG Conf service                  #
# #            HACK - visudo (/etc/sudoers)             #
# #            HACK - createAppliance alias             #
# #######################################################
# mkdir -p /share/ovf_release/autoDeploy
# mkdir -p /slesmedia/12-SLES-DVD1
# mkdir -p /rhelmedia/RHEL-6.5-DVD1
# mkdir -p /rhelmedia/RHEL-7.0-DVD1
# mkdir -p /oraclemedia/OLR6U5S
# cat > /etc/init.d/adgconf <<ADGCONF
# #! /bin/sh
# ### BEGIN INIT INFO
# # Provides:        adgconf
# # Required-Start:  autofs ypbind
# # Required-Stop:
# # Should-Stop:
# # Default-Start:   3 5
# # Default-Stop:    0 1 2 6
# # Description:     Configure base DevKit
# ### END INIT INFO
# 
# cat >> /etc/fstab <<APPENDFSTAB
# lglaf020.lss.emc.com:/share/ovf_release/autoDeploy /share/ovf_release/autoDeploy nfs   defaults    0 0
# /disks/adgbuild/SLES12/SLE-12-Server-DVD-x86_64-GM-DVD1.iso /slesmedia/12-SLES-DVD1 iso9660 noauto,loop,ro 0 0
# /disks/adgbuild/RHEL/6.5/rhel-server-6.5-x86_64-dvd.iso /rhelmedia/RHEL-6.5-DVD1 iso9660 noauto,loop,ro 0 0
# /disks/adgbuild/RHEL/7.0/rhel-server-7.0-x86_64-dvd.iso /rhelmedia/RHEL-7.0-DVD1 iso9660 noauto,loop,ro 0 0
# /disks/adgbuild/ORACLELINUX/6.5/OracleLinux-R6-U5-Server-x86_64-dvd.iso /oraclemedia/OLR6U5S iso9660 noauto,loop,ro 0 0
# APPENDFSTAB
# /bin/mount -a
# 
# hostname -F /etc/HOSTNAME
# insserv -r adgconf
# rm -f /etc/init.d/adgconf
# ADGCONF
# chmod +x /etc/init.d/adgconf
# insserv -d adgconf
# 
# cat > /etc/init.d/adgmount <<ADGMOUNT
# #! /bin/sh
# ### BEGIN INIT INFO
# # Provides:        adgmount
# # Required-Start:  autofs ypbind
# # Required-Stop:
# # Should-Stop:
# # Default-Start:   3 5
# # Default-Stop:    0 1 2 6
# # Description:     Remount remote fs after autofs loads
# ### END INIT INFO
# 
# . /etc/rc.status
# 
# /bin/mount -a
# mount -t iso9660 -o loop /disks/adgbuild/SLES12/SLE-12-Server-DVD-x86_64-GM-DVD1.iso /slesmedia/12-SLES-DVD1
# mount -t iso9660 -o loop /disks/adgbuild/RHEL/6.5/rhel-server-6.5-x86_64-dvd.iso /rhelmedia/RHEL-6.5-DVD1
# mount -t iso9660 -o loop /disks/adgbuild/RHEL/7.0/rhel-server-7.0-x86_64-dvd.iso /rhelmedia/RHEL-7.0-DVD1
# mount -t iso9660 -o loop /disks/adgbuild/ORACLELINUX/6.5/OracleLinux-R6-U5-Server-x86_64-dvd.iso /oraclemedia/OLR6U5S
# rc_status -v
# ADGMOUNT
# chmod +x /etc/init.d/adgmount
# insserv -d adgmount

#fix_sudoers
# cat >> /etc/sudoers <<VISUDO
# ALL ALL=NOPASSWD:/usr/sbin/kiwi *
# ALL ALL=NOPASSWD:/bin/rm [-]* /opt/ADG/createAppliance/jobs/[A-z]*
# VISUDO
# 
# cat >> /etc/profile <<ALIAS
# alias createAppliance="/usr/bin/perl /opt/ADG/createAppliance/bin/createAppliance.pl"
# ALIAS

#adg_KIWIMods
# #######################################################
# # KIWI Modifications                                  #
# #######################################################
# kiwiPath="/usr/share/kiwi/modules"
# 
# #######################################################
# # Changes for 3 digits to 5 digits version
# FileValidator="$kiwiPath/KIWIXMLValidator.pm"
# if [ -f $FileValidator ]; then
#  echo "Modifying to fix version check for $FileValidator"
#  sed -i."bak" 's/\($version !~ \/^\\d+\\.\\d+\\.\\d+$\/\)/\($version !~ \/^\\d+\\.\\d+\\.\\d+$\/\) \&\& \($version !~ \/^\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+$\/\)/g' $FileValidator
# fi
# 
# FilePreference="$kiwiPath/KIWIXMLPreferenceData.pm"
# if [ -f $FilePreference ]; then
#  echo "Modifying to fix version check for $FilePreference"
#  sed -i."bak" 's/\( $ver !~ \/^\\d+?\\.\\d+?\\.\\d+?$\/smx \)/\( $ver !~ \/^\\d+?\\.\\d+?\\.\\d+?$\/smx \) \&\& \( $ver !~ \/^\\d+?\\.\\d+?\\.\\d+?\\.\\d+?\\.\\d+?$\/smx \)/g' $FilePreference
# fi
# 
# 
# #######################################################
# # Modify zypper cache behaviour
# sed -i "s|my @cache = (\"/var/cache/kiwi\");|my \$packageCache = (dirname \$root) . \"/packages\";\n\tmy @cache = (\"/var/cache/kiwi\", \$packageCache);|" $kiwiPath/KIWIRoot.pm
# 
# linenum=`grep -n "# KIWI Modules" $kiwiPath/KIWIRoot.pm|cut -d ":" -f1`
# let newnum=$linenum-1
# sed -i "$newnum i use File::Basename;" $kiwiPath/KIWIRoot.pm
# 
# linenum=`grep -n "Store zypper command parameters" $kiwiPath/KIWIManagerZypper.pm|cut -d ":" -f1`
# let newnum=$linenum+2
# sed -i "$newnum i my \$packageCache = (dirname \$root) . \"/packages\";" $kiwiPath/KIWIManagerZypper.pm
# sed -i "s|'--pkg-cache-dir /var/cache/kiwi/packages'|\"--pkg-cache-dir \$packageCache\"|g" $kiwiPath/KIWIManagerZypper.pm
# sed -i "s|my @zopts = ();|my @zopts = (\"--keep-packages\");|" $kiwiPath/KIWIManagerZypper.pm
# sed -i -e '/keep packages on remote repos/,+6d' $kiwiPath/KIWIManagerZypper.pm

#######################################################
#Configure Firewall to allow ssh
#######################################################
sed -i 's/FW_SERVICES_EXT_TCP=""/FW_SERVICES_EXT_TCP="ssh"/' /etc/sysconfig/SuSEfirewall2
echo "Firewall configuration completed.."


# Changed the permission on /var/log
chmod 1777 /var/log

# Fixed the problem with accessing webserver on /workspace
a2enmod mod_access_compat

#######################################################
#           Link to .prod file needed by zypper       #
#######################################################
ln -s /etc/products.d/SLES.prod /etc/products.d/baseproduct

#######################################################
# Starting to call funtions from configurations.sh    #
#######################################################

echo "Running common configurations for ViPRDevKit"
fix_workspace
adg_bootClock
adg_DevKitConfig
adg_DevKitHomeEndKeys
adg_kernelMessage
adg_KIWIMods
adg_motd
adg_ovftoolInstall
adg_zypperRepos
fix_adg_security
fix_devkit_root_umask
fix_apache
fix_atop
fix_audit
fix_autofs
fix_bootfs
fix_cacerts
fix_connectemc
fix_connectemc_data
fix_data
fix_devkit_firewall
fix_ifcfg
fix_kiwi_storage_network
fix_network_config
fix_openssl_links
fix_patches
fix_permission
fix_rootdir
fix_rsyslog
fix_ssh
fix_sshd_config
#fix_sudoers
fix_symssl
fix_users
fix_workspace
vipr_baseServices
vipr_DevKitConfig
vipr_devKitServices
vipr_fix_svcuser_ssh
vipr_removeUsers
vipr_removeGroups
vipr_svcuser
fix_alias
fix_devkit_storageos_key
fix_devkit_storageos_cert
fix_devkit_readme
fix_enable_java_sslv3
vipr_fix_add_strongswan


#######################################################
# Ending call funtions from configurations.sh      #
#######################################################

# Run as default for SSL certificate
git config --global http.sslVerify false

# Disable services wickedd-dhcp4, wickedd-dhcp6:
systemctl disable wickedd-dhcp4
systemctl disable wickedd-dhcp6

# Removed the floppy 
echo "blacklist floppy" > /etc/modprobe.d/blacklist-floppy.conf

echo "Start: Creating/Change permission the storageos directory"
cat > /usr/lib/systemd/system/varRunStorageos.service <<SERVICE
[Unit]
Description=Ensure /var/run/storageos exist after boot

[Service]
Type=oneshot
ExecStart=/usr/bin/mkdir -p /var/run/storageos
ExecStart=/usr/bin/chmod 0755 /var/run/storageos
ExecStart=/usr/bin/chown -Rh storageos:storageos /var/run/storageos
ExecStart=/usr/bin/chown -h storageos:storageos /etc/config.defaults
ExecStart=/usr/bin/systemctl disable varRunStorageos.service
ExecStart=/usr/bin/rm /usr/lib/systemd/system/varRunStorageos.service

[Install]
WantedBy=multi-user.target
SERVICE
systemctl enable varRunStorageos.service
chown -Rh storageos:storageos /opt/storageos
chown -Rh storageos:storageos /data/db
chown -Rh storageos:storageos /data/geodb
chown -Rh storageos:storageos /data/zk


# Umount kernel filesystems
#--------------------------------------
baseCleanMount

# Add more specific alias on ViPRDevKit:
cat  >> /etc/bash.bashrc.local <<DEVKIT_ALIAS
alias installeclipse='zypper --no-gpg-checks install xorg-x11-Xvnc libcairo2 xauth libgtk-2_0-0 xkbcomp'
alias installintellij='zypper --no-gpg-checks install xorg-x11-Xvnc xauth libgtk-2_0-0 xkbcomp'
alias startvnc='vncserver; xrdp'
alias stopvnc='vncserver -kill :1; xrdp -kill'   
DEVKIT_ALIAS

# Mount all the iso sles12 DVD:
mkdir -p /disks/adgbuild
mount -o nolock nasl02.lss.emc.com:/mnt_0231/imgengrep/adgbuild /disks/adgbuild
mkdir -p /slesmedia/12-SLES-DVD1
mount /disks/adgbuild/SLES12/SLE-12-Server-DVD-x86_64-GM-DVD1.iso /slesmedia/12-SLES-DVD1

exit 0

#######################################################
#                     Done                            #
#######################################################
