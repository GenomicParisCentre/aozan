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

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import uk.ac.babraham.FastQC.Modules.PerSequenceQualityScores;

/**
 * This class define a sample test for FastQC per sequence quality scores
 * module.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class FastQCPerSequenceQualityScoresSampleTest
    extends AbstractFastQCSampleTest {

  private static final String FASTQC_MODULE_NAME =
      new PerSequenceQualityScores().name();

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
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
  public FastQCPerSequenceQualityScoresSampleTest() {

    super("sample.fastqc.per.sequence.quality.scores",
        "Per Sequence Quality Scores", "Per Sequence Quality Scores");
  }

}
