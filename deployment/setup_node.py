#!/usr/bin/env python
from __future__ import print_function

import sys
import os
import ConfigParser
import re

STANBOL_FOLDER = 'stanbol'
NUXEO_CONF = '/etc/nuxeo/nuxeo.conf'
NUXEO_HOME = '/var/lib/nuxeo/server'
NUXEO_CONFIG_DIR = NUXEO_HOME + '/nxserver/config'
ENTITY_INDEX_FILE = 'dbpedia.solrindex.zip'
ENTITY_INDEX_URL = ('http://dev.iks-project.eu/downloads/stanbol-indices/'
                    'dbpedia-3.7/' + ENTITY_INDEX_FILE)
WORKING_DIR = '/mnt/samar'

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


def cmd(command):
    """Fail early to make it easier to troubleshoot"""
    pflush("remote> " + command)
    code = os.system(command)
    if code != 0:
        raise RuntimeError("Error executing: " + command)


def sudocmd(command, user=None):
    if user is not None:
        command = "sudo -E -u " + user + " " + command
    else:
        command = "sudo -E " + command
    cmd(command)


def pflush(*args, **kwargs):
    """Flush stdout for making Jenkins able to monitor the progress"""
    print(*args, **kwargs)
    sys.stdout.flush()


def debconfselect(pkg, param, value):
    """Preselect DPKG options before installing in non-interactive mode"""
    cmd("echo %s %s select %s | debconf-set-selections" % (pkg, param, value))


def getconfig(filepath, param, default=None):
    """Read a parameter from a config file"""
    with open(filepath, 'rb') as f:
        for line in f:
            if line.strip().startswith('#') or '=' not in line:
                continue
            k, v = line.split('=', 1)
            if k.strip() == param:
                return v.strip()
    return default


def setconfig(filepath, param, value):
    """Edit a config file to set / add a parameter to a specific value"""

    with open(filepath, 'rb') as f:
        lines = f.readlines()
    with open(filepath, 'wb') as f:
        updated = False
        for line in lines:
            if line.strip().startswith('#') or '=' not in line:
                # keep comments and other non informative lines unchanged
                f.write(line)
                continue
            k, v = line.split('=', 1)
            if k.strip() == param:
                # update with new value
                f.write('%s=%s\n' % (param, value))
                updated = True
            else:
                # keep line unchanged
                f.write(line)
        if not updated:
            # append the new param at the end of the file
            f.write('%s=%s\n' % (param, value))


def check_install_nuxeo():
    """Check that Nuxeo is installed from the latest datebased release"""

    # Ensure the datebased release repo is configured and up to date
    with open('/etc/apt/sources.list', 'rb') as f:
        sources = f.readlines()
        databased_sources = [s for s in sources
                             if (not s.strip().startswith('#')
                                 and "snapshots" in s
                                 and "apt.nuxeo.org" in s)]
    if not databased_sources:
        cmd('apt-add-repository '
                    '"deb http://apt.nuxeo.org/ oneiric snapshots"')
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
    # Additional codecs for ffmpeg
    # TODO: uncomment the multiverse repos before updating
    #cmd("apt-get install -y libavcodec-extra-53")


def setup_nuxeo(marketplace_package=None):
    pflush('Configuring Nuxeo server for the SAMAR demo')

    # Check that the repository config has the entity index (ugly patching
    # function, should use an xml parser instead or an incremental extension
    # point instead
    repo_conf = NUXEO_HOME + '/nxserver/config/default-repository-config.xml'
    index_field = '<field>entity:altnames</field>'

    with open(repo_conf, 'rb') as f:
        repo_conf_lines = f.readlines()

    updated = False
    for i, line in enumerate(repo_conf_lines):
        if (line.strip() == '<index name="title">'
            and repo_conf_lines[i + 1].strip() != index_field):
            repo_conf_lines.insert(i + 1, index_field + '\n')
            updated = True
            break

    if updated:
        with open(repo_conf, 'wb') as f:
            for line in repo_conf_lines:
                f.write(line)

    # Skip wizard
    setconfig(NUXEO_CONF, 'nuxeo.wizard.done', 'true')

    # Define an environment variable to locate the nuxeo configuration
    os.environ['NUXEO_CONF'] = NUXEO_CONF

    # Shutting down nuxeo before update
    cmd('service nuxeo stop')

    # Register default nuxeo marketplace packages usually available in the
    # wizard
    nuxeoctl = NUXEO_HOME + '/bin/nuxeoctl'

    pflush('Full purge of existing marketplace packages')
    sudocmd(nuxeoctl + ' mp-purge --accept true', user='nuxeo')
    sudocmd(nuxeoctl + ' mp-init', user='nuxeo')

    pflush('Deploying DM')
    sudocmd(nuxeoctl + ' mp-install nuxeo-dm --accept true', user='nuxeo')
    pflush('Deploying DAM')
    sudocmd(nuxeoctl + ' mp-install nuxeo-dam --accept true', user='nuxeo')

    #pflush('Remove the previous version of the SAMAR package')
    #sudocmd(nuxeoctl + ' mp-remove samar --accept true', user='nuxeo')

    pflush('Deploying / upgrading SAMAR package')
    sudocmd(nuxeoctl + ' mp-install --accept=true --nodeps file://'
        + os.path.abspath(marketplace_package), user='nuxeo')

    # Put the credentials file into the nuxeo config dir
    sudocmd('cp samar.properties ' + NUXEO_CONFIG_DIR, user='nuxeo')

    # Restarting nuxeo
    cmd('service nuxeo start')


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

    pflush("Shutting down and deleting previous instance of "
           "Stanbol server (if any).")
    cmd('cp stanbol_init.sh /etc/init.d/stanbol')
    cmd('cp stanbol_env.sh /etc/default/stanbol')
    cmd('update-rc.d stanbol defaults')
    cmd('service stanbol stop')
    cmd('rm -rf ' + STANBOL_FOLDER)
    cmd('rm -rf stanbol-launcher.jar')

    pflush("Launching new updated Stanbol server")
    cmd('ln -s %s stanbol-launcher.jar' % stanbol_launcher_jar)

    if not os.path.exists(ENTITY_INDEX_FILE):
        pflush('Downloading entity index from ' + ENTITY_INDEX_URL)
        cmd('wget -nv ' + ENTITY_INDEX_URL)
    cmd('mkdir -p %s/datafiles' % STANBOL_FOLDER)
    cmd('ln -s %s %s/datafiles/%s' % (
        os.path.abspath(ENTITY_INDEX_FILE), STANBOL_FOLDER, ENTITY_INDEX_FILE)

    cmd('service stanbol start')


def deploy_translation():
    """Install the translation command"""
    if not os.path.exists('mosesdecoder'):
        cmd("export DEBIAN_FRONTEND=noninteractive; "
            "sudo apt-get install -y libboost-all-dev libz-dev"
            " git build-essential xsltproc")
        cmd("git clone https://github.com/moses-smt/mosesdecoder.git")
        cmd("(cd mosesdecoder && ./bjam -j4)")

    config = ConfigParser.ConfigParser()
    config.read('translation.ini')
    username = config.get('ftp', 'username')
    password = config.get('ftp', 'password')
    server = config.get('ftp', 'server')

    if not os.path.exists('Integration/wapiti-1.3.0.4samar'):
        cmd("wget -nH -nv -r ftp://%s:%s@%s/Integration/wapiti-1.3.0.4samar"
            % (username, password, server))
        cmd("(cd Integration/wapiti-1.3.0.4samar && make clean && make)")
    if not os.path.exists('Integration/models4samar'):
        cmd("wget -nH -nv -r ftp://%s:%s@%s/Integration/models4samar"
            % (username, password, server))

    with open('Integration/models4samar/translate.sh', 'rb') as f:
        def reconfigure(line):
            if '=' in line:
                key, value = line.split('=', 1)
                try:
                    value = config.get('paths', key)
                    return "%s=%s\n" % (key, value.strip())
                except ConfigParser.NoOptionError:
                    pass
            return line
        translate_lines = [reconfigure(l) for l in f.readlines()]
    translate_script = 'Integration/models4samar/translate_reconfigured.sh'
    with open(translate_script, 'wb') as f:
        f.write("".join(translate_lines))
    cmd('chmod +x ' + translate_script)

    # check moses model configuration
    moses_ini_filename = 'Integration/models4samar/moses.tuned.ini'
    with open(moses_ini_filename, 'rb') as f:
        moses_ini_original = f.read()
    moses_ini_updated = re.sub(r"\/vol\/.*/([^ \/]*)",
                               r'/mnt/samar/Integration/models4samar/\1',
                               moses_ini_original)
    if moses_ini_original != moses_ini_updated:
        print("Updating " + moses_ini_filename)
        with open(moses_ini_filename, 'wb') as f:
            f.write(moses_ini_updated)


if __name__ == "__main__":
    os.chdir(WORKING_DIR)
    check_install_nuxeo()
    setup_nuxeo(sys.argv[2])
    check_install_vhost()
    deploy_stanbol(sys.argv[1])
    deploy_translation()
