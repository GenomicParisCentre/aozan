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

package fr.ens.transcriptome.aozan.collectors.interop;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.collectors.interop.ReadsData.ReadData;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;

/**
 * The abstract class define same action on all metric collector.
 * @author Sandrine Perrin
 * @since 1.4
 */
abstract class AbstractMetricsCollector implements Collector {

  /** The dir inter op path. */
  private String dirInterOpPath = null;

  /** The reads data. */
  private ReadsData readsData = null;

  /** The lanes count. */
  private int lanesCount;

  /** The reads count. */
  private int readsCount;

  @Override
  public boolean isStatisticCollector() {
    return false;
  }

  /**
   * Get the name of the collectors required to run this collector.
   * @return a list of String with the name of the required collectors
   */
  @Override
  public List<String> getCollectorsNamesRequiered() {
    return Collections.unmodifiableList(Lists
        .newArrayList(RunInfoCollector.COLLECTOR_NAME));
  }

  @Override
  public void configure(Properties properties) {
    final String RTAOutputDirPath = properties.getProperty(QC.RTA_OUTPUT_DIR);
    this.dirInterOpPath = RTAOutputDirPath + "/InterOp/";
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Build map on all reads in run
    this.readsData = new ReadsData(data);

    this.lanesCount = data.getLaneCount();
    this.readsCount = data.getReadCount();
  }

  /**
   * Define the readNumber corresponding to the cycle.
   * @param cycleNumber the cycle number
   * @return readNumber
   * @throws AozanException if cycleNumber invalid or if no read found
   */
  public int getReadFromCycleNumber(final int cycleNumber)
      throws AozanException {

    return readsData.getReadFromCycleNumber(cycleNumber);

  }

  /**
   * Remove temporary files.
   */
  @Override
  public void clear() {
  }

  //
  // Getter
  //

  /**
   * Gets the directory interOp file path.
   * @return the directory interOp file path.
   */
  public String getInterOpDirPath() {
    if (this.dirInterOpPath == null) {
      throw new EoulsanRuntimeException(
          "Run metrics collector, path to binary file is not define. Configuration skipping.");
    }

    return this.dirInterOpPath;
  }

  /**
   * Gets the lanes count.
   * @return the lanes count
   */
  public int getLanesCount() {
    return this.lanesCount;
  }

  /**
   * Gets the reads count.
   * @return the reads count
   */
  public int getReadsCount() {
    return this.readsCount;
  }

  /**
   * Gets the reads data.
   * @return the reads data
   */
  public ReadsData getReadsData() {
    return readsData;
  }

  /**
   * Gets the read data.
   * @param read the read
   * @return the read data
   */
  public ReadData getReadData(final int read) {

    return readsData.getReadData(read);
  }
}
