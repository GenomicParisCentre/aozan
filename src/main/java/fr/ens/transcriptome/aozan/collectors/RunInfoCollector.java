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
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.eoulsan.illumina.RunInfo;

/**
 * This collector collect data from the RunInfo.xml file.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class RunInfoCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "runinfo";

  private File runInfoFile;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return null;
  }

  @Override
  public void configure(final Properties properties) {

    if (properties == null)
      return;

    this.runInfoFile =
        new File(properties.getProperty(QC.RTA_OUTPUT_DIR), "RunInfo.xml");
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null)
      return;

    try {
      // Parse run info file
      final RunInfo runInfo = new RunInfo();
      runInfo.parse(this.runInfoFile);

      final String prefix = "run.info";

      data.put(prefix + ".run.id", runInfo.getId());
      data.put(prefix + ".run.number", runInfo.getNumber());
      data.put(prefix + ".flow.cell.id", runInfo.getFlowCell());
      data.put(prefix + ".flow.cell.lane.count", runInfo.getFlowCellLaneCount());
      data.put(prefix + ".flow.cell.surface.count",
          runInfo.getFlowCellSurfaceCount());
      data.put(prefix + ".flow.cell.swath.count",
          runInfo.getFlowCellSwathCount());
      data.put(prefix + ".flow.cell.tile.count", runInfo.getFlowCellTileCount());
      data.put(prefix + ".instrument", runInfo.getInstrument());
      data.put(prefix + ".date", runInfo.getDate());

      data.put(prefix + ".read.count", runInfo.getReads().size());
      for (RunInfo.Read read : runInfo.getReads()) {
        data.put(prefix + ".read" + read.getNumber() + ".cycles",
            read.getNumberCycles());
        data.put(prefix + ".read" + read.getNumber() + ".indexed",
            read.isIndexedRead());
      }

      final Set<Integer> lanesToAlign =
          Sets.newHashSet(runInfo.getAlignToPhix());
      for (int i = 1; i <= runInfo.getFlowCellLaneCount(); i++)
        data.put(prefix + ".align.to.phix.lane" + i, lanesToAlign.contains(i));

      // Add new entry in data : run mode
      boolean runPE =
          runInfo.getReads().size() > 1
              && !data.isReadIndexed(runInfo.getReads().size());
      data.put(prefix + ".run.mode", runPE ? "PE" : "SR");

    } catch (IOException e) {
      throw new AozanException(e);
    } catch (ParserConfigurationException e) {
      throw new AozanException(e);
    } catch (SAXException e) {
      throw new AozanException(e);
    }
  }

  @Override
  public void clear() {
  }
}
