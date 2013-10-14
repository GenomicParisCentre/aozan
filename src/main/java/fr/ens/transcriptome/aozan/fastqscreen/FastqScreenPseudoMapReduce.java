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
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
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
 * This class account reads that map to each of the reference genome.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenPseudoMapReduce extends PseudoMapReduce {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  protected static final String COUNTER_GROUP = "reads_mapping";
  private final Reporter reporter;

  private int mapperThreads = Runtime.getRuntime().availableProcessors();
  private final SequenceReadsMapper bowtie;
  private GenomeDescStorage storage;
  private GenomeDescription desc = null;
  private FastqScreenResult fastqScreenResult;
  private String tmpDir;

  private Pattern pattern = Pattern.compile("\t");

  private int readsprocessed = 0;
  private int readsmapped = 0;
  private String genomeReference;

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome.
   * @param fastqRead fastq file
   * @param listGenomes list of reference genome
   * @param genomeSample genome reference corresponding to sample
   * @param properties properties for mapping
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @throws AozanException if an error occurs while mapping
   * @throws BadBioEntryException if an error occurs while creating index genome
   */
  public void doMap(final File fastqRead, final List<String> listGenomes,
      final String genomeSample, final int numberThreads, final boolean paired)
      throws AozanException, BadBioEntryException {

    this.doMap(fastqRead, null, listGenomes, genomeSample, numberThreads,
        paired);
  }

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome
   * @param fastqRead1 fastq file
   * @param fastqRead1 fastq file in mode paired
   * @param listGenomes list of genome reference
   * @param genomeSample genome reference corresponding to sample
   * @param properties properties for mapping
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @throws AozanException if an error occurs while mapping
   * @throws BadBioEntryException if an error occurs while creating index genome
   * @throws InterruptedException if an error occurs during mapper process
   */
  public void doMap(final File fastqRead1, final File fastqRead2,
      final List<String> listGenomes, final String genomeSample,
      final int numberThreads, final boolean pairedMode) throws AozanException {

    // change mapper arguments
    final String newArgumentsMapper =
        " -l 20 -k 2 --chunkmbs 512" + (pairedMode ? " --maxins 1000" : "");

    if (numberThreads > 0)
      mapperThreads = numberThreads;

    for (String genome : listGenomes) {
      // Timer : for step mapping on genome
      final Stopwatch timer = new Stopwatch().start();

      try {
        DataFile genomeFile = new DataFile("genome://" + genome);

        // get index Genome reference exists
        File archiveIndexFile = createIndex(bowtie, genomeFile);

        if (archiveIndexFile == null) {
          LOGGER.warning("FASTQSCREEN : archive index file not found for "
              + genome);
          continue;
        }

        FastqScreenSAMParser parser =
            new FastqScreenSAMParser(this.getMapOutputTempFile(), genome,
                pairedMode);

        this.setGenomeReference(genome, genomeSample);

        final File indexDir =
            new File(StringUtils.filenameWithoutExtension(archiveIndexFile
                .getPath()));

        // remove default argument
        bowtie.setMapperArguments("");

        bowtie.init(archiveIndexFile, indexDir, reporter, COUNTER_GROUP);

        // define new argument
        bowtie.setMapperArguments(newArgumentsMapper);
        bowtie.setThreadsNumber(mapperThreads);

        if (pairedMode) {
          // mode pair-end
          InputStream outputSAM = bowtie.mapPE(fastqRead1, fastqRead2, desc);
          parser.parseLine(outputSAM);

          parser.closeMapOutputFile();
          this.readsprocessed = parser.getReadsprocessed();

        } else {

          if (desc == null)
            throw new AozanException(
                "Fastqscreen : genome description is null for bowtie");

          InputStream outputSAM = bowtie.mapSE(fastqRead1, desc);
          parser.parseLine(outputSAM);

          parser.closeMapOutputFile();
          this.readsprocessed = parser.getReadsprocessed();
        }

        LOGGER.fine("FASTQSCREEN : mapping on genome "
            + genome + " in mode " + (pairedMode ? "paired" : "single")
            + ", in "
            + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

        timer.stop();

      } catch (IOException e) {
        throw new AozanException(e);

      } catch (BadBioEntryException bee) {
        throw new AozanException(bee);
      }
    }
  }

  /**
   * Create a index with bowtie from the fasta file genome
   * @param bowtie mapper
   * @param genomeDataFile fasta file of genome
   * @param tmpDir temporary directory
   * @return File file of genome index
   * @throws BadBioEntryException if an error occurs while creating index genome
   * @throws IOException if an error occurs while using file index genome
   */
  private File createIndex(final SequenceReadsMapper bowtie,
      final DataFile genomeDataFile) throws BadBioEntryException, IOException {

    // Timer :
    final Stopwatch timer = new Stopwatch().start();

    final DataFile result =
        new DataFile(this.tmpDir
            + "/aozan-" + bowtie.getMapperName().toLowerCase() + "-index-"
            + genomeDataFile.getName() + ".zip");

    // Create genome description
    this.desc = createGenomeDescription(genomeDataFile);

    if (desc == null)
      return null;

    GenomeMapperIndexer indexer = new GenomeMapperIndexer(bowtie);
    indexer.createIndex(genomeDataFile, desc, result);

    LOGGER.fine("FASTQSCREEN : create/retrieve index for "
        + genomeDataFile.getName() + " in "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    timer.stop();

    return result.toFile();
  }

  /**
   * Create a GenomeDescription object from a Fasta file
   * @param genomeFile file used for create index
   * @return genomeDescription description of the genome
   * @throws BadBioEntryException if an error occurs while creating index genome
   * @throws IOException if an error occurs while using file index genome
   */
  private GenomeDescription createGenomeDescription(final DataFile genomeFile)
      throws BadBioEntryException, IOException {

    if (!genomeFile.exists())
      LOGGER.warning("Fastqscreen "
          + genomeFile.getBasename()
          + " not exists, Index mapper can't be created.");
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

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  @Override
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

    this.readsmapped++;

    // values format : a number 1 or 2 which represent the number of hits for
    // the read on one genome and after name of the genome
    while (values.hasNext()) {
      String s = values.next();
      oneHit = s.charAt(0) == '1' ? true : false;

      currentGenome = s.substring(1);

      // set false if more on genome are done
      if (oneGenome && values.hasNext())
        oneGenome = false;

      fastqScreenResult.countHitPerGenome(currentGenome, oneHit, oneGenome);
    }
  }

  /**
   * Define the genome reference for mapping
   * @param genome name of the new genome
   * @param genomeSample genome reference corresponding to sample
   */
  public void setGenomeReference(final String genome, final String genomeSample) {
    this.genomeReference = genome;

    fastqScreenResult.addGenome(genome, genomeSample);
  }

  /**
   * Compile data of fastqscreen in percentage
   * @return FastqScreenResult result of FastqScreen
   */
  public FastqScreenResult getFastqScreenResult() throws AozanException {

    if (readsmapped > readsprocessed) {
      LOGGER.warning("FASTQSCREEN : nb read mapped "
          + readsmapped + " must been superior to nb read processed "
          + readsprocessed);
      return null;
    }
    LOGGER.fine("FASTQSCREEN : result of mappings : nb read mapped "
        + readsmapped + " / nb read " + readsprocessed);

    fastqScreenResult.countPercentValue(readsmapped, readsprocessed);

    return fastqScreenResult;
  }

  //
  // Constructor
  //

  /**
   * Public construction which declare the bowtie mapper
   */
  public FastqScreenPseudoMapReduce(final String tmpDir) {

    this.bowtie = new BowtieReadsMapper();
    this.tmpDir = tmpDir;

    // Define temporary directory
    this.setMapReduceTemporaryDirectory(new File(this.tmpDir));
    bowtie.setTempDirectory(new File(this.tmpDir));

    this.reporter = new Reporter();

    Settings settings = EoulsanRuntime.getSettings();
    DataFile genomeDescStoragePath =
        new DataFile(settings.getGenomeDescStoragePath());

    if (genomeDescStoragePath != null)
      this.storage = SimpleGenomeDescStorage.getInstance(genomeDescStoragePath);

    this.fastqScreenResult = new FastqScreenResult();
  }

}
