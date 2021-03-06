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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.tests.pooledsample;

import static fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatisticsCollector.COLLECTOR_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatisticsCollector;
import fr.ens.biologie.genomique.aozan.tests.pooledsample.AbstractSimpleSampleTest;

/**
 * The class define a test the maximum on raw clusters on samples in a project.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class PFClusterRecoveryCountPooledSampleTest
    extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatisticsCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final int pooledSampleId) {

    return COLLECTOR_PREFIX
        + ".pooledsample" + pooledSampleId + ".pf.cluster.recovery.sum";
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
  public PFClusterRecoveryCountPooledSampleTest() {

    super("pooledsample.pf.cluster.recovery.count", "PF Cluster Recovery",
        "PF Cluster Recovery");
  }

}
