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

package fr.ens.biologie.genomique.aozan.collectors.stats;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.StatisticsCollector;
import fr.ens.biologie.genomique.aozan.util.StatisticsUtils;

/**
 * The class define a entity statistics which compute data collected on project,
 * sample or others and update run data.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class EntityStat {

  // TODO Reorganise this code

  /** Default genome value. */
  private static final String DEFAULT_GENOME = "No genome.";

  /** Default read value, manage only read 1. */
  private static final int READ = 1;

  /** Run data. */
  private final RunData data;

  /** Project Id. */
  private final int projectId;

  /** The sample Id. */
  private final int sampleId;

  /** Genomes. */
  private final Set<String> genomes;

  /** Samples, with technical replicats. */
  private final List<Integer> samples;

  /** Lanes in run for project. */
  private final Set<Integer> lanes;

  private final StatisticsCollector statisticsCollector;

  // Compile demultiplexing data on all samples
  /** Raw cluster count samples. */
  private List<Integer> rawClusterSamples;

  /** PF cluster count samples. */
  private List<Integer> pfClusterSamples;

  /**
   * Percent mapped contamination samples, value related to mapped read on data
   * set genomes contaminant setting.
   */
  private List<Double> mappedContaminationPercentSamples;

  /** Project is indexed. */
  private boolean isIndexed;

  /** Raw cluster recovery sum. */
  private int rawClusterRecoverySum = 0;

  /** Cluster recovery sum. */
  private int pfClusterRecoverySum = 0;

  /** Data compile in run data. */
  private boolean compiledData = false;

  /**
   * Creates the run data project.
   * @return the run data.
   * @throws AozanException if run data object has already be create.
   */
  public RunData createRunDataProject(final String prefix)
      throws AozanException {

    if (compiledData) {
      throw new AozanException("Run data always updated for project "
          + this.data.getProjectName(this.projectId) + ".");
    }

    final RunData data = new RunData();

    StatisticsUtils stats = null;

    data.put(prefix + ".lanes", Joiner.on(",").join(this.lanes));

    data.put(prefix + ".genomes.ref",
        (this.genomes.isEmpty() ? "NA" : Joiner.on(",").join(getGenomes())));

    data.put(prefix + ".samples.count", samples.size());
    data.put(prefix + ".isindexed", isIndexed);

    // Compile data on raw cluster
    stats = new StatisticsUtils(this.rawClusterSamples);

    data.put(prefix + ".raw.cluster.sum", stats.getSumToInteger());
    data.put(prefix + ".raw.cluster.min", stats.getMin().intValue());
    data.put(prefix + ".raw.cluster.max", stats.getMax().intValue());

    // Compile data on raw cluster
    stats = new StatisticsUtils(this.pfClusterSamples);

    data.put(prefix + ".pf.cluster.sum", stats.getSumToInteger());
    data.put(prefix + ".pf.cluster.min", stats.getMin().intValue());
    data.put(prefix + ".pf.cluster.max", stats.getMax().intValue());

    addConditionalRundata(data, prefix);

    compiledData = true;

    return data;
  }

  /**
   * Adds the conditional rundata.
   * @param data the data
   * @param prefix the prefix
   */
  private void addConditionalRundata(final RunData data, final String prefix) {

    // Check collector is selected
    if (this.statisticsCollector.isUndeterminedIndexesCollectorSelected()) {
      // Compile data on recoverable cluster
      data.put(prefix + ".raw.cluster.recovery.sum", rawClusterRecoverySum);
      data.put(prefix + ".pf.cluster.recovery.sum", pfClusterRecoverySum);
    }

    // Check collector is selected
    if (this.statisticsCollector.isFastqScreenCollectorSelected()) {
      // Compile data on detection contamination
      data.put(prefix + ".samples.exceeded.contamination.threshold.count",
          getSamplesWithContaminationCount());
    }
  }

  /**
   * Adds the sample.
   * @param sampleId the sample id
   * @throws AozanException if run data object has already be create.
   */
  public void addEntity(final int sampleId) throws AozanException {

    if (compiledData) {
      throw new AozanException("Can not add new sample ("
          + this.data.getSampleDemuxName(sampleId) + ") for project "
          + this.data.getProjectName(this.projectId)
          + ". Data always compile to updata run data.");
    }

    final int lane = this.data.getSampleLane(sampleId);

    this.lanes.add(lane);

    this.isIndexed = this.data.isLaneIndexed(lane);

    // Extract raw cluster
    this.rawClusterSamples
        .add(this.data.getSampleRawClusterCount(sampleId, READ));

    // Extract pf cluster
    this.pfClusterSamples
        .add(this.data.getSamplePFClusterCount(sampleId, READ));

    computeConditionalRundata(sampleId);

    this.samples.add(sampleId);

    // Extract from samplesheet file
    if (data.getSampleGenome(sampleId) != null) {
      this.genomes.add(data.getSampleGenome(sampleId));
    }
  }

  /**
   * Compute conditional rundata according to collector selected.
   * UndeterminedIndexesCollector and FastqScreenCollector is optional for this
   * collector.
   * @param sampleId the sample Id
   */
  private void computeConditionalRundata(final int sampleId) {

    final int lane = this.data.getSampleLane(sampleId);

    // Check collector is selected
    if (this.statisticsCollector.isUndeterminedIndexesCollectorSelected()
        && this.data.isUndeterminedInLane(lane)) {

      // Check if lane is indexed
      if (this.data.isLaneIndexed(lane)) {

        if (!this.data.isUndeterminedSample(sampleId)) {
          this.rawClusterRecoverySum +=
              this.data.getSampleRawClusterRecoveryCount(sampleId);

          this.pfClusterRecoverySum +=
              this.data.getSamplePFClusterRecoveryCount(sampleId);

        } else {
          this.rawClusterRecoverySum +=
              this.data.getLaneRawClusterRecoveryCount(lane);

          this.pfClusterRecoverySum +=
              this.data.getLanePFClusterRecoveryCount(lane);
        }
      }
    }

    // Check collector is selected
    if (this.statisticsCollector.isFastqScreenCollectorSelected()) {

      this.mappedContaminationPercentSamples.add(
          this.data.getPercentMappedReadOnContaminationSample(sampleId, READ));
    }
  }

  //
  // Getter
  //

  /**
   * Gets the samples with contamination count.
   * @return the samples with contamination count
   */
  private String getSamplesWithContaminationCount() {
    int count = 0;

    if (this.data.isUndeterminedSample(this.sampleId)) {
      return "NA";
    }

    for (double percent : this.mappedContaminationPercentSamples) {

      if (percent >= this.statisticsCollector.getContaminationThreshold())
        count++;
    }

    return "" + count;
  }

  /**
   * Gets the genomes.
   * @return the genomes
   */
  private Set<String> getGenomes() {

    if (this.genomes.isEmpty())
      return Collections.singleton(DEFAULT_GENOME);

    return this.genomes;
  }

  @Override
  public String toString() {

    return com.google.common.base.Objects.toStringHelper(this).add("genomes", this.genomes)
        .add("samples", this.samples).add("lanes", this.lanes)
        .add("statisticsCollector", this.statisticsCollector)
        .add("rawClusterSamples", this.rawClusterSamples)
        .add("pfClusterSamples", this.pfClusterSamples)
        .add("mappedContaminationPercentSamples",
            this.mappedContaminationPercentSamples)
        .add("isIndexed", this.isIndexed)
        .add("rawClusterRecoverySum", this.rawClusterRecoverySum)
        .add("pfClusterRecoverySum", this.pfClusterRecoverySum)
        .add("compiledData", this.compiledData).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hash(genomes, isIndexed, lanes, projectId, sampleId,
        samples);
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof EntityStat)) {
      return false;
    }

    EntityStat that = (EntityStat) obj;

    return Objects.equals(this.genomes, that.genomes)
        && Objects.equals(this.isIndexed, that.isIndexed)
        && Objects.equals(this.lanes, that.lanes)
        && Objects.equals(this.projectId, that.projectId)
        && Objects.equals(this.sampleId, that.sampleId)
        && Objects.equals(this.samples, that.samples);
  }

  //
  // Constructor
  //
  /**
   * Instantiates a new project stat.
   * @param runData the run data
   * @param projectId the project id
   * @param statCollector the stat collector
   * @throws AozanException if an error occurs when listing source fastqscreen
   *           xml report file.
   */
  public EntityStat(final RunData runData, final int projectId,
      final StatisticsCollector statCollector) throws AozanException {

    this(runData, projectId, -1, statCollector);
  }

  /**
   * Instantiates a new entity stat.
   * @param runData the run data
   * @param projectId the project id
   * @param sampleId the sample id
   * @param statCollector the stat collector
   * @throws AozanException the aozan exception
   */
  public EntityStat(final RunData runData, final int projectId,
      final int sampleId, final StatisticsCollector statCollector)
      throws AozanException {

    checkNotNull(runData, "runData argument cannot be null");
    checkNotNull(statCollector, "statCollector argument cannot be null");

    this.data = runData;
    this.projectId = projectId;
    this.sampleId = sampleId;

    this.genomes = new LinkedHashSet<>();
    this.lanes = new LinkedHashSet<>();
    this.samples = new ArrayList<>();

    // Compile demultiplexing data
    this.rawClusterSamples = new ArrayList<>();
    this.pfClusterSamples = new ArrayList<>();
    this.mappedContaminationPercentSamples = new ArrayList<>();

    this.statisticsCollector = statCollector;
  }
}