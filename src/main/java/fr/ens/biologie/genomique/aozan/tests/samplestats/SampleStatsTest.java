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
package fr.ens.biologie.genomique.aozan.tests.samplestats;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestResult;

/**
 * This interface define a test on lane.
 * @since 1.3
 * @author Laurent Jourdren
 */
public interface SampleStatsTest extends AozanTest {

  /**
   * Do a test.
   * @param data result object
   * @param pooledSampleId pooled sample Id
   * @return a TestResult object with the result of the test
   */
  TestResult test(RunData data, int pooledSampleId);

}
