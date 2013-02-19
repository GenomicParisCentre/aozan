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

package fr.ens.transcriptome.aozan.collectors;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.FastqScreenDemo;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.RunDataGenerator;
import fr.ens.transcriptome.aozan.io.FastqStorage;

abstract class AbstractFastqCollector implements Collector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected FastqStorage fastqStorage;

  public static final String COLLECTOR_NAME = "";

  public static final String KEY_READ_COUNT = "run.info.read.count";
  public static final String KEY_READ_X_INDEXED = "run.info.read";
  public static final String COMPRESSION_EXTENSION = "fastq.bz2";

  protected String casavaOutputPath;
  protected String qcReportOutputPath;
  protected String tmpPath;

  // mode threaded
  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  protected List<AbstractFastqProcessThread> threads;
  protected List<Future<? extends AbstractFastqProcessThread>> futureThreads;
  protected ExecutorService executor;
  private boolean modeMonoThreaded;

  protected boolean paired = false;

  @Override
  /**
   * Get the name of the collector
   * @return the name of the collector
   */
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  /**
   * Get the name of the collectors required to run this collector.
   * @return an array of String with the name of the required collectors
   */
  public String[] getCollectorsNamesRequiered() {
    return new String[] {RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME};
  }

  @Override
  /**
   * Configure the collector with the path of the run data
   * @param properties object with the collector configuration
   */
  public void configure(Properties properties) {

    this.casavaOutputPath =
        properties.getProperty(RunDataGenerator.CASAVA_OUTPUT_DIR);
    System.out.println("casava output " + this.casavaOutputPath);

    this.qcReportOutputPath =
        properties.getProperty(RunDataGenerator.QC_OUTPUT_DIR);
    this.tmpPath = properties.getProperty(RunDataGenerator.TMP_DIR);
    this.fastqStorage = FastqStorage.getInstance(this.tmpPath);

    System.out.println("Abstract configure ");
    // + " " + casavaOutputPath + " " + qcOutputDir + " " + tmpPath);

    this.modeMonoThreaded = this.getNumberThreads() == 1;

    if (!modeMonoThreaded) {

      if (properties.containsKey("qc.conf.fastqc.threads")) {

        try {
          int confThreads =
              Integer.parseInt(properties.getProperty("qc.conf.fastqc.threads")
                  .trim());
          if (confThreads > 0)
            this.setNumberThreads(confThreads);

        } catch (NumberFormatException e) {
        }

        // Create the list for threads
        this.threads = Lists.newArrayList();
        this.futureThreads = Lists.newArrayList();

        // Create executor service
        this.executor = Executors.newFixedThreadPool(this.getNumberThreads());
      }
    }

  }

  @Override
  /**
   * Collect data : browse data for call concrete collector,
   * @param data result data object
   * @throws AozanException if an error occurs while collecting data
   */
  public void collect(RunData data) throws AozanException {

    System.out.println("abstract collect ");

    // TODO to remove
    data = FastqScreenDemo.getRunData();

    final int laneCount = data.getInt("run.info.flow.cell.lane.count");

    // mode paired or single-end present in Rundata
    final int readCount = data.getInt(KEY_READ_COUNT);
    final boolean lastReadIndexed =
        data.getBoolean(KEY_READ_X_INDEXED + readCount + ".indexed");
    int readSample = 0;

    paired = readCount > 1 && !lastReadIndexed;

    for (int read = 1; read <= readCount - 1; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;
      readSample++;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {

          // Get the sample index
          String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Get project name
          String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          collectSample(data, read, lane, projectName, sampleName, index,
              readSample);

        } // sample
      }// lane
    }// read

    if (!this.modeMonoThreaded) {
      System.out.println("multithread " + futureThreads.size());
      // Wait for threads
      waitThreads(futureThreads, executor);

      // Add results of the threads to the data object
      for (AbstractFastqProcessThread sft : threads)
        data.put(sft.getResults());
    }
  }

  abstract public void collectSample(/* final */RunData data, final int read,
      final int lane, final String projectName, final String sampleName,
      final String index, final int readSample) throws AozanException;

  abstract public int getNumberThreads();

  abstract public void setNumberThreads(final int numberThreads);

  /**
   * Wait the end of the threads.
   * @param threads list with the threads
   * @param executor the executor
   * @throws AozanException if an error occurs while executing a thread
   */
  private void waitThreads(
      final List<Future<? extends AbstractFastqProcessThread>> threads,
      final ExecutorService executor) throws AozanException {

    int samplesNotProcessed = 0;

    // Wait until all samples are processed
    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (InterruptedException e) {
        // LOGGER.warning("InterruptedException: " + e.getMessage());
      }

      samplesNotProcessed = 0;

      for (Future<? extends AbstractFastqProcessThread> fst : threads) {

        if (fst.isDone()) {

          try {

            final AbstractFastqProcessThread st = fst.get();

            if (!st.isSuccess()) {

              // Close the thread pool
              executor.shutdownNow();

              // Wait the termination of current running task
              executor
                  .awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);

              // Return error Step Result
              throw new AozanException(st.getException());
            }

          } catch (InterruptedException e) {
            // LOGGER.warning("InterruptedException: " + e.getMessage());
          } catch (ExecutionException e) {
            throw new AozanException(e);
          }

        } else {
          samplesNotProcessed++;
        }

      }

    } while (samplesNotProcessed > 0);

    // Close the thread pool
    executor.shutdown();
  }

  //
  // Constructor
  //

  public AbstractFastqCollector() {

  }
}
