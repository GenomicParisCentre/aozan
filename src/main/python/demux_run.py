# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''
import os.path, stat, sys
import common, hiseq_run, time
import glob
from xml.etree.ElementTree import ElementTree
from java.io import IOException
from java.lang import Runtime, Throwable, Exception
from java.util import HashMap

from fr.ens.biologie.genomique.aozan import AozanException
from fr.ens.biologie.genomique.aozan.util import DockerUtils
from fr.ens.biologie.genomique.aozan.illumina.samplesheet import SampleSheetUtils, \
    SampleSheetCheck

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_URL_KEY
from fr.ens.biologie.genomique.aozan.Settings import DEMUX_SPACE_FACTOR_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_ADAPTER_FASTA_FILE_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_COMPRESSION_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_COMPRESSION_LEVEL_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_FASTQ_CLUSTER_COUNT_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_MISMATCHES_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEET_FORMAT_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEETS_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_THREADS_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_WITH_FAILED_READS_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_USE_DOCKER_KEY
from fr.ens.biologie.genomique.aozan.Settings import INDEX_SEQUENCES_KEY
from fr.ens.biologie.genomique.aozan.Settings import FASTQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY
from fr.ens.biologie.genomique.aozan import Settings

from fr.ens.biologie.genomique.aozan.util import StringUtils
from fr.ens.biologie.genomique.aozan.illumina import RunInfo
from fr.ens.biologie.genomique.aozan.illumina.samplesheet.io import SampleSheetXLSReader, \
    SampleSheetCSVWriter, SampleSheetCSVReader

BCL2FASTQ2_VERSION = "latest"


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/demux.done')


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf[AOZAN_VAR_PATH_KEY] + '/demux.done', conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] demultiplexer: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/demux.lasterr', conf)


def load_index_sequences(conf):
    """Load the map of the index sequences.

    Arguments:
        index_shortcut_path: the path to the index sequences
    """

    result = HashMap()

    if not common.is_file_exists(INDEX_SEQUENCES_KEY, conf):
        return result

    f = open(conf[INDEX_SEQUENCES_KEY], 'r')

    for l in f:
        l = l[:-1]
        if len(l) == 0:
            continue
        fields = l.split('=')
        if len(fields) == 2:
            result[fields[0].strip().lower()] = fields[1].strip().upper()

    f.close()

    return result


def get_flowcell_id_in_demultiplex_xml(fastq_output_dir):
    """Get the flowcell id in DemultiplexConfig.xml.

    Arguments:
        fastq_output_dir: the path to the fastq output directory
    """

    tree = ElementTree()
    tree.parse(fastq_output_dir + '/DemultiplexConfig.xml')

    return tree.find("Flowcell").attrib['flowcell-id']


def build_samplesheet_filename(run_id, conf):
    """ Return the sample sheet filename on the run.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    run_number = hiseq_run.get_run_number(run_id)
    instrument_sn = hiseq_run.get_instrument_sn(run_id)

    return conf[BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY] + '_' + instrument_sn + '_%04d' % run_number


def check_samplesheet(run_id, input_run_data_path, samplesheet_filename, conf):
    """ Check sample sheet and convert in csv format if useful.

    Arguments:
        run id: The run id
        input_run_data_path: The input run data path
        samplesheet_filename: sample sheet filename
        bcl2fastq_major_version: bcl2fastq major version
        conf: configuration dictionary

    Return:
        true it's all ok otherwise false
    """

    run_info_path = input_run_data_path + '/RunInfo.xml'

    if not os.path.isfile(run_info_path):
        error("no RunInfo.xml file found for run " + run_id,
              "No RunInfo.xml file found for run " + run_id + ': ' + run_info_path + '.\n', conf)
        return False, []

    run_info = RunInfo.parse(run_info_path)
    flow_cell_id = run_info.getFlowCell()
    lane_count = run_info.getFlowCellLaneCount()

    input_samplesheet_xls_path = conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '/' + samplesheet_filename + '.xls'
    input_samplesheet_csv_path = conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '/' + samplesheet_filename + '.csv'
    samplesheet_csv_path = conf[TMP_PATH_KEY] + '/' + samplesheet_filename + '.csv'

    common.log("INFO", "Flowcell id: " + flow_cell_id, conf)
    common.log("INFO", "Flowcell lane count: " + str(lane_count), conf)
    common.log("INFO", "Samplesheet format: " + str(conf[BCL2FASTQ_SAMPLESHEET_FORMAT_KEY]), conf)

    if common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xls', conf):

        # Convert samplesheet in XLS format to CSV format
        common.log("INFO", "Samplesheet path: " + str(input_samplesheet_xls_path), conf)

        # Check if the xls samplesheet exists
        if not os.path.exists(input_samplesheet_xls_path):
            error("no bcl2fastq sample sheet found", "No bcl2fastq sample sheet found for " + run_id + " run.\n" +
                  'You must provide a ' + samplesheet_filename + '.xls file in ' + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] +
                  ' directory to demultiplex and create fastq files for this run.\n', conf)
            return False, []

        try:

            # Load XLS samplesheet file
            samplesheet = SampleSheetXLSReader(input_samplesheet_xls_path).read()

            # Replace index sequence shortcuts by sequences
            SampleSheetUtils.replaceIndexShortcutsBySequences(samplesheet, load_index_sequences(conf))

            # Set the lane field if does not set
            SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(samplesheet, lane_count)

            # Write CSV samplesheet file in BCL2FASTQ2 format
            writer = SampleSheetCSVWriter(samplesheet_csv_path)
            writer.setVersion(2)
            writer.writer(samplesheet)

        except AozanException, exp:
            print StringUtils.stackTraceToString(exp)

            error("error while converting " + samplesheet_filename + ".xls to CSV format", exp.getMessage(), conf)
            return False, []
        except Exception, exp:
            print StringUtils.stackTraceToString(exp)

            error("error while converting " + samplesheet_filename + ".xls to CSV format", exp.getMessage(), conf)
            return False, []

    elif common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'csv', conf):

        # Copy the CSV file
        common.log("INFO", "sample sheet filename : " + str(input_samplesheet_xls_path), conf)

        # Check if the csv samplesheet exists
        if not os.path.exists(input_samplesheet_csv_path):
            error("no bcl2fastq sample sheet found", "No bcl2fastq sample sheet found for " + run_id + " run.\n" +
                  'You must provide a ' + samplesheet_filename + '.csv file in ' + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] +
                  ' directory to demultiplex and create fastq files for this run.\n', conf)
            return False, []

        cmd = 'cp ' + input_samplesheet_csv_path + ' ' + samplesheet_csv_path
        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while copying Bcl2fastq CSV sample sheet file to temporary directory for run " + run_id,
                  'Error while copying Bcl2fastq CSV sample sheet file to temporary directory.\nCommand line:\n' + cmd,
                  conf)
            return False, []

    elif common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'command', conf):

        action_error_msg = 'Error while creating Bcl2fastq CSV sample sheet file'
        if not common.is_conf_key_exists(BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY, conf):
            error(action_error_msg + ' for run ' + run_id, action_error_msg + ' the command is empty.', conf)
            return False, []

        cmd = conf[BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY] + ' ' + run_id + ' ' + samplesheet_csv_path
        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error(action_error_msg + ' for run ' + run_id,
                  action_error_msg + '.\nCommand line:\n' + cmd, conf)

        if not os.path.exists(samplesheet_csv_path):
            error(action_error_msg + ' for run ' + run_id,
                  action_error_msg + ', the external command did not create Bcl2fastq CSV file:\n' + cmd, conf)
            return False, []
    else:
        error('Error while creating Bcl2fastq CSV sample sheet file for run ' + run_id,
              'No method to get Bcl2fastq sample sheet file has been defined. Please, set the ' +
              '"bcl2fastq.samplesheet.format" property.\n',
              conf)
        return False, []

    # Check if Bcl2fastq CSV samplesheet file has been created
    if not os.path.exists(samplesheet_csv_path):
        error("error while reading Bcl2fastq CSV sample sheet file for run " + run_id,
              'Error while reading Bcl2fastq CSV sample sheet file, the sample sheet file does not exist: \n' +
              samplesheet_csv_path, conf)
        return False, []

    samplesheet_warnings = {}

    # Check Bcl2fastq CSV samplesheet file
    try:
        # Load CSV samplesheet file
        samplesheet = SampleSheetCSVReader(samplesheet_csv_path).read()

        # Check values of samplesheet file
        samplesheet_warnings = SampleSheetCheck.checkSampleSheet(samplesheet, flow_cell_id)

    # TODO: remove lock

    except IOException, exp:
        error("error while checking " + samplesheet_filename + ".csv file ", exp.getMessage(), conf)
        return False, samplesheet_warnings
    except AozanException, exp:
        error("error while checking " + samplesheet_filename + ".csv file ", exp.getMessage(), conf)
        return False, samplesheet_warnings

    # Log Bcl2fastq samplesheet warning
    if samplesheet_warnings > 0:
        msg = ''
        first = True
        for warn in samplesheet_warnings:
            if first:
                first = False
            else:
                msg += ' '
            msg += warn
        common.log("WARNING", "bcl2fastq sample sheet warnings: " + msg, conf)

    # Sample sheet
    return samplesheet_csv_path, samplesheet_warnings


def get_cpu_count(conf):
    """ Return cpu count.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    # Get the number of cpu
    cpu_count = int(conf[BCL2FASTQ_THREADS_KEY])
    if cpu_count < 1:
        cpu_count = Runtime.getRuntime().availableProcessors()

    return cpu_count


def create_bcl2fastq_command_line(run_id, command_path, input_run_data_path, fastq_output_dir, samplesheet_csv_path,
                                  tmp_path, nb_mismatch, conf):
    nb_threads = str(get_cpu_count(conf))

    if command_path is None:
        final_command_path = 'bcl2fastq'
    else:
        final_command_path = command_path

    #  List arg
    args = []
    args.append(final_command_path)
    args.extend(['--loading-threads', nb_threads])
    args.extend(['--demultiplexing-threads', nb_threads])
    args.extend(['--processing-threads', nb_threads])
    args.extend(['--writing-threads', nb_threads])

    args.extend(['--sample-sheet', samplesheet_csv_path])
    args.extend(['--barcode-mismatches', nb_mismatch])

    # Common parameter, setting per default
    args.extend(['--input-dir', input_run_data_path + '/Data/Intensities/BaseCalls'])
    args.extend(['--output-dir', fastq_output_dir])

    if common.is_conf_value_equals_true(BCL2FASTQ_WITH_FAILED_READS_KEY, conf):
        args.extend(['--with-failed-reads'])

    # Specific parameter
    args.extend(['--runfolder-dir', input_run_data_path])
    args.extend(['--interop-dir', fastq_output_dir + '/InterOp'])
    args.extend(['--min-log-level', 'TRACE'])
    # args.extend(['--stats-dir', fastq_output_dir + '/Stats'])
    # args.extend(['--reports-dir', fastq_output_dir + '/Reports'])

    if common.is_conf_key_exists(BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY, conf):
        args.append(conf[BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY])

    # Retrieve output in file
    args.extend(['>', tmp_path + '/bcl2fastq_output_' + run_id + '.out'])
    args.extend(['2>', tmp_path + '/bcl2fastq_output_' + run_id + '.err'])

    return " ".join(args)


def demux_run_standalone(run_id, input_run_data_path, fastq_output_dir, samplesheet_csv_path, nb_mismatch, conf):
    """ Demultiplexing the run with bcl2fastq on version parameter.

    Arguments:
        run_id: The run id
        input_run_data_path: input run data path to demultiplexing
        fastq_output_dir: fastq directory to save result on demultiplexing
        samplesheet_csv_path: sample sheet path in csv format, version used by bcl2fastq
        conf: configuration dictionary
    """

    bcl2fastq_executable_path = conf[BCL2FASTQ_PATH_KEY]

    # Check if the bcl2fastq path is OK
    if os.path.isdir(bcl2fastq_executable_path):
        bcl2fastq_executable_path += '/bcl2fastq'
    elif not os.path.isfile(bcl2fastq_executable_path):
        error("error while setting executable command file bcl2fastq for run " + run_id + ", invalid bcl2fastq path: " +
              bcl2fastq_executable_path, "error while setting executable command file bcl2fastq for run " + run_id +
              ", invalid bcl2fastq path: " + bcl2fastq_executable_path, conf)
        return False

    cmd = create_bcl2fastq_command_line(run_id, bcl2fastq_executable_path, input_run_data_path, fastq_output_dir,
                                        samplesheet_csv_path, conf[TMP_PATH_KEY], nb_mismatch, conf)

    common.log('INFO', 'demultiplexing in standalone mode with the following command line: ' + str(cmd), conf)

    exit_code = os.system(cmd)
    if exit_code != 0:
        error("error while setting executable command file bcl2fastq for run " + run_id,
              'Error while setting executable command file bcl2fastq (exit code: ' + str(
                  exit_code) + ').\nCommand line:\n' + cmd, conf)
        return False

    cmd = 'cp ' + conf[TMP_PATH_KEY] + '/bcl2fastq_output_' + run_id + '.* ' + fastq_output_dir
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while setting read only the output fastq directory for run " + run_id,
              'Error while setting read only the output fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    # The output directory must be read only
    # cmd = 'chmod -R ugo-w ' + fastq_output_dir + '/Project_*'
    cmd = 'find ' + fastq_output_dir + ' -type f -name "*.fastq.*" -exec chmod ugo-w {} \; '
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while setting read only the output fastq directory for run " + run_id,
              'Error while setting read only the output fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    # All ok
    return True


def demux_run_with_docker(run_id, input_run_data_path, fastq_output_dir, samplesheet_csv_path, nb_mismatch, conf):
    """ Demultiplexing the run with bcl2fastq on version parameter with image Docker.

    Arguments:
        run_id: The run id
        input_run_data_path: input run data path to demultiplexing
        fastq_output_dir: fastq directory to save result on demultiplexing
        samplesheet_csv_path: sample sheet path in csv format, version used by bcl2fastq
        conf: configuration dictionary
    """

    # In docker mount with input_run_data_path
    input_docker = '/data/input'
    input_run_data_path_in_docker = input_docker

    # In docker mount with fastq_output_dir
    output_docker = '/data/output'
    fastq_data_path_in_docker = output_docker + '/' + os.path.basename(fastq_output_dir)

    tmp = conf[TMP_PATH_KEY]
    tmp_docker = '/tmp'

    samplesheet_csv_docker = tmp_docker + '/' + os.path.basename(samplesheet_csv_path)

    cmd = create_bcl2fastq_command_line(run_id, None, input_run_data_path_in_docker, fastq_data_path_in_docker,
                                        samplesheet_csv_docker, tmp_docker, nb_mismatch, conf)

    try:
        # Set working in docker on parent demultiplexing run directory.
        # Demultiplexing run directory will create by bcl2fastq
        docker = DockerUtils(['/bin/bash', '-c', cmd], 'bcl2fastq2', BCL2FASTQ2_VERSION)
        # docker = DockerUtils('touch /tmp/totot', 'bcl2fastq2', bcl2fastq_version)

        common.log("CONFIG", "bcl2fastq run with image docker from " + docker.getImageDockerName() +
                   " with command line " + cmd, conf)

        common.log("CONFIG", "bcl2fastq docker mount: " +
                   str(os.path.dirname(fastq_output_dir)) + ":" + str(output_docker) + "; " +
                   input_run_data_path + ":" + input_docker + "; " + tmp + ":" + tmp_docker, conf)

        # Mount input directory
        docker.addMountDirectory(input_run_data_path, input_docker)
        docker.addMountDirectory(os.path.dirname(fastq_output_dir), output_docker)
        docker.addMountDirectory(tmp, tmp_docker)

        docker.run()
        # docker.runTest();

        if docker.getExitValue() != 0:
            error("error while demultiplexing run " + run_id, 'Error while demultiplexing run (exit code: ' +
                  str(docker.getExitValue()) + ').\nCommand line:\n' + cmd, conf)

            # TODO add exception message in log file
            return False

    except Throwable, exp:
        error("error while running image Docker ", common.exception_msg(exp, conf), conf)
        return False

    # The output directory must be read only
    cmd = 'cp ' + tmp + '/bcl2fastq_output_' + run_id + '.* ' + fastq_output_dir
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while setting read only the output fastq directory for run " + run_id,
              'Error while setting read only the output fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    cmd = 'find ' + fastq_output_dir + ' -type f -name "*.fastq.*" -exec chmod ugo-w {} \; '
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while setting read only the output fastq directory for run " + run_id,
              'Error while setting read only the output fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    return True


def is_confirmed_fastq_existence(fastq_output_dir):
    """ Archive demultplexing statistics results file.

    Arguments:
        fastq_output_dir: fastq directory to save result on demultiplexing

        Return true if define at least on FASTQ files
    """

    fastq_files = glob.glob(fastq_output_dir + "/*fastq*")

    return len(fastq_files) > 0


def archive_demux_stat(run_id, fastq_output_dir, reports_data_path, basecall_stats_file,
                       basecall_stats_prefix, samplesheet_csv_path, conf):
    """ Archive demultplexing statistics results file.

    Arguments:
        run_id: The run id
        fastq_output_dir: fastq directory to save result on demultiplexing
        reports_data_path: directory to save archives
        basecall_stats_file: file to archive
        basecall_stats_prefix: prefix file to archive
        samplesheet_csv_path: sample sheet in csv
        conf: configuration dictionary
    """

    archive_run_dirname = basecall_stats_prefix + run_id
    archive_run_dir = reports_data_path + '/' + archive_run_dirname
    archive_run_tar_file = reports_data_path + '/' + basecall_stats_file

    # With bcl2fastq 2
    cmd_list = []
    cmd_list.extend(['cd', fastq_output_dir, '&&'])
    cmd_list.extend(['mkdir', archive_run_dir, '&&'])
    cmd_list.extend(['cp', '-r', 'Reports', 'Stats', 'InterOp', samplesheet_csv_path, archive_run_dir, '&&'])
    cmd_list.extend(['tar cjf', archive_run_tar_file, '-C', reports_data_path, archive_run_dirname])

    cmd = " ".join(cmd_list)

    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while saving the basecall stats file for " + run_id,
              'Error while saving the basecall stats files.\nCommand line:\n' + cmd, conf)
        return False

    # Set read only basecall stats archives files
    os.chmod(archive_run_tar_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)

    return True


def archive_samplesheet(run_id, samplesheet_xls_path, samplesheet_csv_path, conf):
    """ Archive sample sheet file in archive sample sheet directory.

    Arguments:
        run_id: The run id
        samplesheet_xls_path: sample sheet path in format xls if exists
        samplesheet_csv_path: sample sheet path in format csv
        conf: configuration dictionary
    """

    # Add samplesheet to the archive of samplesheets
    if common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xls', conf):
        cmd = 'cp ' + samplesheet_xls_path + ' ' + conf[TMP_PATH_KEY] + \
              ' && cd ' + conf[TMP_PATH_KEY] + \
              ' && zip -q ' + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '/' + conf[
                  BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY] + 's.zip ' + \
              os.path.basename(samplesheet_csv_path) + ' ' + os.path.basename(samplesheet_xls_path)
    else:
        cmd = 'cd ' + conf[TMP_PATH_KEY] + \
              ' && zip -q ' + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '/' + conf[
                  BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY] + 's.zip ' + \
              os.path.basename(samplesheet_csv_path)

    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while archiving the sample sheet file for " + run_id,
              'Error while archiving the sample sheet file for.\nCommand line:\n' + cmd, conf)
        return False

    # Remove temporary samplesheet files
    os.remove(samplesheet_csv_path)
    if common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xls', conf):
        os.remove(conf[TMP_PATH_KEY] + '/' + os.path.basename(samplesheet_xls_path))

    return True


def demux(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()
    common.log('INFO', 'Demux step: start', conf)

    reports_data_base_path = conf[REPORTS_DATA_PATH_KEY]
    reports_data_path = common.get_report_run_data_path(run_id, conf)

    samplesheet_filename = build_samplesheet_filename(run_id, conf)

    input_samplesheet_xls_path = conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '/' + samplesheet_filename + '.xls'

    input_run_data_path = common.get_input_run_data_path(run_id, conf)

    if input_run_data_path is None:
        return False

    fastq_output_dir = conf[FASTQ_DATA_PATH_KEY] + '/' + run_id

    basecall_stats_prefix = 'basecall_stats_'
    basecall_stats_file = basecall_stats_prefix + run_id + '.tar.bz2'

    # Check if root input bcl data directory exists
    if not os.path.exists(input_run_data_path):
        error("Basecalling data directory does not exists",
              "Basecalling data directory does not exists: " + str(input_run_data_path), conf)
        # return False

    # Check if root input fastq data directory exists
    if not common.is_dir_exists(FASTQ_DATA_PATH_KEY, conf):
        error("Fastq data directory does not exists",
              "Fastq data directory does not exists: " + conf[FASTQ_DATA_PATH_KEY], conf)
        return False

    # Check if bcl2fastq samplesheets path exists
    if not common.is_dir_exists(BCL2FASTQ_SAMPLESHEETS_PATH_KEY, conf):
        error("bcl2fastq sample sheets directory does not exists",
              "Bcl2fastq sample sheets does not exists: " + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY], conf)
        return False

    # Check if bcl2fastq basedir path exists
    if not common.is_conf_value_equals_true(BCL2FASTQ_USE_DOCKER_KEY, conf):
        if not common.is_dir_exists(BCL2FASTQ_PATH_KEY, conf):
            error("bcl2fastq directory path does not exists",
                  "Bcl2fastq path does not exists: " + conf[BCL2FASTQ_PATH_KEY], conf)
            return False

    # Check if temporary directory exists
    if not common.is_dir_exists(TMP_PATH_KEY, conf):
        error("Temporary directory does not exists",
              "Temporary directory does not exists: " + conf[TMP_PATH_KEY], conf)
        return False

    # Check if reports_data_path exists
    if not os.path.exists(reports_data_base_path):
        error("Report directory does not exists",
              "Report directory does not exists: " + reports_data_base_path, conf)
        return False

    # Create if not exists report directory for the run
    if not os.path.exists(reports_data_path):
        os.mkdir(reports_data_path)

    # Check if basecall stats archive exists
    if os.path.exists(reports_data_path + '/' + basecall_stats_file):
        error('Basecall stats archive already exists for run ' + run_id,
              'Basecall stats archive already exists for run ' + run_id + ': ' + basecall_stats_file, conf)
        return False

    # Check if the output directory already exists
    if os.path.exists(fastq_output_dir):
        error("Fastq output directory already exists for run " + run_id,
              'The fastq output directory already exists for run ' + run_id + ': ' + fastq_output_dir, conf)
        return False

    # Compute disk usage and disk free to check if enough disk space is available
    input_path_du = common.du(input_run_data_path)
    output_df = common.df(conf[FASTQ_DATA_PATH_KEY])
    du_factor = float(conf[DEMUX_SPACE_FACTOR_KEY])
    space_needed = input_path_du * du_factor

    common.log("WARNING", "Demux step: input disk usage: " + str(input_path_du), conf)
    common.log("WARNING", "Demux step: output disk free: " + str(output_df), conf)
    common.log("WARNING", "Demux step: space needed: " + str(space_needed), conf)

    common.log("CONFIG", "Bcl2fastq Docker mode: " + str(
        common.is_conf_value_equals_true(Settings.BCL2FASTQ_USE_DOCKER_KEY, conf)), conf)

    # Check if free space is available
    if output_df < space_needed:
        error("Not enough disk space to perform demultiplexing for run " + run_id,
              "Not enough disk space to perform demultiplexing for run " + run_id +
              '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) + ' is needed (factor x' + str(
                  du_factor) + ') on ' + fastq_output_dir + '.', conf)
        return False

    # Check and convert if useful samplesheet
    samplesheet_csv_path, samplesheet_warnings = check_samplesheet(run_id, input_run_data_path, samplesheet_filename, conf)
    if not samplesheet_csv_path:
        return False

    # Check format compression bcl2fastq
    if not common.is_fastq_compression_format_valid(conf):
        error("error while checking FASTQ compression format",
              "Invalid FASTQ compression format: " + conf[BCL2FASTQ_COMPRESSION_KEY], conf)
        return False

    # Get the number of mismatches
    nb_mismatch = conf[BCL2FASTQ_MISMATCHES_KEY]

    # Run demultiplexing
    if common.is_conf_value_equals_true(Settings.BCL2FASTQ_USE_DOCKER_KEY, conf):
        # With image docker
        if not demux_run_with_docker(run_id, input_run_data_path, fastq_output_dir, samplesheet_csv_path, nb_mismatch, conf):
            return False
    else:
        if not demux_run_standalone(run_id, input_run_data_path, fastq_output_dir, samplesheet_csv_path, nb_mismatch, conf):
            return False

    if not is_confirmed_fastq_existence(fastq_output_dir):
        error("error with bcl2fastq execution for run " + run_id,
              "Error with bcl2fastq execution for run " + run_id + " none FASTQ files found in " + fastq_output_dir,
              conf)
        return False

    # Copy samplesheet to output directory
    cmd = "cp -p " + samplesheet_csv_path + ' ' + fastq_output_dir
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while copying sample sheet file to the fastq directory for run " + run_id,
              'Error while copying sample sheet file to fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    # Create archives on demultiplexing statistics
    if not archive_demux_stat(run_id, fastq_output_dir, reports_data_path, basecall_stats_file,
                              basecall_stats_prefix, samplesheet_csv_path, conf):
        return False

    # Archive samplesheet
    if not archive_samplesheet(run_id, input_samplesheet_xls_path, samplesheet_csv_path, conf):
        return False

    # Create index.hml file
    common.create_html_index_file(conf, run_id, [Settings.HISEQ_STEP_KEY, Settings.DEMUX_STEP_KEY])

    df_in_bytes = common.df(fastq_output_dir)
    du_in_bytes = common.du(fastq_output_dir)
    df = df_in_bytes / (1024 * 1024 * 1024)
    du = du_in_bytes / (1024 * 1024 * 1024)

    common.log("WARNING", "Demux step: output disk free after demux: " + str(df_in_bytes), conf)
    common.log("WARNING", "Demux step: space used by demux: " + str(du_in_bytes), conf)

    duration = time.time() - start_time

    msg = 'End of demultiplexing for run ' + run_id + '.' + \
          '\nJob finished at ' + common.time_to_human_readable(time.time()) + \
          ' with no error in ' + common.duration_to_human_readable(duration) + '.\n\n' + \
          'Fastq files for this run ' + \
          'can be found in the following directory:\n  ' + fastq_output_dir

    if samplesheet_warnings.size() > 0:
        msg += '\n\nSample sheet warnings:'
        for warn in samplesheet_warnings:
            msg += "\n  - " + warn

    # Add path to report if reports.url exists
    if common.is_conf_key_exists(REPORTS_URL_KEY, conf):
        msg += '\n\nRun reports can be found at following location:\n  ' + conf[REPORTS_URL_KEY] + '/' + run_id

    msg += '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df)

    common.send_msg('[Aozan] End of demultiplexing for run ' + run_id, msg, False, conf)
    common.log('INFO', 'Demux step: success in ' + common.duration_to_human_readable(duration), conf)

    return True
