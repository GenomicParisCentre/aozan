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

package fr.ens.biologie.genomique.aozan.fastqc;

import uk.ac.babraham.FastQC.Sequence.Contaminant.Contaminant;
import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

/**
 * This class define a object which handles result xml file blastn for one
 * sequence query.
 * @since 1.2
 * @author Sandrine Perrin
 */
class BlastResultHit {

  private static final String LINK_NCBI_BLASTN =
      "\"http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&amp;PAGE=Nucleotides&amp;DATABASE=nr&amp;QUERY=";

  // Parameters for return only the best hit
  private static final int MIN_IDENTITY_EXPECTED = 100;
  private static final int MAX_QUERYCOVERT_EXPECTED = 0;

  private final boolean htmlTypeOutput;
  private final String sequence;
  private int queryLength;
  private String result;
  private String hspEValue;
  private int countHits;
  private int prcIdentity;
  private int queryCover;

  private boolean isNull = true;

  /**
   * Generate hit data.
   * @param hitNum hit number
   * @param result result string
   * @param hspEValue HSP e-value
   * @param hspIdentity HSP identity
   * @param hspAlignLen HSP alignment length
   * @param countHits number hits retrieved by blast
   * @param queryLength number base in sequence query
   */
  private void addHitData(final int hitNum, final String result,
      final String hspEValue, final int hspIdentity, final int hspAlignLen,
      final int countHits, final int queryLength) {

    // No hit found for this sequence
    if (hitNum == 0) {
      return;
    }

    this.queryLength = queryLength;
    this.countHits = countHits;

    this.result = result;
    this.hspEValue = hspEValue;
    final int countGap = queryLength - hspAlignLen;

    this.prcIdentity = (int) ((double) hspIdentity / this.queryLength * 100);

    this.queryCover = (int) ((double) countGap / this.queryLength * 100);

    this.isNull = false;

  }

  /**
   * Create an object contaminant hit.
   * @return object contaminantHit
   */
  public ContaminantHit toContaminantHit() {
    final String resultContaminant;
    if (this.htmlTypeOutput) {
      resultContaminant = contaminantHitToHtmlType();
    } else {
      resultContaminant = contaminantHitToTextType();
    }

    final Contaminant cont = new Contaminant(resultContaminant, "");

    // Override method
    return new ContaminantHit(cont, 1, this.queryLength, this.prcIdentity) {

      @Override
      public String toString() {

        return cont.name();
      }
    };
  }

  /**
   * Return result search on contaminant hit in text type to display in control
   * quality report.
   * @return result search on contaminant hit in text type.
   */
  private String contaminantHitToTextType() {

    String name = "Search with Blastn, "
        + " First hit on " + (this.countHits > 100 ? "+100" : this.countHits)
        + ": " + this.result + " Evalue=" + this.hspEValue + ", " + " Ident="
        + this.prcIdentity + "%," + " QueryCovergap=" + this.queryCover + "%";

    // Return only the best hit
    if (this.prcIdentity < MIN_IDENTITY_EXPECTED
        || this.queryCover > MAX_QUERYCOVERT_EXPECTED) {
      return "No hit";
    }

    return name;

  }

  /**
   * Return result search on contaminant hit in text type to display in control
   * quality report.
   * @return result search on contaminant hit in text type.
   */
  private String contaminantHitToHtmlType() {

    final String name = "Search with Blastn, <a href="
        + LINK_NCBI_BLASTN + this.sequence + "\""
        + " target=\"_blank\">more detail</a>" + " First hit on "
        + (this.countHits > 100 ? "+100" : this.countHits) + ": " + " <br/>"
        + this.result + " <br/>" + " Evalue=" + this.hspEValue + ", "
        + " Ident=" + this.prcIdentity + "%," + " QueryCovergap="
        + this.queryCover + "%";

    // Return only the best hit
    if (this.prcIdentity < MIN_IDENTITY_EXPECTED
        || this.queryCover > MAX_QUERYCOVERT_EXPECTED) {
      return "No hit";
    }

    return name;
  }

  public boolean isNull() {
    return this.isNull;
  }

  //
  // Constructor
  //

  /**
   * Public constructor. Object contains all information for one blast response
   * to a query.
   * @param sequence query blast
   */
  public BlastResultHit(final String sequence) {

    this.sequence = sequence;
    this.htmlTypeOutput = false;

  }

  /**
   * Public constructor. Object contains all information for one blast response
   * to a query.
   * @param hitNum hit number
   * @param result result string
   * @param hspEValue HSP e-value
   * @param hspIdentity HSP identity
   * @param hspAlignLen HSP alignment length
   * @param countHits number hits retrieved by blast
   * @param queryLength number base in sequence query
   * @param sequence query blast
   * @param htmlTypeOutput true if output in html type, otherwise in text type
   */
  public BlastResultHit(final int hitNum, final String result,
      final String hspEValue, final int hspIdentity, final int hspAlignLen,
      final int countHits, final int queryLength, final String sequence,
      final boolean htmlTypeOutput) {

    this.sequence = sequence;
    this.htmlTypeOutput = htmlTypeOutput;

    addHitData(hitNum, result, hspEValue, hspIdentity, hspAlignLen, countHits,
        queryLength);
  }

}
