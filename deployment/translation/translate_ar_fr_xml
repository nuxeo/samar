#!/bin/bash
#
#########################################################################################
# Author: Souhir GAHBICHE BRAHAM
# Description : Machine Translation of Arabic Data
#               Translation from arabic to french
#########################################################################################

set -x
set -e

input_file=$1
filename=`echo $input_file | sed 's/.xml//' `

if [[ ${#@} != 1 ]]
then
  echo "Error ($(readlink -f $0)): Wrong number of arguments (${#@})"
  echo "Usage: translate.sh arabic-file.xml"
  exit 1
fi

ar_fr_root=/mnt/samar/Integration/AR-FR
tmp=/tmp
dir_wapiti=$ar_fr_root/version1/wapiti-1.3.0.4samar/
dir_soupiti=$ar_fr_root/version2/wapiti-1.3.0.4samar/ArabicSplit

filterdecoder=/mnt/samar/mosesdecoder/scripts/training/filter-model-given-input.pl
mosesdecoder=/mnt/samar/mosesdecoder/moses-cmd/src/bin/gcc-4.6/release/debug-symbols-on/link-static/threading-multi/moses
mosesini=/mnt/samar/moses.tuned.ar_fr.ini

modelsoupiti=$ar_fr_root/version1/models4samar/modelPOS+SEG-final-0.1.crf
detok=/mnt/samar/detok4samar.bash

last=`echo $filename | tr '/' '\n' | tail -1`
details=details.$last

#export DIR=/people/lehaison/soul
#export LIB_TYPE=INTEL
#export DIR_INSTALL=$DIR/intel
#export PATH=/people/lehaison/share/$LIB_TYPE/bin:/people/lehaison/share/global/script/bin:$DIR_INSTALL/bin:/usr/lib64/mpi/gcc/openmpi/bin:/usr/local/bin:/usr/bin:/bin:/usr/bin/X11:/usr/X11R6/bin:/usr/games:/opt/kde3/bin:/usr/lib64/jvm/jre/bin:/usr/lib/mit/bin:/usr/lib/mit/sbin:/people/jmcrego/licensed-soft/fsm-4.0/bin:/people/jmcrego/licensed-soft/fsm-4.0/bin
#export LD_LIBRARY_PATH=/people/lehaison/share/global/lib:/people/lehaison/share/$LIB_TYPE/lib64:$DIR_INSTALL/lib
#export OMP_NUM_THREADS=16
#export PYTHONPATH=/people/lehaison/share/global/script:/people/lehaison/share/$LIB_TYPE/lib64/python2.6/site-packages

# extract from newsml
$dir_wapiti/ArabicSplit/newsML2newsML.py < $input_file > $filename.brut
#xsltproc $dir_wapiti/ArabicSplit/extract_txt.xsl $input_file > $filename.brut

#$dir_wapiti/ArabicSplit/convertUTF8toBW.pl $input_file.brut $input_file.bw

# segmentation phrase par phrase avec wapiti
#$dir_wapiti/ArabicSplit/seg_depeche.pl $input_file.bw $dir_wapiti

# soupiti preprocess
$dir_wapiti/ArabicSplit/segment.sh $filename $dir_wapiti $modelsoupiti

rm $filename.*punc
#rm $input_file.*cl
#rm $input_file.*lv
#rm $input_file.*sgm*
rm $filename.*norm
rm $filename.*wa*

# Moses
# filtrer le Modele de trad et le modele de reordering
rm -rf filtered.nc-test.$last
$filterdecoder filtered.nc-test.$last $mosesini $filename.result 

# Traduction  
( $mosesdecoder -config filtered.nc-test.$last/moses.ini -translation-details $details -input-file $filename.result > $filename.out.fr ) >& tuned.decode.log.$last

# Detokenisation
$detok $dir_soupiti $filename.out.fr
# remettre le format newsML
$dir_wapiti/ArabicSplit/newsML2newsML.py $filename.out.fr.detok < $input_file > $filename.fr.xml

sed -i 's/xml:lang=\"ar\"/xml:lang=\"ar:fr\"/' $filename.fr.xml 

