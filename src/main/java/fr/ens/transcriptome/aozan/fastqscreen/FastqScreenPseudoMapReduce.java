/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 3 or
 * later and CeCILL. This should be distributed with the code.
 * If you do not have a copy, see:
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
 * or to join the Aozan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.storages.GenomeDescStorage;
import fr.ens.transcriptome.eoulsan.data.storages.SimpleGenomeDescStorage;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeMapperIndexer;
import fr.ens.transcriptome.eoulsan.util.PseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * @author sperrin
 */
public class FastqScreenPseudoMapReduce extends PseudoMapReduce {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected static final String COUNTER_GROUP = "reads_mapping";
  private static final int NB_STAT_VALUES = 5;
  private static final String KEY_NUMBER_THREAD = "qc.conf.fastqscreen.threads";
  private static final String KEY_TMP_DIR = "tmp.dir";

  private final SequenceReadsMapper bowtie;
  private final Reporter reporter;
  private GenomeDescStorage storage;
  private Pattern pattern = Pattern.compile("\t");

  private FastqScreenResult fastqScreenResult;
  private int readsprocessed = 0;
  private int readsMapped = 0;
  private String genomeReference;

  /**
   * @param fastqRead
   * @param listGenomes
   * @param properties
   * @throws AozanException
   * @throws BadBioEntryException
   */
  public void doMap(File fastqRead, List<String> listGenomes,
      Properties properties) throws AozanException, BadBioEntryException {

    this.doMap(fastqRead, null, listGenomes, properties);
  }

  // TODO : override method doMap() of PseudoMapReduce
  /**
   * @param readsFile fastq file
   * @param listGenomes
   * @throws IOException
   * @throws EoulsanException
   * @throws BadBioEntryException
   */
  public void doMap(File fastqRead1, File fastqRead2, List<String> listGenomes,
      Properties properties) throws AozanException, BadBioEntryException {

    final int mapperThreads =
        Integer.parseInt(properties.getProperty(KEY_NUMBER_THREAD));
    final String tmpDir = properties.getProperty(KEY_TMP_DIR);
    final boolean pairEnd = fastqRead2 == null ? false : true;

    // Mapper change arguments and ignore default arguments
    // Option -l 40 : seed use by bowtie already define to 40
    final String newArgumentsMapper =
        " -l 40 --chunkmbs 512" + (pairEnd ? " --maxins 1000" : "");

    for (String genome : listGenomes) {

      try {
        DataFile genomeFile = new DataFile("genome://" + genome);

        // test if index Genome reference exists
        File archiveIndexFile = createIndex(bowtie, genomeFile, tmpDir);

        final long startTime = System.currentTimeMillis();

        FastsqScreenSAMParser parser =
            new FastsqScreenSAMParser(this.getMapOutputTempFile(), genome,
                pairEnd);
        this.setGenomeReference(genome);

        final File indexDir =
            new File(StringUtils.filenameWithoutExtension(archiveIndexFile
                .getPath()));

        bowtie.setMapperArguments("");

        bowtie.init(pairEnd, FastqFormat.FASTQ_SANGER, archiveIndexFile,
            indexDir, reporter, COUNTER_GROUP);
        bowtie.setMapperArguments(newArgumentsMapper);
        bowtie.setThreadsNumber(mapperThreads);

        if (fastqRead2 == null) {
          // mode single-end
          bowtie.map(fastqRead1, parser);
        } else {
          // mode pair-end
          bowtie.map(fastqRead1, fastqRead2, parser);
        }

        this.readsprocessed = parser.getReadsprocessed();

        parser.closeMapOutputFile();

        final long endTime = System.currentTimeMillis();
        LOGGER.info("Mapping with genome "
            + genome + "  in " + toTimeHumanReadable(endTime - startTime));

        System.out.println("Mapping with genome "
            + genome + "  in " + toTimeHumanReadable(endTime - startTime)
            + "\n");

      } catch (IOException e) {
        e.printStackTrace();
        throw new AozanException(e.getMessage());
      }
    } // for
  } // doMap

  /**
   * @param bowtie
   * @param genomeDataFile
   * @param tmpDir
   * @return
   * @throws BadBioEntryException
   * @throws IOException
   */
  private File createIndex(final SequenceReadsMapper bowtie,
      final DataFile genomeDataFile, final String tmpDir)
      throws BadBioEntryException, IOException {

    final long startTime = System.currentTimeMillis();

    final DataFile result =
        new DataFile(tmpDir
            + "/aozan-bowtie-index-" + genomeDataFile.getName() + ".zip");

    // Create genome description
    GenomeDescription desc = createGenomeDescription(genomeDataFile);

    GenomeMapperIndexer indexer = new GenomeMapperIndexer(bowtie);
    indexer.createIndex(genomeDataFile, desc, result);

    final long endTime = System.currentTimeMillis();
    LOGGER.info("Get back index for "
        + genomeDataFile.getName() + " in "
        + toTimeHumanReadable(endTime - startTime));

    System.out.println("Get back index for "
        + genomeDataFile.getName() + " in "
        + toTimeHumanReadable(endTime - startTime));

    return result.toFile();
  }

  /**
   * @param genomeFile
   * @return
   * @throws BadBioEntryException
   * @throws IOException
   */
  private GenomeDescription createGenomeDescription(final DataFile genomeFile)
      throws BadBioEntryException, IOException {

    GenomeDescription desc = null;

    if (storage != null) {
      desc = storage.get(genomeFile);
    }

    if (desc == null) {
      // Compute the genome description
      desc =
          GenomeDescription.createGenomeDescFromFasta(genomeFile.open(),
              genomeFile.getName());

      if (storage != null)
        storage.put(genomeFile, desc);
    }

    return desc;
  }

  @Override
  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with genome reference used
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  public void map(final String value, final List<String> output,
      final Reporter reporter) throws IOException {

    if (value == null || value.length() == 0 || value.charAt(0) == '@')
      return;

    String[] tokens = pattern.split(value, 3);
    String nameRead = null;

    // flag of SAM format are in case 2 and flag = 4 for read unmapped
    if (Integer.parseInt(tokens[1]) != 4)
      nameRead = tokens[0];

    if (nameRead == null)
      return;
    output.add(nameRead + "\t" + genomeReference);

  }

  /**
   * Reducer Receive for each read list mapped genome Values first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome
   * @param key input key of the reducer
   * @param values values for the key
   * @param output list of output values of the reducer : here not use
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the reducer
   */
  public void reduce(final String key, final Iterator<String> values,
      final List<String> output, final Reporter reporter) throws IOException {

    boolean oneHit = true;
    boolean oneGenome = true;
    String currentGenome = null;
    String nextGenome = null;
    readsMapped++;

    // values format : a number 1 or 2 which represent the number of hits for
    // the read on one genome and after name of the genome
    while (values.hasNext()) {
      String s = values.next();
      oneHit = s.charAt(0) == '1' ? true : false;
      nextGenome = s.substring(1);

      fastqScreenResult.countHitPerGenome(nextGenome, oneHit, oneGenome);

      oneGenome = !nextGenome.equals(currentGenome);
      currentGenome = nextGenome;
    }

  }

  /**
   * Update list genomeReference : create a new entry for the new reference
   * genome
   */
  public void setGenomeReference(final String genome) {
    this.genomeReference = genome;
    fastqScreenResult.addGenome(genome);
  }

  /**
   * Compute percent for each count of hits per reference genome without
   * rounding
   * @return FastqScreenResult result of FastqScreen
   */
  public FastqScreenResult getFastqScreenResult() throws AozanException {

    System.out.println("nb read mapped "
        + readsMapped + " / nb read " + readsprocessed);

    if (readsMapped > readsprocessed)
      return null;

    fastqScreenResult.countPercentValue(readsMapped, readsprocessed);

    return fastqScreenResult;
  }

  //
  // Constructor
  //

  /**
   * Public construction which declare the bowtie mapper used
   */
  public FastqScreenPseudoMapReduce() {
    this.bowtie = new BowtieReadsMapper();
    this.reporter = new Reporter();

    Settings settings = EoulsanRuntime.getSettings();
    DataFile genomeDescStoragePath =
        new DataFile(settings.getGenomeDescStoragePath());

    if (genomeDescStoragePath != null)
      this.storage = SimpleGenomeDescStorage.getInstance(genomeDescStoragePath);

    this.fastqScreenResult = new FastqScreenResult();
  }

}
