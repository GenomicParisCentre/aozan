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

package fr.ens.transcriptome.aozan.fastqscreen;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.LocalEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

/**
 * This class execute fastqscreen pair-end mode or single-end
 * @author Sandrine Perrin
 */
public class FastqScreen {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /** Timer */
  private final Stopwatch timer = new Stopwatch();

  protected static final String COUNTER_GROUP = "fastqscreen";

  private static final String KEY_TMP_DIR = "tmp.dir";
  private static final String KEY_GENOMES_DESC_PATH =
      "qc.conf.settings.genomes.desc.path";
  private static final String KEY_MAPPERS_INDEXES_PATH =
      "qc.conf.settings.mappers.indexes.path";
  private static final String KEY_GENOMES_PATH = "qc.conf.settings.genomes";

  private Properties properties;

  /**
   * Mode pair-end : execute fastqscreen count
   * @param fastqRead1 fastq file input for mapper
   * @param fastqRead2 fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @param genomeSample genome reference corresponding to sample
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead,
      final FastqSample fastqSample, final List<String> genomes,
      final String genomeSample) throws AozanException {

    return this.execute(fastqRead, null, fastqSample, genomes, genomeSample);

  }

  /**
   * Mode single-end : execute fastqscreen calcul
   * @param fastqFile fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @param genomeSample genome reference corresponding to sample
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead1,
      final File fastqRead2, final FastqSample fastqSample,
      final List<String> genomes, final String genomeSample)
      throws AozanException {

    timer.start();

    String tmpDir = properties.getProperty(KEY_TMP_DIR);

    FastqScreenPseudoMapReduce pmr = new FastqScreenPseudoMapReduce();
    pmr.setMapReduceTemporaryDirectory(new File(tmpDir));

    try {

      if (fastqRead2 == null)
        pmr.doMap(fastqRead1, genomes, genomeSample, properties);
      else
        pmr.doMap(fastqRead1, fastqRead2, genomes, genomeSample, properties);

      LOGGER.fine("Fastqscreen : step map for "
          + fastqSample.getName() + " in mode "
          + (fastqRead2 == null ? "single" : "paired") + " in "
          + toTimeHumanReadable(timer.elapsedMillis()));

      timer.reset();
      timer.start();

      pmr.doReduce(new File(tmpDir + "/outputDoReduce.txt"));

      LOGGER.fine("Fastqscreen : step reduce for "
          + fastqSample.getName() + " in mode "
          + (fastqRead2 == null ? "single" : "paired") + " in "
          + toTimeHumanReadable(timer.elapsedMillis()));

      // remove temporary output file use in map-reduce step
      new File(tmpDir + "/outputDoReduce.txt").delete();

    } catch (IOException e) {
      throw new AozanException(e.getMessage());

    } catch (BadBioEntryException bad) {
      throw new AozanException(bad.getMessage());

    } finally {
      timer.stop();
    }

    return pmr.getFastqScreenResult();
  }

  //
  // Constructor
  //

  /**
   * Public constructor of fastqscreen. Initialization of settings of Eoulsan
   * necessary for use of mapper index.
   * @param properties properties defines in configuration of aozan
   */
  public FastqScreen(final Properties properties) {

    this.properties = properties;

    try {
      // init EoulsanRuntime, it is necessary to use the implementation of
      // bowtie in Eoulsan
      LocalEoulsanRuntime.initEoulsanRuntimeForExternalApp();
      Settings settings = EoulsanRuntime.getSettings();

      settings.setGenomeDescStoragePath(properties
          .getProperty(KEY_GENOMES_DESC_PATH));
      settings.setGenomeMapperIndexStoragePath(properties
          .getProperty(KEY_MAPPERS_INDEXES_PATH));
      settings.setGenomeStoragePath(properties.getProperty(KEY_GENOMES_PATH));

    } catch (IOException e) {
      e.printStackTrace();

    } catch (EoulsanException ee) {
      ee.printStackTrace();
    }

  }
}