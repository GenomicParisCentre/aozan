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

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.collectors.UndeterminedIndexesCollector;

/**
 * This class define a recoverable passing filter clusters count test for
 * samples.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class RecoverablePFClusterSampleTest extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(UndeterminedIndexesCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final int read, final int readSample, final int lane,
      final String sampleName) {

    if (sampleName == null)
      return "undeterminedindices.lane"
          + lane + ".recoverable.pf.cluster.count";

    return "undeterminedindices.lane"
        + lane + ".sample." + sampleName + ".recoverable.pf.cluster.count";
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public RecoverablePFClusterSampleTest() {
    super("recoverablepfclusterssamples", "", "Recoverable PF clusters");
  }

}
