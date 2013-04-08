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

import fr.ens.transcriptome.aozan.collectors.ReadCollector;

/**
 * This class define a test on first cycle intensity.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FirstCycleIntensityPFLaneTest extends AbstractSimpleLaneTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return Lists.newArrayList(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  @Override
  protected String getKey(final int read, final boolean indexedRead,
      final int lane) {

    return "read" + read + ".lane" + lane + ".first.cycle.int.pf";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public FirstCycleIntensityPFLaneTest() {

    super("firstcycleintensity", "", "First cyle intensity");
  }

}
