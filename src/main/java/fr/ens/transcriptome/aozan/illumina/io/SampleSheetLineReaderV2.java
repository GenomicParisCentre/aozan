/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */
package fr.ens.transcriptome.aozan.illumina.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntryVersion2;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetVersion2;

class SampleSheetLineReaderV2 extends SampleSheetLineReader {

  // Required in this order columns header for version2
  private static final List<String> FIELDNAMES_VERSION2_REQUIERED = Arrays
      .asList("sampleid");

  private static final List<String> FIELDNAMES_VERSION2_REQUIERED_FOR_QC =
      Arrays.asList("sampleid", "sampleref", "index", "index2" ,"description",
          "sampleproject");

  private static final List<String> FIELDNAMES_VERSION2_FORBIDDEN = Arrays
      .asList("samplename");

  private static final List<String> FIELDNAMES_VERSION2 = buildList();

  private static final List<String> SESSIONS_HEADER = Arrays.asList("[Header]",
      "[Reads]", "[Settings]", "[Data]");

  private final boolean isCompatibleForQCReport;

  private final int laneCount;

  private int fieldsCountExpected;

  private String currentSessionName;

  private boolean firstLineData = true;

  private Map<String, Integer> positionFields;

  private boolean columnLaneFound = false;

  @Override
  public void parseLine(final SampleSheet design, final List<String> fields)
      throws AozanException {

    final String firstField = fields.get(0).trim();
    final SampleSheetVersion2 design2 = (SampleSheetVersion2) design;

    // First field empty
    if (firstField.isEmpty())
      return;

    // If first field start with '[' is an new session
    if (firstField.startsWith("[") && firstField.contains("]")) {

      // Check exist
      if (!SESSIONS_HEADER.contains(firstField))
        throw new AozanException(
            "Parsing sample sheet file, invalid session name " + firstField);

      // Set currentSessionName
      this.currentSessionName = firstField;

    } else {

      if (this.currentSessionName.contains("[Data]")) {

        addSessionDataEntry(fields, design2);

      } else {
        design2.addSessionEntry(fields, this.currentSessionName);
      }
    }

  }

  private void addSessionDataEntry(List<String> fields,
      SampleSheetVersion2 design2) throws AozanException {

    if (firstLineData) {

      this.firstLineData = false;
      this.positionFields = checkHeaderColumnSessionData(fields);
      this.fieldsCountExpected = positionFields.size();

      Common.getLogger().warning(
          "DEBUG: to parse line from samplesheet V2 option is Column lane exist "
              + isColumnLaneExist()
              + " compatibility with QCReport is requiered "
              + isCompatibleForQCReport);

    } else {
      // assert (fields.size() == this.fieldsCountExpected);

      // Check field on sample description in run
      // trimAndCheckFields(fields, fieldsCountExpected);

      if (isColumnLaneExist()) {
        // Set lane value on sample
        int laneNumber = parseLane(fields.get(this.positionFields.get("lane")));

        // Set sample as the same line contains in sample sheet file
        createSample(design2, fields, laneNumber);

      } else if (isCompatibleForQCReport) {

        // Set same sample for each lane and add field lane
        createAndDuplicateSample(design2, fields);

      } else {

        // Set sample as the same line contains in sample sheet file
        createSample(design2, fields, 0);
      }
    }
  }

  private void createAndDuplicateSample(final SampleSheetVersion2 design2,
      final List<String> fields) throws AozanException {

    for (int lane = 1; lane <= this.laneCount; lane++) {

      // Create same sample for each lane number
      createSample(design2, fields, lane);
    }
  }

  private void createSample(final SampleSheetVersion2 design2,
      final List<String> fields, int laneNumber) throws AozanException {

    Preconditions.checkNotNull(design2, "sample sheet instance");
    Preconditions.checkNotNull(fields,
        "fields for one line on sample sheet file");

    final SampleEntry sample = new SampleEntryVersion2();
    sample.setLane(laneNumber);

    for (int indice = 0; indice <= this.fieldsCountExpected; indice++) {

      final String key = FIELDNAMES_VERSION2.get(indice);

      if (key == null) {
        throw new AozanException("Parsing design file, key is invalid: "
            + key + " no position found in list "
            + Joiner.on("\t").withKeyValueSeparator("=").join(positionFields));
      }

      if (this.positionFields.containsKey(key)) {
        final String value = fields.get(this.positionFields.get(key));

        switch (key) {

        case "sampleid":
          sample.setSampleId(value);
          break;

        case "sampleref":
          sample.setSampleRef(value);
          break;

        case "index":
          sample.setIndex(value);
          break;

        case "index2":
          sample.setIndex2(value);
          break;

        case "description":
          sample.setDescription(value);
          break;

        case "sampleproject":
          sample.setSampleProject(value);
          break;

        default:
        }
      }
    }

    // Add sample in design instance
    design2.addSample(sample, isColumnLaneExist());
  }

  private static List<String> buildList() {
    final List<String> l = new ArrayList<>();

    l.addAll(FIELDNAMES_VERSION2_REQUIERED);
    l.addAll(FIELDNAMES_VERSION2_REQUIERED_FOR_QC);

    // Optional
    l.add("Lane");

    return Collections.unmodifiableList(l);
  }

  private Map<String, Integer> checkHeaderColumnSessionData(
      final List<String> rawFields) throws AozanException {

    final List<String> fields = convertAndLowerCase(rawFields);
    checkFieds(rawFields, fields);

    final Map<String, Integer> pos = new HashMap<>(fields.size());

    // Check all required field exists
    for (String name : FIELDNAMES_VERSION2_REQUIERED) {
      if (!fields.contains(name)) {
        throw new AozanException(
            "Parsing Sample sheet file: missing required field for version2 "
                + name + " in header columns " + Joiner.on(",").join(fields));
      }
    }

    if (isCompatibleForQCReport) {
      // Check exist usefull columns in sample sheet
      for (String name : FIELDNAMES_VERSION2_REQUIERED_FOR_QC) {
        if (!fields.contains(name)) {
          throw new AozanException(
              "Parsing Sample sheet file: missing required field to create quality control report "
                  + name + " in header " + Joiner.on(", ").join(fields));
        }
      }
    }

    // Check no field which change tree directory is included in samplesheet
    for (String name : FIELDNAMES_VERSION2_FORBIDDEN) {
      if (fields.contains(name)) {

        throw new AozanException(
            "Parsing Sample sheet file: the column name can not use " + name);
      }
    }

    // Locate fields
    for (int i = 0; i < fields.size(); i++) {
      if (FIELDNAMES_VERSION2.contains(fields.get(i))) {
        // Save position only for field useful for sample entry instance
        pos.put(fields.get(i), i);

      } else if (fields.get(i).equals("lane")) {

        pos.put(fields.get(i), i);

        // } else if (fields.get(i).equals("index2")) {
        //
        // pos.put(fields.get(i), i);

      }

    }
    System.out.println("position header fields "
        + Joiner.on("\n").withKeyValueSeparator("=").join(pos));

    // Check exist lane columns in sample sheet
    this.columnLaneFound = pos.containsKey("lane");

    return pos;
  }

  /**
   * Convert remove characters '-_ ' and lower case.
   * @param rawFields the raw fields
   * @return the list
   */
  private List<String> convertAndLowerCase(final List<String> rawFields) {

    final List<String> l = new LinkedList<>();

    for (String field : rawFields) {
      l.add(field.replaceAll("[_ -]", "").toLowerCase(Globals.DEFAULT_LOCALE));
    }

    return Collections.unmodifiableList(l);
  }

  /**
   * Check fields with right syntax for bcl2fastq 2.
   * @param fields the fields
   * @throws AozanException the Aozan Exception
   */
  private void checkFieds(final List<String> fields,
      final List<String> fieldsConverted) throws AozanException {

    final String s = Joiner.on(",").join(fields);

    // Check SampleID or Sample_ID exist
    if (!(fields.contains("SampleID") || fields.contains("Sample_ID"))) {
      throw new AozanException(
          "in sample sheet missing requiered field SampleID or Sample_ID ("
              + s + ").");
    }

    if (fieldsConverted.contains("sampleproject")) {
      // Check SampleProject or Sample_Project exist in raw fields
      if (!(fields.contains("SampleProject") || fields
          .contains("Sample_Project"))) {
        throw new AozanException(
            "in sample sheet missing requiered field SampleProject or Sample_Project ("
                + s + ").");
      }
    }

    if (fieldsConverted.contains("index")) {
      // Check index exist in raw fields
      if (!fields.contains("index")) {
        throw new AozanException(
            "in sample sheet missing requiered field index (" + s + ").");
      }
    }

    if (fieldsConverted.contains("index2")) {
      // Check index exist in raw fields
      if (!fields.contains("index2")) {
        throw new AozanException(
            "in sample sheet missing requiered field index2 (" + s + ").");
      }
    }

  }

  private boolean isColumnLaneExist() {

    return this.columnLaneFound;
  }

  //
  // Constructor
  //
  public SampleSheetLineReaderV2(final int laneCount,
      final boolean compatibleForQCReport) {

    this.laneCount = (laneCount == -1 ? 2 : laneCount);

    // TODO throw exception if incompatible data
    this.isCompatibleForQCReport = compatibleForQCReport && this.laneCount > 0;

  }
}