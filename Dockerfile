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

## Examples to build Aozan workspace repository, corresponding with path in Aozan configuration file. 

# Mount usefull directory for Aozan
RUN mkdir -p /aozan_data/var_aozan
RUN mkdir -p /aozan_data/conf
RUN mkdir -p /aozan_data/log
RUN mkdir -p /aozan_data/hiseq
RUN mkdir -p /aozan_data/bcl
RUN mkdir -p /aozan_data/fastq
RUN mkdir -p /aozan_data/runs
RUN mkdir -p /aozan_data/casava_samplesheet
RUN mkdir -p /aozan_data/aozan_tmp
RUN mkdir -p /aozan_data/ressources
RUN mkdir -p /aozan_data/ncbi_nt
RUN mkdir -p /aozan_data/ressources/genomes
RUN mkdir -p /aozan_data/ressources/genomes_descs
RUN mkdir -p /aozan_data/ressources/mappers_indexes


## Command to launch docker Aozan-1.2.7
# docker run -i -t --rm -v /path/to/real/dir/var_aozan:/aozan_data/var_aozan -v /path/to/real/dir/conf:/aozan_data/conf -v /path/to/real/dir/hiseq:/aozan_data/hiseq -v /path/to/real/dir/bcl:/aozan_data/bcl -v /path/to/real/dir/fastq:/aozan_data/fastq -v /path/to/real/dir/runs:/aozan_data/runs -v /path/to/real/dir/casava_designs:/aozan_data/casava_samplesheet -v /path/to/real/dir/aozan_tmp:/aozan_data/aozan_tmp -v /path/to/real/dir/ressources:/aozan_data/ressources -v /path/to/real/dir/ncbi_nt:/aozan_data/ncbi_nt -v /path/to/real/dir/ressources/genomes:/aozan_data/ressources/genomes -v /path/to/real/dir/ressources/genomes_descs:/aozan_data/ressources/genomes_descs -v /path/to/real/dir/ressources/mappers_indexes:/aozan_data/ressources/mappers_indexes genomicpariscentre/aozan:1.2.7 bash
