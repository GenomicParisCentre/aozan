# -*- coding: utf-8 -*-
'''
Aozan main file.
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import sys, os, traceback
import common, hiseq_run, sync_run, demux_run, qc_run
from java.util import Locale
import first_base_report

def create_lock_file(lock_file_path):
    """Create the lock file.

    Arguments:
        lock_file_path path of the lock file
    """

    f = open(lock_file_path, 'w')
    f.write(str(os.getpid()))
    f.close()


def delete_lock_file(lock_file_path):
    """Create the lock file.

    Arguments:
        lock_file_path path of the lock file
    """

    os.unlink(lock_file_path)


def load_pid_in_lock_file(lock_file_path):
    """Load the pid in the lock file.

    Arguments:
        lock_file_path path of the pid file
    """

    f = open(lock_file_path, 'r')
    pid = int(f.readline().strip())
    f.close()
    return pid
   

aozan_version = "0.4"

# Main function
if __name__ == "__main__":

    # Create configuration
    conf = {}
    common.set_default_conf(conf)
    
    # Use default (C) locale
    Locale.setDefault(Locale.US)

    if len(sys.argv) < 1:
        print "No configuration file define in command line.\nSyntax: aozan.py conf_file"
        sys.exit(1)
    else:
        common.load_conf(conf, sys.argv[0])

    # End of Aozan if aozan is not enable
    if conf['aozan.enable'].lower().strip() == 'false':
        sys.exit(0)

    # Check critical free space available
    hiseq_run.send_mail_if_critical_free_space_available(conf)

    lock_file_path = conf['lock.file']

    # Run only if there is no lock
    if not os.path.exists(lock_file_path):

        try:
            create_lock_file(lock_file_path)
            common.log('INFO', 'Start Aozan version ' + aozan_version, conf)
    
            #
            # Discover first base report
            #
    
            first_base_report_sent = first_base_report.load_processed_run_ids(conf)
    
            if conf['first.base.report.step'].lower().strip() == 'true':
                for run_id in (first_base_report.get_available_run_ids(conf) - first_base_report_sent):
                    common.log('INFO', 'First base report ' + run_id, conf)
                    first_base_report.send_report(run_id, conf)
                    first_base_report.add_run_id_to_processed_run_ids(run_id, conf)
                    first_base_report_sent.add(run_id)
                
    
            #
            # Discover hiseq run done
            #
    
            hiseq_run_ids_done = hiseq_run.load_processed_run_ids(conf)
             
            if conf['hiseq.step'].lower().strip() == 'true':
                for run_id in (hiseq_run.get_available_run_ids(conf) - hiseq_run_ids_done):
                    common.log('INFO', 'Discover ' + run_id, conf)
                    hiseq_run.send_mail_if_recent_run(run_id, 12 * 3600, conf)
                    hiseq_run.add_run_id_to_processed_run_ids(run_id, conf)
                    hiseq_run_ids_done.add(run_id)
    
    
            #
            # Load run do not process
            #
    
            hiseq_run_ids_do_not_process = hiseq_run.load_deny_run_ids(conf)
            
            #
            # Sync hiseq and storage
            #
    
            sync_run_ids_done = sync_run.load_processed_run_ids(conf)
    
            # Get the list of run available on HiSeq output
            if conf['sync.step'].lower().strip() == 'true':
                for run_id in (hiseq_run_ids_done - sync_run_ids_done - hiseq_run_ids_do_not_process):
                    common.log('INFO', 'Synchronize ' + run_id, conf)
                    if sync_run.sync(run_id, conf):
                            sync_run.add_run_id_to_processed_run_ids(run_id, conf)
                            sync_run_ids_done.add(run_id)
    
            #
            # Demultiplexing
            #
    
            demux_run_ids_done = demux_run.load_processed_run_ids(conf)
    
            if conf['demux.step'].lower().strip() == 'true':
                for run_id in (sync_run_ids_done - demux_run_ids_done):
                        common.log('INFO', 'Demux ' + run_id, conf)
                        if demux_run.demux(run_id, conf):
                            demux_run.add_run_id_to_processed_run_ids(run_id, conf)
                            demux_run_ids_done.add(run_id)
    
            #
            # Quality control
            #
    
            qc_run_ids_done = qc_run.load_processed_run_ids(conf)
            
            if conf['qc.step'].lower().strip() == 'true':
                for run_id in (demux_run_ids_done - qc_run_ids_done):
                        common.log('INFO', 'Quality control ' + run_id, conf)
                        if qc_run.qc(run_id, conf):
                            qc_run.add_run_id_to_processed_run_ids(run_id, conf)
                            qc_run_ids_done.add(run_id)
    
            delete_lock_file(lock_file_path)
            
            # TODO remove *.lasterr files
    
            common.log('INFO', 'End of Aozan', conf)
        except:
                common.log('CRITICAL', 'Exception: ' +  str(sys.exc_info()[0]) + ' (' + str(sys.exc_info()[1]) + ')' , conf)
                common.log('TRACEBACK', traceback.format_exc(sys.exc_info()[2]).replace('\n', ' '), conf)
    else:
        print "A lock file exists."
        if not os.path.exists('/proc/%d' % (load_pid_in_lock_file(lock_file_path))):
            common.error('[Aozan] A lock file exists', 'A lock file exist at ' + conf['lock.file'] +
                         ". Please investigate last error and then remove the lock file.", conf['aozan.var.path'] + '/aozan.lasterr', conf)
