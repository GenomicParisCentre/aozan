/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.FastQCCollector;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.collectors.UncompressFastqCollector;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class FastqscreenDemo {

  /** Logger */
  // private static final Logger LOGGER = Common.getLogger();
  private static final Logger LOGGER = Logger
      .getLogger(fr.ens.transcriptome.aozan.Globals.APP_NAME);

  /** Timer */
  private static Stopwatch timer = new Stopwatch();

  public static final Properties properties = new Properties();

  public static final String RESOURCE_ROOT =
      "/home/sperrin/Documents/FastqScreenTest/resources";
  public static final String SRC_RUN =
      "/home/sperrin/Documents/FastqScreenTest/runtest";
  public static final String TMP_DIR =
      "/home/sperrin/Documents/FastqScreenTest/tmp";
  public static final String ALIAS_GENOME_PATH =
      "/home/sperrin/Documents/FastqScreenTest/resources/alias_name_genome_fastqscreen.txt";

  public static final String GENOMES_DESC_PATH = RESOURCE_ROOT
      + "/genomes_descs";
  public static final String MAPPERS_INDEXES_PATH = RESOURCE_ROOT
      + "/mappers_indexes";
  public static final String GENOMES_PATH = RESOURCE_ROOT + "/genomes";

  public static final String AOZAN_CONF =
  // "/home/sperrin/Documents/FastqScreenTest/runtest/aozan_test.conf";
      "/home/sperrin/Documents/FastqScreenTest/runtest/aozan_without_fastqc.conf";

  public static RunData data = null;
  public static Map<String, FastqSample> prefixList;
  private static boolean paired = false;

  static String runId;
  static String date;
  static String qcDir;

  public static final void main(String[] args) {

    timer.start();

    try {
      Locale.setDefault(Locale.US);

      if (paired) {
        // run test pair-end
        // runId = "120830_SNL110_0055_AD16D9ACXX";
        runId = "130801_SNL110_0079_AD2CR3ACXX";
      } else {
        // run test single-end
        // runId = "120301_SNL110_0038_AD0EJRABXX";
        // runId = "121116_SNL110_0058_AC11HRACXX";
        // runId = "130214_SNL110_0062_AD1GKTACXX";
        // runId = "121219_SNL110_0059_AD1B1BACXX";
        // runId = "120615_SNL110_0051_AD102YACXX";
        // runId = "130326_SNL110_0066_AD1GG4ACXX";
        // runId = "130507_SNL110_0068_AD20WUACXX";
        // runId = "130617_SNL110_0071_AH0B40ADXX";
        // runId = "130604_SNL110_0070_AD26NTACXX";

        // ESSAI fastqscreen partial fastq
        // runId = "130726_SNL110_0078_AC2AJTACXX";
        // runId = "130709_SNL110_0075_AD2C79ACXX";
        // runId = "130715_SNL110_0076_AD2C4UACXX";
        runId = "130722_SNL110_0077_AH0NT2ADXX";
      }

      date = new SimpleDateFormat("yyMMdd").format(new Date());

      Common.initLogger(TMP_DIR + "/" + runId + "_aozan_test.log");
      LOGGER.setLevel(Level.ALL);

      System.out.println("Create report qc for run "
          + runId + "  " + FastqSample.VALUE);
      // reportQC();
      reportQC2();

      LOGGER.info("Runtime for demo with a run "
          + runId + " "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

    timer.stop();

  }

  public static void reportQC2() throws Exception {

    qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;
    String bclDir = "/home/sperrin/shares-net/sequencages/bcl/" + runId;
    String fastqDir = "/home/sperrin/shares-net/sequencages/fastq/" + runId;
    // String bclDir, String fastqDir, String qcDir, File tmpDir

    QC qc =
        new QC(getMapAozanConf(), qcDir, fastqDir, qcDir + "_qc_tmp", TMP_DIR,
            runId);

    // Compute report
    final QCReport report = qc.computeReport();

    // Save report data
    qc.writeXMLReport(report, qcDir + "_qc_tmp/" + runId + "_reportXmlFile.xml");

    // Save HTML report
    qc.writeReport(report, (String) null, qcDir
        + "_qc_tmp/" + runId + "_reportHtmlFile.html");
  }

  public static void reportQC() throws Exception {

    qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;

    String[] tabGenomes = {"phix" /* , "adapters2", "lsuref_dna", "ssuref" */};
    String genomes = "";
    for (String g : tabGenomes) {
      genomes += g + ",";
    }
    // remove last separator character ","
    genomes = genomes.substring(0, genomes.length() - 1);

    Collector uncompressFastqCollector = new UncompressFastqCollector();
    Collector fsqCollector = new FastqScreenCollector();
    Collector fqcCollector = new FastQCCollector();
    List<Collector> collectorList = new ArrayList<Collector>();
    collectorList.add(new RunInfoCollector());
    // collectorList.add(new ReadCollector());
    // collectorList.add(new DesignCollector());

    collectorList.add(fqcCollector);
    collectorList.add(fsqCollector);
    collectorList.add(uncompressFastqCollector);

    RunDataGenerator rdg = new RunDataGenerator(collectorList);

    // add new property for execute fastqscreen
    properties.put("qc.conf.fastqscreen.genomes", genomes);

    // data include in aozan.conf
    properties.put("fastq.data.path", SRC_RUN);
    properties.put("reports.data.path", SRC_RUN);
    properties.put("tmp.dir", TMP_DIR);
    properties.put("collect.done", "collect.done");

    properties.put(QC.CASAVA_OUTPUT_DIR, qcDir);
    properties.put(QC.QC_OUTPUT_DIR, qcDir + "_qc");

    // number threads used for fastqscreen is defined in aozan.conf
    properties.put("qc.conf.fastqc.threads", "4");

    // elements for configuration of eoulsanRuntime settings
    // use for create index
    properties.put("qc.conf.settings.genomes.desc.path", GENOMES_DESC_PATH);
    properties.put("qc.conf.settings.genomes", GENOMES_PATH);
    properties.put("qc.conf.settings.mappers.indexes.path",
        MAPPERS_INDEXES_PATH);
    properties.put("qc.conf.genome.alias.path", ALIAS_GENOME_PATH);

    File f = new File(qcDir + "/data-" + runId + ".txt");

    try {
      data = new RunData(f);

      // Create directory who save report collectors
      if (!new File(qcDir + "_qc_tmp").mkdir())
        System.out.println("Error creating report directory for Collectors");

      // Configure : create list of reference genome
      System.out.println("\nFASTQC COLLECTOR");
      fqcCollector.configure(properties);
      fqcCollector.collect(data);
      // System.exit(1);

      System.out.println("\nUNCOMPRESS COLLECTOR");
      uncompressFastqCollector.configure(properties);
      uncompressFastqCollector.collect(data);

      System.out.println("\nFASTQ SCREEN COLLECTOR");
      fsqCollector.configure(properties);
      fsqCollector.collect(data);

      System.out.println("\nCLEAR QC_REPORT COLLECTOR");
      // ((AbstractFastqCollector) fsqCollector).clear();

      // completed rundata
      // data =
      // new RunData(
      // new File(
      // "/home/sperrin/Bureau/data-120301_SNL110_0038_AD0EJRABXX_construit.txt"));
      //
      // System.out.println("restore data " + data.size());
      // rdg.collect();

      System.out.println("rundata Complet "
          + qcDir + "/RunDataCompleted_" + runId + ".txt");

      FileWriter fw =
          new FileWriter(
              new File(qcDir + "/RunDataCompleted_" + runId + ".txt"));
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(data.toString());
      bw.close();

      // Move action to the python code -> rename qc Report directory
      int n = new String(qcDir + "_qc_tmp").indexOf("_tmp");
      if (!new File(qcDir + "_qc_tmp").renameTo(new File(new String(qcDir
          + "_qc").substring(0, n))))
        System.out.println("Can not rename qc report directory.");

    } catch (Exception io) {
      System.out.println(io.getMessage());
    }

    // QC qc = new QC(getMapAozanConf(), TMP_DIR);
    // QCReport report = new QCReport(data, qc.laneTests, qc.sampleTests);
    //
    // // Save report data
    // qc.writeXMLReport(report, TMP_DIR + "/" + runId + "_reportXmlFile.xml");
    //
    // // Save HTML report
    // qc.writeReport(report, (String) null, TMP_DIR
    // + "/" + runId + "_reportHtmlFile.html");
  }

  public static RunData getRunData() {
    return data;
  }

  public static Properties getPropertiesDemo() {
    return properties;
  }

  public static Map<String, String> getMapAozanConf() {
    Map<String, String> conf = new LinkedHashMap<String, String>();
    String line;
    try {
      FileReader aozanConf = new FileReader(AOZAN_CONF);
      BufferedReader br = new BufferedReader(aozanConf);

      while ((line = br.readLine()) != null) {

        final int pos = line.indexOf('=');
        if (pos == -1)
          continue;

        final String key = line.substring(0, pos);
        final String value = line.substring(pos + 1);

        conf.put(key, value);
      }
      br.close();
      aozanConf.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    conf.put("qc.conf.read.xml.collector.used", "false");
    conf.put("qc.conf.cluster.density.ratio", "0.3472222");
    // conf.put("qc.conf.fastqscreen.genomes", "phix");

    // conf.put("qc.conf.ignore.paired.mode", "False");
    // parse fully fastq file
    // conf.put("qc.conf.max.reads.parsed", "-1");
    // use fully fastq for create tmp fastq file for fastqscreen
    // conf.put("qc.conf.reads.pf.used", "-1");

    System.out.println("genomes : "
        + conf.get("qc.conf.fastqscreen.genomes") + " mapping mode "
        + conf.get("qc.conf.ignore.paired.mode"));

    return conf;
  }

  public static void initLogger(final String logPath) throws SecurityException,
      IOException {

    final Logger aozanLogger =
        Logger.getLogger(fr.ens.transcriptome.aozan.Globals.APP_NAME);

    // Set default log level
    aozanLogger.setLevel(Level.FINEST);

    final Handler fh = new FileHandler(logPath, true);
    fh.setFormatter(new Formatter() {

      private final DateFormat df = new SimpleDateFormat("yyyy.MM.dd kk:mm:ss",
          Locale.US);

      public String format(final LogRecord record) {
        return record.getLevel()
            + "\t" + df.format(new Date(record.getMillis())) + "\t"
            + record.getMessage() + "\n";
      }
    });

    aozanLogger.setUseParentHandlers(false);

    // Remove default Handler
    aozanLogger.removeHandler(aozanLogger.getParent().getHandlers()[0]);

    aozanLogger.addHandler(fh);
    // eoulsanLogger.addHandler(fh);
  }
}
