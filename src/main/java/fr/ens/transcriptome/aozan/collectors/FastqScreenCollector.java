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

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class manages the execution of Fastq Screen for a full run according to
 * the properties defined in the configuration file Aozan, which define the list
 * of references genomes.
 * @author Sandrine Perrin
 */
public class FastqScreenCollector extends AbstractFastqCollector {

  public static final String COLLECTOR_NAME = "fastqscreen";
  public static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";
  private static List<String> listGenomes;

  private FastqScreen fastqscreen;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector
   * @return list of names collector
   */
  @Override
  public String[] getCollectorsNamesRequiered() {

    List<String> result =
        Lists.newArrayList(super.getCollectorsNamesRequiered());

    result.add(UncompressFastqCollector.COLLECTOR_NAME);

    return result.toArray(new String[] {});

  }

  // @Override
  // public AbstractFastqProcessThread processFile(final RunData data,
  // final FastqSample fastqSample) throws AozanException {
  // return null;
  //
  // }

  /**
   * Configure fastqScreen with properties from file aozan.conf
   * @param properties
   */
  @Override
  public void configure(/* final */Properties properties) {
    System.out.println("fsq configure");

    super.configure(properties);

    this.fastqscreen = new FastqScreen(properties);

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    for (String genome : s.split(properties.getProperty(KEY_GENOMES))) {
      listGenomes.add(genome);
    }
  }

  @Override
  public AbstractFastqProcessThread collectSample(RunData data,
      final FastqSample fastqSample, final File reportDir)
      throws AozanException {

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {
      return null;
    }

    // Create the thread object
    return new FastqScreenProcessThread(fastqSample, fastqscreen, listGenomes,
        reportDir, paired);
  }

  //
  // Getters & Setters
  //

  public int getThreadsNumber() {
    return 1;
  }

  public void setThreadsNumber(int n) {
    return;
  }

  //
  // Constructor
  //

  /**
   * Public constructor for FastqScreenCollector
   */
  public FastqScreenCollector() {
    listGenomes = new ArrayList<String>();
  }

}
