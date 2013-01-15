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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import uk.ac.bbsrc.babraham.FastQC.Modules.PerSequenceQualityScores;
import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define a sample test for FastQC per sequence quality scores
 * module.
 * @author Laurent Jourdren
 */
public class PerSequenceQualityScoresFastQCSampleTest extends
    AbstractFastQCSampleTest {

  private static final String FASTQC_MODULE_NAME =
      new PerSequenceQualityScores().name();

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {
    return Collections.singletonList((AozanTest) this);
  }

  @Override
  protected String getQCModuleName() {

    return FASTQC_MODULE_NAME;
  }

  @Override
  protected int getHTMLAnchorIndex() {

    return 2;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PerSequenceQualityScoresFastQCSampleTest() {

    super("persequencequalityscores", "per sequence quality scores",
        "Per sequence quality scores");
  }

}
