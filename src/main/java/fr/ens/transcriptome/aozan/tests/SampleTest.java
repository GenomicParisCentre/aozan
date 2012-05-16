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

/**
 * This interface define sample test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface SampleTest extends AozanTest {

  /**
   * Do a test.
   * @param data result object
   * @param read index of read
   * @param readSample index of read without indexed reads
   * @param lane the index of the lane
   * @param sample name of the sample to test. If null test must process
   *          undetermined indexes reads
   * @return a TestResult object with the result of the test
   */
  public TestResult test(RunData data, int read, int readSample, int lane,
      String sampleName);

}
