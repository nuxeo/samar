#/bin/sh

set +ex

if [ ! -d stanbol-temis ]
then
    git clone https://github.com/nuxeo/stanbol-temis
fi
if [ ! -d stanbol-mondeca ]
then
    git clone https://github.com/nuxeo/stanbol-mondeca
fi
if [ ! -d nuxeo-newsml ]
then
    git clone https://github.com/nuxeo/nuxeo-newsml
fi
if [ ! -d nuxeo-platform-semantic-entities ]
then
    git clone https://github.com/nuxeo/nuxeo-platform-semantic-entities
fi
