#!/usr/bin/env python
from __future__ import print_function

import sys
import os


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


def cmd_or_fail(cmd):
    """Fail early to make it easier to troubleshoot"""
    code = os.system(cmd)
    if code != 0:
        raise RuntimeError("Error executing: " + cmd)


def pflush(*args, **kwargs):
    """Flush stdout for making Jenkins able to monitor the progress"""
    print(*args, **kwargs)
    sys.stdout.flush()


def debconfselect(pkg, param, value):
    cmd = "echo %s %s select %s | debconf-set-selections" % (
        pkg, param, value)
    cmd_or_fail(cmd)


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
        cmd_or_fail('apt-add-repository '
                    '"deb http://apt.nuxeo.org/ natty datebased"')
        cmd_or_fail("wget -O- http://apt.nuxeo.org/nuxeo.key "
                    "| apt-key add -")
    cmd_or_fail("apt-get update && apt-get upgrade -y")

    # Pre-accept Sun Java license & set Nuxeo options
    debconfselect("sun-java6-jdk", "shared/accepted-sun-dlj-v1-1", "true")
    debconfselect("sun-java6-jre", "shared/accepted-sun-dlj-v1-1", "true")
    debconfselect("nuxeo", "nuxeo/bind-address", "127.0.0.1")
    debconfselect("nuxeo", "nuxeo/http-port", "8080")
    debconfselect("nuxeo", "nuxeo/database", "Autoconfigure PostgreSQL")

    # Install or upgrade Nuxeo
    cmd_or_fail("export DEBIAN_FRONTEND=noninteractive; "
                "apt-get install -y nuxeo")


def check_install_vhost():
    cmd_or_fail("apt-get install -y apache2")
    filename = '/etc/apache2/sites-available/nuxeo'
    if not os.path.exists(filename):
        with open(filename, 'wb') as f:
            f.write(NUXEO_VHOST)

    cmd_or_fail("a2enmod proxy proxy_http rewrite")
    cmd_or_fail("a2dissite default")
    cmd_or_fail("a2ensite nuxeo")
    cmd_or_fail("apache2ctl -k graceful")


if __name__ == "__main__":
    check_install_nuxeo()
    check_install_vhost()
