############################################################
# Dockerfile to build aozan container images
# Based on CentOS with bcl2fastq-1.8.4 images made by Laurent Jourdren
############################################################

# Set the base image to bcl2fastq-1.8.4
FROM genomicpariscentre/bcl2fastq:1.8.4

# File Author / Maintainer
MAINTAINER Laurent Jourdren <jourdren@biologie.ens.fr>

# Update repository sources list
# RUN yum update

# Install java 7
RUN yum install -y java-1.7.0-openjdk.x86_64

# Build work directory
RUN mkdir /aozan_data

# Install Aozan public version
RUN cd /tmp && wget www.transcriptome.ens.fr/aozan/aozan-1.2.7.tar.gz
RUN cd /usr/local && tar xvzf /tmp/aozan-*.tar.gz && rm /tmp/aozan-*.tar.gz

RUN ln -s /usr/local/aozan*/aozan.sh /usr/local/bin


# Patch bug in aozan.sh
RUN sed -i 's/BASEDIR=`dirname $0`/ARG0=`readlink $0` ; BASEDIR=`dirname $ARG0`/' /usr/local/aozan-*/aozan.sh && chmod +x /usr/local/aozan-*/aozan.sh

# Patch bcl2fast configuration file for use bz2 compression type
# Comment 2 lines
RUN sed -i -e 's/^COMPRESSION:=gzip$/#COMPRESSION:=gzip/' -e 's/^COMPRESSIONSUFFIX:=.gz$/#COMPRESSIONSUFFIX:=.gz/' /usr/local/share/bcl2fastq-*/makefiles/Config.mk

# Install blast
RUN cd /tmp ; wget download.opensuse.org/repositories/home:/joscott/CentOS_CentOS-5/x86_64/ncbi-blast-2.2.29_plus-2.3.x86_64.rpm

RUN yum -y --nogpgcheck localinstall /tmp/ncbi-blast*.rpm

RUN rm -rf /tmp/*.rpm


# Add script to run blastall via script legacy_blast.pl
RUN echo -e  '#! /bin/bash\n\n/usr/bin/legacy_blast.pl blastall $@\n' > /usr/local/blastall && chmod +x /usr/local/blastall
#Build usefull directory for Aozan
