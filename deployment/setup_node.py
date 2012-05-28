#!/usr/bin/env python
from __future__ import print_function

import sys
import os
from ConfigParser import RawConfigParser


STANBOL_FOLDER = 'sling'

NUXEO_VHOST = """\
<VirtualHost _default_:80>

    CustomLog /var/log/apache2/nuxeo_access.log combined
    ErrorLog /var/log/apache2/nuxeo_error.log

    DocumentRoot /var/www

    ProxyRequests Off
    <Proxy *>
        Order allow,deny
        Allow from all
    </Proxy>

    RewriteEngine On
    RewriteRule ^/$ /nuxeo/ [R,L]
    RewriteRule ^/nuxeo$ /nuxeo/ [R,L]

    ProxyPass        /nuxeo/ http://localhost:8080/nuxeo/
    ProxyPassReverse /nuxeo/ http://localhost:8080/nuxeo/
    ProxyPreserveHost On

    # WSS
    ProxyPass        /_vti_bin/     http://localhost:8080/_vti_bin/
    ProxyPass        /_vti_inf.html http://localhost:8080/_vti_inf.html
    ProxyPassReverse /_vti_bin/     http://localhost:8080/_vti_bin/
    ProxyPassReverse /_vti_inf.html http://localhost:8080/_vti_inf.html

</VirtualHost>
"""


def cmd(cmd):
    """Fail early to make it easier to troubleshoot"""
    code = os.system(cmd)
    if code != 0:
        raise RuntimeError("Error executing: " + cmd)


def pflush(*args, **kwargs):
    """Flush stdout for making Jenkins able to monitor the progress"""
    print(*args, **kwargs)
    sys.stdout.flush()


def debconfselect(pkg, param, value):
    cmd("echo %s %s select %s | debconf-set-selections" % (pkg, param, value))


def check_install_nuxeo():
    """Check that Nuxeo is installed from the latest datebased release"""

    # Ensure the datebased release repo is configured and up to date
    with open('/etc/apt/sources.list', 'rb') as f:
        sources = f.readlines()
        databased_sources = [s for s in sources
                             if (not s.strip().startswith('#')
                                 and "datebased" in s
                                 and "apt.nuxeo.org" in s)]
    if not databased_sources:
        cmd('apt-add-repository '
                    '"deb http://apt.nuxeo.org/ natty datebased"')
        cmd("wget -O- http://apt.nuxeo.org/nuxeo.key "
                    "| apt-key add -")
    cmd("apt-get update && apt-get upgrade -y")

    # Pre-accept Sun Java license & set Nuxeo options
    debconfselect("sun-java6-jdk", "shared/accepted-sun-dlj-v1-1", "true")
    debconfselect("sun-java6-jre", "shared/accepted-sun-dlj-v1-1", "true")
    debconfselect("nuxeo", "nuxeo/bind-address", "127.0.0.1")
    debconfselect("nuxeo", "nuxeo/http-port", "8080")
    debconfselect("nuxeo", "nuxeo/database", "Autoconfigure PostgreSQL")

    # Install or upgrade Nuxeo
    cmd("export DEBIAN_FRONTEND=noninteractive; "
                "apt-get install -y nuxeo")


def check_install_vhost():
    cmd("apt-get install -y apache2")
    filename = '/etc/apache2/sites-available/nuxeo'
    if not os.path.exists(filename):
        with open(filename, 'wb') as f:
            f.write(NUXEO_VHOST)

    cmd("a2enmod proxy proxy_http rewrite")
    cmd("a2dissite default")
    cmd("a2ensite nuxeo")
    cmd("apache2ctl -k graceful")


def deploy_stanbol(stanbol_launcher_jar):
    """Delete previous stanbol install and redeploy from launcher"""

    pflush("Shutting down and deleting previous Stanbol server (if any).")
    cmd('cp stanbol_init.sh /etc/init.d/stanbol')
    cmd('cp stanbol_env.sh /etc/default/stanbol')
    cmd('update-rc.d stanbol defaults')
    cmd('service stanbol stop')
    cmd('rm -rf sling')
    cmd('rm -rf stanbol-launcher.jar')

    pflush("Launching new updated Stanbol server")
    cmd('ln -s %s stanbol-launcher.jar' % stanbol_launcher_jar)
    cmd('service stanbol start')


if __name__ == "__main__":
    check_install_nuxeo()
    check_install_vhost()
    deploy_stanbol(sys.argv[1])
