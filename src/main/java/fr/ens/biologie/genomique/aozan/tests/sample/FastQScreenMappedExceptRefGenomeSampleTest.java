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

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.FastqScreenCollector;

/**
 * The class adds in the qc report html one result from fastqScreen for each
 * sample. It print the percent of reads which mapped on at least one genomes
 * except the genome sample.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastQScreenMappedExceptRefGenomeSampleTest
    extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final int read, final int readSample,
      final int sampleId, final int lane, final boolean undetermined) {

    return "fastqscreen.sample"
        + sampleId + ".read" + readSample + ".mappedexceptgenomesample";
  }

  @Override
  protected boolean isValuePercent() {
    return true;
  }

  @Override
  protected Class<?> getValueType() {
    return Double.class;
  }

  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value.doubleValue() * 100.0;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public FastQScreenMappedExceptRefGenomeSampleTest() {

    super("sample.fastqscreen.mapped.except.ref.genome.percent",
        "FastQ Screen mapped except. Ref. Genome",
        "FastQ Screen mapped except. Ref. Genome", "%");
  }
}
