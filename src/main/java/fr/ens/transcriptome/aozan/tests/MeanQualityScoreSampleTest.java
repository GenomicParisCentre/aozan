/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.tests;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;
import fr.ens.transcriptome.aozan.util.Interval;

/**
 * This class define a percent quality mean score sample test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class MeanQualityScoreSampleTest extends AbstractSampleTest {

  private Interval interval;

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {FlowcellDemuxSummaryCollector.COLLECTOR_NAME};
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int lane, final String sampleName) {

    final String prefix;

    if (sampleName == null)
      prefix = "demux.lane" + lane + ".sample.lane" + lane + ".read" + readSample;
    else
      prefix = "demux.lane" + lane + ".sample." + sampleName + ".read" + readSample;

    final long qualityScoreSum = data.getLong(prefix + ".pf.quality.score.sum");
    final long yield = data.getLong(prefix + ".pf.yield");

    final double mean = (double) qualityScoreSum / (double) yield;

    if (interval == null)
      return new TestResult(mean);

    return new TestResult(this.interval.isInInterval(mean) ? 9 : 0, mean);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public MeanQualityScoreSampleTest() {
    super("meanqualityscore", "", "Mean Quality Score Base PF", "");
    this.interval = null;
  }

}
