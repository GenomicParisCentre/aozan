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

package fr.ens.transcriptome.aozan.fastqc;

import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToInt;
import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToString;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.FastqscreenDemo;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class launches blastn on one sequence query and parses xml result file
 * to identify the best hit.
 * @since 1.2
 * @author Sandrine Perrin
 */
public class OverrepresentedSequencesBlast {

  /** LOGGER */
  private static final Logger LOGGER = Common.getLogger();

  // Key for parameters in Aozan configuration
  public static final String KEY_STEP_BLAST_REQUIRED =
      "qc.conf.step.blast.enable";
  public static final String KEY_BLAST_PATH = "qc.conf.blast.path";
  public static final String KEY_BLAST_PATH_DB = "qc.conf.blast.path.db";
  public static final String KEY_BLAST_ARGUMENTS = "qc.conf.blast.arguments";
  public static final String KEY_BLAST_VERSION_EXPECTED =
      "qc.conf.blast.version.expected";
  public static final String KEY_STEP_BLAST_ENABLE =
      "qc.conf.step.blast.enable";

  public static String TMP_PATH;

  // Save sequence and result blast for the run
  final static ConcurrentMap<String, BlastResultHit> sequencesAlreadyAnalysis =
      Maps.newConcurrentMap();

  // Tag configuration general of blast
  final static String tag_queryLength = "Iteration_query-len";
  final static String tag_blastVersion = "BlastOutput_version";

  static AozanException aozanException;

  static boolean firstCall = true;
  static boolean stepEnable = false;

  static List<String> sequencesToIgnore = Lists.newArrayList();
  final static List<String> cmd = Lists.newLinkedList();
  static String blastVersionExpected;

  /**
   * Configure and check that blastall can be launched
   * @param properties object with the collector configuration
   */
  public static void configure(final Properties properties) {

    stepEnable =
        Boolean.parseBoolean(properties.getProperty(KEY_STEP_BLAST_ENABLE)
            .trim().toLowerCase());

    if (stepEnable) {

      // Check parameters
      blastVersionExpected = properties.getProperty(KEY_BLAST_VERSION_EXPECTED);

      TMP_PATH = properties.getProperty(QC.TMP_DIR);

      String blastPath = properties.getProperty(KEY_BLAST_PATH);
      String blastDBPath = properties.getProperty(KEY_BLAST_PATH_DB);

      // Check paths needed in configuration aozan
      if (blastPath == null
          || blastPath.trim().length() == 0 || blastDBPath == null
          || blastDBPath.trim().length() == 0)
        stepEnable = false;

      else {

        try {
          // Add arguments from configuration aozan
          String blastArguments = properties.getProperty(KEY_BLAST_ARGUMENTS);

          createCommonArgs(blastPath, blastDBPath,
              checkArgBlast(blastArguments));

        } catch (IOException e) {
          // TODO
          // e.printStackTrace();
          stepEnable = false;
        } catch (AozanException e) {
          // TODO
          // e.printStackTrace();
          stepEnable = false;
        }

        LOGGER.info("FastQC-step blast : step is enable, command line = "
            + Joiner.on(' ').join(cmd));
      }
    }
  }

  /**
   * @return true if blastall can be launched else false
   */
  public static boolean isStepBlastEnable() {
    return stepEnable;
  }

  /**
   * Build the line command for a specific result file.
   * @param outputFile xml file to write result
   * @return command line in a list
   * @throws IOException occurs if outputfile doesn't exist
   */
  private List<String> createCommandLine(final File outputFile,
      final String sequence) throws IOException {

    checkExistingFile(outputFile,
        "FastQC-step blast : path to output file doesn't exist for the sequence "
            + sequence);

    // Clone command line
    List<String> finalCommand = Lists.newLinkedList(cmd);

    // Add output file result
    finalCommand.add("-o");
    finalCommand.add(outputFile.getAbsolutePath());

    return finalCommand;
  }

  /**
   * Build the command line, part common to all sequences
   * @param blastPath path to blastall
   * @param blastDBPath path to blastn database
   * @param argBlast argument for blastn added in configuration aozan, optional
   * @throws IOException
   */
  private static void createCommonArgs(final String blastPath,
      final String blastDBPath, final List<String> argBlast) throws IOException {

    checkExistingFile(new File(blastPath),
        "FastQC-step blast : path to blast doesn't exist.");
    // Check nt.nal file exists
    checkExistingFile(new File(blastDBPath + ".nal"),
        " FastQC-step blast : path to database blast doesn't exist.");

    cmd.clear();

    cmd.add(blastPath);
    cmd.add("-p");
    cmd.add("blastn");
    cmd.add("-d");
    cmd.add(blastDBPath);
    cmd.add("-m");
    cmd.add("7");

    // Add arguments from configuration aozan
    if (argBlast != null && argBlast.size() > 0)
      cmd.addAll(argBlast);

  }

  /**
   * Launch blastn and parse result xml file for identify the best hit.
   * @param sequence query blastn
   * @return a ContaminantHit for the best hit return by blastn or null
   */
  public ContaminantHit searchSequenceInBlast(final String sequence) {

    if (checkSequencesNotToIgnore(sequence))
      return null;

    BlastResultHit blastResult = null;
    File resultXML = null;

    // Check if new sequence
    if (sequencesAlreadyAnalysis.containsKey(sequence))
      blastResult = sequencesAlreadyAnalysis.get(sequence);

    else {
      // Save sequence in multithreading context
      // sequencesAlreadyAnalysis.put(sequence, blastResult);

      try {

        resultXML = FileUtils.createTempFile("blast_", "_result.xml");

        launchBlastSearch(createCommandLine(resultXML, sequence), sequence);

        if (resultXML.length() > 0)
          blastResult = parseDocument(resultXML, sequence);

        // Save result for sequence
        sequencesAlreadyAnalysis.put(sequence, blastResult);

      } catch (IOException e) {
        // e.printStackTrace();
        aozanException = new AozanException(e);
      } catch (AozanException e) {
        // e.printStackTrace();
        aozanException = new AozanException(e);

      } finally {

        // Remove XML file
        if (resultXML.exists())
          if (!resultXML.delete())
            LOGGER
                .warning("FastQC-step blast : Can not delete xml file with result from blast : "
                    + resultXML.getAbsolutePath());
      }
    }

    if (blastResult == null)
      // No hit found
      return null;

    return blastResult.getContaminantHit();

  }

  /**
   * Process blastn, one instance at the same time
   * @param cmd command line
   * @param sequence query blastn
   * @throws AozanException occurs if the process fails
   */
  private static synchronized void launchBlastSearch(final List<String> cmd,
      final String sequence) throws AozanException {

    final ProcessBuilder builder = new ProcessBuilder(cmd);

    System.out.println(sequence
        + " -> " + Joiner.on(' ').join(builder.command()));
    Process process;
    try {

      process = builder.start();

      // Writing on standard input
      OutputStreamWriter os = new OutputStreamWriter(process.getOutputStream());
      os.write(">seq_automatic");
      os.write("\n");
      os.write(sequence);
      os.flush();
      os.close();

      final int exitValue = process.waitFor();
      if (exitValue > 0)
        LOGGER
            .warning("FastQC-step blast : fail of process to launch blastn with sequence "
                + sequence + ", exist value is : " + exitValue);

    } catch (IOException e) {
      // e.printStackTrace();
      throw new AozanException(e);

    } catch (InterruptedException e) {
      // e.printStackTrace();
      throw new AozanException(e);
    }

  }

  /**
   * Parse xml file result to identify the best hit.
   * @param resultXML result file from blastn
   * @param sequence query blastn
   * @return the best hit or null
   * @throws AozanException occurs if the parsing fails.
   */
  public BlastResultHit parseDocument(final File resultXML,
      final String sequence) throws AozanException {

    BlastResultHit blastResult = null;

    InputStream is = null;
    try {
      checkExistingFile(resultXML,
          "FastQC-step blast : query result xml doesn't exist");

      // Create the input stream
      is = new FileInputStream(resultXML);

      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Parse general information on blastn
      parseHeaderDocument(doc);

      // Search the best hit
      blastResult = parseHit(doc, sequence);

      is.close();
    } catch (IOException e) {
      throw new AozanException(e);
    } catch (SAXException e) {
      throw new AozanException(e);
    } catch (ParserConfigurationException e) {
      throw new AozanException(e);

    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // e.printStackTrace();
      }
    }
    return blastResult;
  }

  /**
   * Retrieve general informations of blastn, call once.
   * @param doc root of xml file result for blastn
   * @throws AozanException occurs if the parsing fails.
   */
  private void parseHeaderDocument(final Document doc) throws AozanException {

    if (firstCall) {

      final String version = extractFirstValueToString(doc, tag_blastVersion);

      // Check version xml file
      if (!blastVersionExpected.equals(version))
        LOGGER.warning("FastQC - step blast : version xml not expected "
            + version + " instead of " + blastVersionExpected);
      else
        LOGGER.info("FastQC-step blast : blastall version " + version);

      StringBuilder parameters = new StringBuilder();

      parameters.append("Parameters_expect=");
      parameters.append(extractFirstValueToString(doc, "Parameters_expect"));
      parameters.append(", Parameters_sc-match=");
      parameters.append(extractFirstValueToString(doc, "Parameters_sc-match"));
      parameters.append(", Parameters_sc-mismatch=");
      parameters
          .append(extractFirstValueToString(doc, "Parameters_sc-mismatch"));
      parameters.append(", Parameters_gap-open=");
      parameters.append(extractFirstValueToString(doc, "Parameters_gap-open"));
      parameters.append(", Parameters_gap-extend=");
      parameters
          .append(extractFirstValueToString(doc, "Parameters_gap-extend"));
      parameters.append(", Parameters_filter=");
      parameters.append(extractFirstValueToString(doc, "Parameters_filter"));

      LOGGER.info("FastQC-step blast : blastall parameters "
          + parameters.toString());

      firstCall = false;
    }
  }

  /**
   * Search hit in xml result file.
   * @param doc root of the xml file
   * @param sequence query blastn
   * @return best hit contained in xml file or null
   */
  private BlastResultHit parseHit(final Document doc, final String sequence) {

    List<Element> responses = XMLUtils.getElementsByTagName(doc, "Iteration");
    Element elemIteration = responses.get(0);

    final int queryLength =
        extractFirstValueToInt(elemIteration, tag_queryLength);

    BlastResultHit blastResult = null;

    List<Element> hits = XMLUtils.getElementsByTagName(elemIteration, "Hit");
    int countHits = hits.size();

    // No hit found
    if (countHits == 0)
      return null;

    Element firstHit = hits.get(0);
    if (extractFirstValueToInt(firstHit, "Hit_num") == 1)
      // Parse the document
      blastResult =
          new BlastResultHit(firstHit, countHits, queryLength, sequence);

    return blastResult;
  }

  /**
   * Get the exception generated by the call to searchSequenceInBlast() method.
   * @return a exception object or null if no Exception has been thrown
   */
  public static Exception throwException() {
    return aozanException;
  }

  /**
   * Check if the sequence is included in list of sequences which doesn't return
   * a result by blastn.
   * @param sequence query blastn
   * @return true if blastn doesn't be launched for this sequence else false
   */
  private static boolean checkSequencesNotToIgnore(final String sequence) {

    return sequencesToIgnore.contains(sequence);
  }

  /**
   * Check argument for blastn from configuration aozan. This parameters can't
   * been modified : -d, -m, -a. The parameters are returned in a list.
   * @param argBlast parameters for blastn
   * @return parameters for blastn in a list
   * @throws AozanException occurs if the parameters syntaxe is not conforme.
   */
  public static List<String> checkArgBlast(final String argBlast)
      throws AozanException {

    if (argBlast == null || argBlast.length() == 0)
      return null;

    // Check coherence with default parameter defined in Aozan
    List<String> paramEvalue = Lists.newLinkedList();
    paramEvalue.add("-e");
    paramEvalue.add("0.0001");

    if (argBlast == null || argBlast.length() == 0)
      return paramEvalue;

    StringTokenizer tokens = new StringTokenizer(argBlast.trim(), " .,");
    List<String> args = Lists.newLinkedList();

    // Set of pair tokens : first must begin with '-'
    while (tokens.hasMoreTokens()) {
      String param = tokens.nextToken();
      String val = tokens.nextToken();

      if (!param.startsWith("-"))
        throw new AozanException("FastQC-step blast : parameters not conforme "
            + argBlast);

      // Parameters can not redefine
      if (param.equals("-d")
          || param.equals("-p") || param.equals("-m") || param.equals("-a"))
        continue;

      if (param.equals("-e")) {
        // Replace initial value
        paramEvalue.clear();
        paramEvalue.add("-e");
        paramEvalue.add(val);
        continue;
      }

      args.add(param);
      args.add(val);
    }

    args.addAll(paramEvalue);

    return args;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   */
  public OverrepresentedSequencesBlast() {

    // TODO file not exists actually
    // loadSequencesToIgnoreFile();

  }

  /** Test method */
  public static void main(final String[] argv) {

    // TODO to remove after test
    // Properties props = FastqscreenDemo.getPropertiesAozanConf();
    // props.put(KEY_BLAST_ARGUMENTS, "-e 10");
    //
    // configure(props);
    // try {
    // RuntimePatchFastQC.runPatchFastQC(isStepBlastEnable());
    // } catch (AozanException e1) {
    // e1.printStackTrace();
    // }
    //
    // File[] filesXml =
    // (new File("/home/sperrin/Documents/FastqScreenTest/tmp_bis/tmp")
    // .listFiles(new FileFilter() {
    //
    // @Override
    // public boolean accept(final File pathname) {
    // return pathname.length() > 0
    // && pathname.getName().endsWith(".xml");
    // }
    // }));
    //
    // OverrepresentedSequenceBlast ors = new OverrepresentedSequenceBlast();
    // BlastResultHit hit = null;
    // for (File f : filesXml) {
    // System.out.println("file " + f.getName());
    // try {
    // hit = ors.parseDocument(f, "AAAACACTTGTGCATTCTTTG");
    // } catch (AozanException e) {
    // // TODO Auto-generated catch block
    // System.out.println("file " + f.getName());
    // e.printStackTrace();
    // }
    // }
    // }
    //
    // public static void attente() {
    String file =
        "tmp_bis/aozan_fastq_2013_0072_polyA_TTAGGC_L001_R1_001.fastq";
    // String file =
    // "runtest/qc_130904_SNL110_0082_AC2BR0ACXX/"
    // +
    // "130904_SNL110_0082_AC2BR0ACXX/Project_bulbe_D2013/Sample_2013_0209/2013_0209_GTGAAA_L008_R1_001.fastq.bz2";

    final File fastq =
        new File("/home/sperrin/Documents/FastqScreenTest/" + file);

    System.setProperty("fastqc.unzip", "true");

    final String DIR = "/home/sperrin/Documents/FastqScreenTest/blat_fastqc";
    try {

      Properties props = FastqscreenDemo.getPropertiesAozanConf();

      configure(props);

      RuntimePatchFastQC.runPatchFastQC(isStepBlastEnable());
      QCModule[] modules = new QCModule[] {new OverRepresentedSeqs()};

      SequenceFile seqFile =
          SequenceFactory.getSequenceFile(new File[] {fastq});

      while (seqFile.hasNext()) {
        modules[0].processSequence(seqFile.next());
      }

      new HTMLReportArchiveAozan(seqFile, modules, new File(DIR,
          "report-fastqc.zip"));

      // searchSequenceInBlast(seq, null);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
