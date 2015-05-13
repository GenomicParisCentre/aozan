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

package fr.ens.transcriptome.aozan.illumina.io;

import java.io.IOException;

import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;

/**
 * This interface define a writer for Casava designs.
 * @since 1.1
 * @author Laurent Jourdren
 */
public interface CasavaDesignWriter {

  /**
   * Write a design.
   * @param design design to write
   * @throws IOException if an error occurs while writing the design
   */
  void writer(SampleSheet design) throws IOException;

}
