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

import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;

/**
 * This class define a passing filter clusters count test for samples.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PFClustersSampleTest extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return Lists.newArrayList(FlowcellDemuxSummaryCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final int read, final int readSample, final int lane,
      final String sampleName) {

    if (sampleName == null)
      return "demux.lane"
          + lane + ".sample.lane" + lane + ".read" + readSample
          + ".pf.cluster.count";

    return "demux.lane"
        + lane + ".sample." + sampleName + ".read" + readSample
        + ".pf.cluster.count";
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  //
  // Constuctor
  //

  /**
   * Public constructor.
   */
  public PFClustersSampleTest() {
    super("pfclusterssamples", "", "PF clusters");
  }

}
