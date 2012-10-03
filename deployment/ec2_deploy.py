"""Deploy a Nuxeo instance in the EC2 cloud"""
# Author: Olivier Grisel <oogrisel@nuxeo.com>

from __future__ import print_function
import os
from os.path import join
from os.path import abspath
import sys

from time import sleep
from boto import ec2
from boto.exception import EC2ResponseError


PROJECT_PATH = abspath(join(__file__, '..', '..'))
DEPLOYMENT_FOLDER = abspath(join(__file__, '..'))


#IMAGE_ID = 'ami-ae05889e'  # Ubuntu 11.10 64bits for us-west-2
IMAGE_ID = 'ami-895069fd'  # Ubuntu 11.10 64bits for eu-west-1
INSTANCE_NAME = 'samar_demo'
INSTANCE_TYPE = 'm1.large'
REGION_NAME = 'eu-west-1'
KEYS_FOLDER = os.path.expanduser('~/aws_keys')

STANBOL_LAUNCHER_FILE = 'samar-stanbol-launcher-0.1.0-SNAPSHOT.jar'
STANBOL_LAUNCHER_PATH = join(PROJECT_PATH, 'samar-stanbol-launcher', 'target',
                             STANBOL_LAUNCHER_FILE)

SAMAR_PACKAGE_FILE = 'samar-marketplace-0.1.0-SNAPSHOT.zip'
SAMAR_PACKAGE_PATH = join(PROJECT_PATH, 'packaging', 'samar-marketplace',
                          'target', SAMAR_PACKAGE_FILE)

DEFAULT_MARKER = object()


def cmd(cmd):
    code = os.system(cmd)
    if code != 0:
        raise RuntimeError("Error executing: " + cmd)


def pflush(*args, **kwargs):
    """Flush stdout for making Jenkins able to monitor the progress"""
    print(*args, **kwargs)
    sys.stdout.flush()


class Controller(object):
    """Utility class to control the cloud nodes"""

    def __init__(self, region, keypair_name, keys_folder, ssh_user='ubuntu',
                 **ec2_params):
        self.conn = ec2.connect_to_region(region, **ec2_params)

        # issue a dummy query to check the connection
        self.conn.get_all_instances()

        if not os.path.exists(keys_folder):
            os.makedirs(keys_folder)

        self.keypair_name = keypair_name
        self.key_file = os.path.join(keys_folder, keypair_name + '.pem')

        try:
            kp = self.conn.get_key_pair(keypair_name)
        except EC2ResponseError:
            kp = None

        if os.path.exists(self.key_file):
            # check that the keypair exists in the cloud
            if kp is None:
                raise RuntimeError(
                    "Found local key file '%s' but no matching keypairs"
                    " registered under the name '%s' on EC2: delete local key"
                    " file and try again."
                    % (self.key_file, keypair_name))
        else:
            if kp is not None:
                raise RuntimeError(
                    "Existing keypair registered under name '%s' on EC2 but"
                    " could not find local key file '%s'."
                    " If local file was lost, delete keypair on in AWS"
                    " Console and and try again."
                % (keypair_name, self.key_file))

            # create the keypair in the cloud and save it locally
            pflush('Creating new keypair with name:', keypair_name)
            kp = self.conn.create_key_pair(keypair_name)
            kp.save(keys_folder)
            pflush('Saved key file:', self.key_file)

        self.ssh_user = ssh_user

    def get_connection(self):
        return self.conn

    def get_running_instance(self, instance_name):

        instances = []
        for r in self.conn.get_all_instances():
            for i in r.instances:
                if ((i.tags.get('Name') == instance_name
                     or i.tags.get('name') == instance_name)
                    and i.state == 'running'):
                    instances.append(i)

        if len(instances) == 0:
            return None

        elif len(instances) > 1:
            raise RuntimeError(
                'Found more than one running instance with name %s: %r' %
                (instance_name, instances))

        return instances[0]

    def create_instance(self, instance_name, image_id, instance_type,
                        security_groups=(), ports=(22, 80, 443)):

        if not security_groups:
            # check whether there already exist a security group named after
            # the instance name
            existing_groups = [g for g in self.conn.get_all_security_groups()
                               if g.name == instance_name]
            if not existing_groups:
                # create a security group with the instance_name and grant the
                # necessary rights for ssh and 8080
                description = ("Open port 22 for ssh, 80, 443 for web server"
                               " and 8080 for direct acces to Nuxeo.")

                pflush("Creating security group for instance '%s': %s"
                      % (instance_name, description))
                sg = self.conn.create_security_group(instance_name,
                                                     description)
                for port in ports:
                    sg.authorize('tcp', port, port, '0.0.0.0/0')
            else:
                pflush("Reusing existing security group:", instance_name)

            security_groups = [instance_name]

        reservation = self.conn.run_instances(
            image_id,
            key_name=keypair_name,
            instance_type=instance_type,
            security_groups=security_groups,
        )
        assert len(reservation.instances) == 1
        instance = reservation.instances[0]
        # wait a bit before creating the tag otherwise it might be impossible
        # to fetch the status of the instance (AWS bug?).
        sleep(0.5)
        self.conn.create_tags([instance.id], {"Name": instance_name})

        retries = 0
        delay = 10
        while instance.state != 'running' and retries < 10:
            pflush("Waiting %ds for instance '%s' to startup (state='%s')..."
                  % (delay, instance_name, instance.state))
            sleep(delay)
            instance.update()
            retries += 1
        return instance

    def check_ssh_connection(self, max_retries=6, delay=10):
        self.check_connected()
        retries = 0
        while retries < max_retries:
            pflush("Checking ssh connection on: '%s'..."
                  % self.instance.dns_name)
            if self.cmd('echo "connection check"', raise_if_fail=False) == 0:
                return
            sleep(delay)
            retries += 1
        raise RuntimeError('Failed to connect via ssh')

    def connect(self, instance_name, image_id, instance_type,
                security_groups=(), ports=(22, 80, 443)):
        """Connect the crontroller to the remote node, create it if missing"""
        instance = self.get_running_instance(instance_name)

        if instance is not None:
            pflush("Reusing running instance with name '%s' at %s" % (
                instance_name, instance.dns_name))
        else:
            pflush("No running instance with name '%s', creating a new one..."
                  % instance_name)

            instance = controller.create_instance(
                instance_name, image_id, instance_type, ports=ports)

            pflush("Started instance with name '%s' at %s" % (
                instance_name, instance.dns_name))

        self.instance = instance
        self.ssh_host = "%s@%s" % (self.ssh_user, self.instance.dns_name)
        self.check_ssh_connection()

    def check_connected(self):
        if not hasattr(self, 'ssh_host') or self.ssh_host is None:
            raise RuntimeError(
                'No instance connected: call the connect method first')

    def cmd(self, cmd, raise_if_fail=True):
        self.check_connected()
        pflush(">", cmd)
        code = os.system("ssh -o \"StrictHostKeyChecking no\"  -i %s %s '%s'" %
                  (self.key_file, self.ssh_host, cmd))
        if code != 0 and raise_if_fail:
            raise RuntimeError("Remote command %s return %d" % (cmd, code))
        return code

    def put(self, local, remote, rsync=True):
        self.check_connected()
        remote = "%s:%s" % (self.ssh_host, remote)
        pflush("> Sending '%s' to '%s'" % (local, remote))
        if rsync:
            cmd = ('rsync -Paz'
                   ' --rsh "ssh -o \'StrictHostKeyChecking no\' -i %s"'
                   ' --rsync-path "sudo rsync" %s %s' %
                   (self.key_file, local, remote))
        else:
            cmd = "scp -r -o \"StrictHostKeyChecking no\" -i %s %s %s" % (
                self.key_file, local, remote)

        code = os.system(cmd)
        if code != 0:
            raise RuntimeError("Failed to send '%s' to '%s'" % (local, remote))

    def get(self, remote, local=None):
        self.check_connected()
        # TODO

    def exec_script(self, local, arguments=None, sudo=False):
        self.check_connected()
        script_name = os.path.basename(local)
        self.put(local, script_name)
        if sudo:
            self.cmd('sudo chmod +x ' + script_name)
        else:
            self.cmd('chmod +x ' + script_name)
        cmd = "./%s" % script_name
        if sudo:
            cmd = "sudo " + cmd
        if arguments is not None:
            cmd += " " + arguments
        self.cmd(cmd)

    def terminate(self, instance_name=None):
        """Terminate the running instance"""
        if instance_name is None:
            self.check_connected()
            instance = self.instance
        else:
            instance = self.get_running_instance(instance_name)
            if instance is None:
                pflush('Already terminated')
                return

        pflush("Terminating instance:", instance.dns_name)
        instance.terminate()
        if hasattr(self, 'instance')  and instance.id == self.instance.id:
            self.ssh_host = None


def get_env(name, default=DEFAULT_MARKER, required=False):
    value = os.environ.get(name, default)
    if required and value is DEFAULT_MARKER:
        raise RuntimeError('Environment variable "%s" is not defined' % name)
    return value


if __name__ == '__main__':
    aws_key_id = get_env('NX_AWS_ACCESS_KEY_ID', required=True)
    aws_secret_key = get_env('NX_AWS_SECRET_ACCESS_KEY', required=True)
    keypair_name = get_env('NX_KEYPAIR_NAME', INSTANCE_NAME)
    keys_folder = get_env('NX_KEYS_FOLDER', KEYS_FOLDER)

    instance_name = get_env('NX_INSTANCE_NAME', INSTANCE_NAME)
    region_name = get_env('NX_REGION_NAME', REGION_NAME)
    image_id = get_env('NX_IMAGE_ID', IMAGE_ID)
    instance_type = get_env('NX_INSTANCE_TYPE', INSTANCE_TYPE)
    #package_link = get_env('NX_PACKAGE_LINK', required=True)

    pflush("Active parameters:")
    pflush("aws_key_id: ", aws_key_id)
    pflush("region_name: ", region_name)
    pflush("image_id: ", image_id)
    pflush("instance_type: ", instance_type)
    pflush("instance_name: ", instance_name)
    pflush("keypair_name: ", keypair_name)
    pflush()

    controller = Controller(
        region_name,
        aws_access_key_id=aws_key_id,
        aws_secret_access_key=aws_secret_key,
        keypair_name=keypair_name,
        keys_folder=keys_folder,
    )
    controller.connect(instance_name, image_id, instance_type,
                       ports=(22, 80, 443, 9090))

    WORKING_DIR = '/mnt/samar/'
    controller.cmd('sudo mkdir -p ' + WORKING_DIR)
    controller.cmd('sudo chown -R ubuntu:ubuntu ' + WORKING_DIR)

    # Upload the stanbol launcher
    controller.put(STANBOL_LAUNCHER_PATH, WORKING_DIR + STANBOL_LAUNCHER_FILE)

    # Upload the SAMAR marketplace package
    controller.put(SAMAR_PACKAGE_PATH, WORKING_DIR + SAMAR_PACKAGE_FILE)


    # Send a file will all the required enviroment variables
    environment_file = join(DEPLOYMENT_FOLDER, 'stanbol_env.sh')
    if not os.path.exists(environment_file):
        print("ERROR: please copy stanbol_env_sample.sh as stanbol_env.sh"
              " and adjust the credentials")
        sys.exit(1)
    controller.put(environment_file, WORKING_DIR + 'stanbol_env.sh')

    # Send a file with the vocapia credentials as nuxeo properties
    samar_properties = join(DEPLOYMENT_FOLDER, 'samar.properties')
    if not os.path.exists(samar_properties):
        print("ERROR: please copy samar_sample.properties as samar.properties"
              " and adjust the credentials")
        sys.exit(1)
    controller.put(samar_properties, WORKING_DIR + 'samar.properties')

    # Init script
    controller.put(join(DEPLOYMENT_FOLDER, 'stanbol_init.sh'),
                   WORKING_DIR + 'stanbol_init.sh')

    # Send the credentials for downloading the translation model directly from
    # the FTP server
    for filename in [
        'translation.ini',
        'moses.tuned.ar_fr.ini',
        'translate_ar_fr_newsml.sh',
        'translate_ar_fr_txt.sh']:
        controller.put(join(DEPLOYMENT_FOLDER, 'translation', filename),
                   WORKING_DIR + filename)


    # Setup the node by running a script
    arguments = STANBOL_LAUNCHER_FILE + " " + SAMAR_PACKAGE_FILE
    controller.exec_script(join(DEPLOYMENT_FOLDER, 'setup_node.py'),
                           sudo=True, arguments=arguments)

    print("Successfully deployed demo at: http://%s/" %
          controller.instance.dns_name)
