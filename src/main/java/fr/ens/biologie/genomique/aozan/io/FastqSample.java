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

package fr.ens.biologie.genomique.aozan.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;

/**
 * The class correspond of one entity to treat by AbstractFastqCollector, so a
 * sample per lane and in mode-paired one FastSample for each read (R1 and R2).
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqSample {

  public static final String FASTQ_EXTENSION = ".fastq";

  public static final String SUBSET_FASTQ_FILENAME_PREFIX =
      Globals.APP_NAME_LOWER_CASE + "_subset_fastq_";
  private static final String NO_INDEX = "NoIndex";

  private final int sampleId;
  private final int read;
  private final int lane;
  private final String sampleDirname;
  private final String sampleName;
  private final String projectName;
  private final String description;
  private final String index;

  private final boolean undeterminedIndex;

  private final String keyFastqSample;
  private final String subsetFastqFilename;

  private final List<File> fastqFiles;
  private final CompressionType compressionType;

  private final Bcl2FastqOutput bcl2fastqOutput;

  private final File tmpDir;

  /**
   * Create a key unique for each fastq sample.
   * @return key
   */
  private String createKeyFastqSample() {

    // Case exists only during step test
    if (this.fastqFiles.isEmpty()) {
      return this.lane + " " + this.sampleName;
    }

    final String firstFastqFileName = this.fastqFiles.get(0).getName();

    return firstFastqFileName.substring(0,
        firstFastqFileName.length()
            - FASTQ_EXTENSION.length()
            - this.compressionType.getExtension().length());
  }

  /**
   * Create name for temporary fastq file uncompressed.
   * @param runId run id
   * @param key sample key
   * @return name fastq file
   */
  private static String createSubsetFastqFilename(final String runId,
      final String key) {
    return SUBSET_FASTQ_FILENAME_PREFIX + runId + '_' + key + FASTQ_EXTENSION;
  }

  /**
   * Get ratio compression for fastq files according to type compression.
   * @return ratio compression or 1 if not compress
   */
  private double ratioCommpression() {

    switch (this.compressionType) {

    case GZIP:
      return 7.0;

    case BZIP2:
      return 5.0;

    case NONE:
      return 1.0;

    default:
      return 1.0;

    }

  }

  /**
   * Receive the type of compression use for fastq files, only one possible per
   * sample.
   */
  private static CompressionType getCompressionExtension(
      final List<File> fastqFiles) {

    if (fastqFiles.isEmpty()) {
      throw new IllegalArgumentException("fastqFiles argument cannot be empty");
    }

    if (fastqFiles.get(0).getName().endsWith(FASTQ_EXTENSION)) {
      return CompressionType.NONE;
    }

    return CompressionType
        .getCompressionTypeByFilename(fastqFiles.get(0).getName());

  }

  /**
   * Create the prefix used for add data in a RunData for each FastqSample.
   * @return prefix
   */
  public String getRundataPrefix() {

    return ".sample" + this.sampleId + ".read" + this.read;
  }

  /**
   * Return if it must uncompress fastq files else false.
   * @return true if it must uncompress fastq files else false.
   */
  public boolean isUncompressedNeeded() {

    return !this.compressionType.equals(CompressionType.NONE);
  }

  /**
   * Returns a estimation of the size of uncompressed fastq files according to
   * the type extension of files and the coefficient of uncompression
   * corresponding.
   * @return size if uncompressed fastq files
   */
  public long getUncompressedSize() {
    // according to type of compressionExtension
    long sizeFastqFiles = 0;

    for (final File f : this.fastqFiles) {
      sizeFastqFiles += f.length();
    }

    return (long) (sizeFastqFiles * ratioCommpression());
  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix.
   * @return an array of abstract pathnames
   */
  private List<File> createListFastqFiles(final int read,
      final boolean createEmptyFastq) {

    final List<File> result =
        this.bcl2fastqOutput.createListFastqFiles(this, read);

    // Empty FASTQ files are not created by bcl2fastq 2
    if (result.isEmpty()) {

      final String filenamePrefix =
          this.bcl2fastqOutput.getFilenamePrefix(this, read);

      final File emptyFile =
          new File(this.tmpDir, filenamePrefix + "_empty.fastq");

      // Create empty file
      if (!emptyFile.exists() && createEmptyFastq) {
        try {
          if (!emptyFile.createNewFile()) {
            throw new IOException(
                "Unable to create empty FASTQ file: " + emptyFile);
          }
        } catch (IOException e) {
          throw new AozanRuntimeException(e);
        }
      }

      return Collections.singletonList(emptyFile);
    }

    return result;
  }

  /**
   * Gets the prefix report filename.
   * @return the prefix report
   */
  public String getPrefixReport(final int read) {

    return this.bcl2fastqOutput.buildPrefixReport(this, read);

  }

  public String getPrefixReport() {

    return this.bcl2fastqOutput.buildPrefixReport(this);

  }

  //
  // Getters
  //

  /**
   * Get the sample Id.
   * @return the sample Id
   */
  public int getSampleId() {
    return this.sampleId;
  }

  /**
   * Get the number of read from the sample in run.
   * @return number read
   */
  public int getRead() {
    return this.read;
  }

  /**
   * Get the number of lane from the sample in run.
   * @return number lane
   */
  public int getLane() {
    return this.lane;
  }

  /**
   * Get the directory name of the sample. The value can be empty if there is
   * not dedicated directory for the sample.
   * @return the directory name of the sample
   */
  public String getSampleDirectoryName() {
    return this.sampleDirname;
  }

  /**
   * Get the project name from the sample in run.
   * @return project name
   */
  public String getProjectName() {
    return this.projectName;
  }

  /**
   * Get the sample name in run.
   * @return sample name
   */
  public String getSampleName() {
    return this.sampleName;
  }

  /**
   * Get the description of the sample in run.
   * @return description of the sample
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Test if the sample is an undetermined index sample.
   * @return true if the sample has an undetermined index
   */
  public boolean isUndeterminedIndex() {
    return this.undeterminedIndex;
  }

  /**
   * Gets the index.
   * @return the index
   */
  public String getIndex() {
    return this.index;
  }

  /**
   * Get list of fastq files for this sample.
   * @return list fastq files
   */
  public List<File> getFastqFiles() {
    return this.fastqFiles;
  }

  /**
   * Get the prefix corresponding on read 2 for this sample, this value exists
   * only in mode paired.
   * @return prefix for read 2
   */
  public String getPrefixRead2() {
    return this.keyFastqSample.replaceFirst("R1", "R2");
  }

  /**
   * Get the name for temporary fastq files uncompressed.
   * @return temporary fastq file name
   */
  public String getSubsetFastqFilename() {
    return this.subsetFastqFilename;
  }

  /**
   * Get the unique key for sample.
   * @return unique key for sample
   */
  public String getKeyFastqSample() {
    return this.keyFastqSample;
  }

  /**
   * Get the compression type for fastq files.
   * @return compression type for fastq files
   */
  public CompressionType getCompressionType() {
    return this.compressionType;
  }

  //
  // Partial FASTQ file methods
  //

  /**
   * Get a SampleSheetFile from a QC
   * @return sampleSheetFile
   */
  private static File getSampleSheetFileFromQC(QC qc) {
    checkNotNull(qc, "qc argument cannot be null");
    return qc.getSampleSheetFile();
  }

  /**
   * Return the temporary if exists which correspond to the key.
   * @return File temporary file or null if it not exists
   */
  public File getSubsetFastqFile() {

    return new File(this.tmpDir, getSubsetFastqFilename());
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("read", read).add("lane", lane)
        .add("sampleName", sampleName).add("projectName", projectName)
        .add("descriptionSample", description).add("index", index)
        .add("undeterminedIndex", undeterminedIndex)
        .add("keyFastqSample", keyFastqSample)
        .add("subsetFastqFilename", subsetFastqFilename)
        .add("fastqFiles", fastqFiles).add("compressionType", compressionType)
        .toString();
  }

  //
  // Constructors
  //

  /**
   * Public constructor corresponding of a technical replica sample.
   * @param qc QC object
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @param sampleDirname sample directory name
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final QC qc, final int sampleId, final int read,
      final int lane, final String sampleDirname, final String sampleName,
      final String projectName, final String descriptionSample,
      final String index) throws IOException {

    this(getSampleSheetFileFromQC(qc), qc.getFastqDir(), qc.getTmpDir(),
        qc.getRunId(), sampleId, read, lane, sampleDirname, sampleName,
        projectName, descriptionSample, index);
  }

  /**
   * Public constructor corresponding of a technical replica sample.
   * @param sampleSheetFile samplesheet file
   * @param fastqDir FASTQ directory
   * @param tmpDir temporary directory
   * @param runId the run id
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @param sampleDirname sample directory name
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final File sampleSheetFile, final File fastqDir,
      final File tmpDir, final String runId, final int sampleId, final int read,
      final int lane, final String sampleDirname, final String sampleName,
      final String projectName, final String descriptionSample,
      final String index) throws IOException {

    this(new Bcl2FastqOutput(sampleSheetFile, fastqDir), fastqDir, tmpDir,
        runId, sampleId, read, lane, sampleDirname, sampleName, projectName,
        descriptionSample, index, true);

  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param qc QC object
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final QC qc, final int sampleId, final int read,
      final int lane) throws IOException {

    this(getSampleSheetFileFromQC(qc), qc.getFastqDir(), qc.getTmpDir(),
        qc.getRunId(), sampleId, read, lane);
  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final File sampleSheetFile, final File fastqDir,
      final File tmpDir, final String runId, final int sampleId, final int read,
      final int lane) throws IOException {

    this(new Bcl2FastqOutput(sampleSheetFile, fastqDir), fastqDir, tmpDir,
        runId, sampleId, read, lane, null, "lane" + lane, "", "", NO_INDEX,
        true);
  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param bcl2FastqOutput Bcl2FastqOutput object
   * @param fastqDir FASTQ directory
   * @param tmpDir temporary directory
   * @param runId the run id
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @param sampleDirname sample dir name
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   * @param createEmptyFastq enable the creation of empty FASTQ files
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final Bcl2FastqOutput bcl2FastqOutput, final File fastqDir,
      final File tmpDir, final String runId, final int sampleId, final int read,
      final int lane, final String sampleDirname, final String sampleName,
      final String projectName, final String descriptionSample,
      final String index, final boolean createEmptyFastq) throws IOException {

    checkArgument(read > 0, "read value cannot be lower than 1");
    checkArgument(lane > 0, "read value cannot be lower than 1");

    this.sampleId = sampleId;
    this.read = read;
    this.lane = lane;
    this.sampleDirname = sampleDirname == null ? "" : sampleDirname.trim();
    this.sampleName = sampleName;
    this.projectName = projectName;
    this.description = descriptionSample;
    this.index = (index == null || index.isEmpty()) ? NO_INDEX : index;
    this.undeterminedIndex = this.index.equals(NO_INDEX);
    this.bcl2fastqOutput = bcl2FastqOutput;
    this.tmpDir = tmpDir;

    this.fastqFiles = createListFastqFiles(this.read, createEmptyFastq);

    this.compressionType = getCompressionExtension(this.fastqFiles);
    this.keyFastqSample = createKeyFastqSample();

    this.subsetFastqFilename =
        createSubsetFastqFilename(runId, this.keyFastqSample);

  }

}
