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

package fr.ens.biologie.genomique.aozan.tests.project;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.FastqScreenCollector;
import fr.ens.biologie.genomique.aozan.collectors.stats.ProjectStatisticsCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;

public class FastQScreenReportProjectTest extends AbstractProjectTest {

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {
    return Collections.singletonList((AozanTest) this);
  }

  @Override
  public TestResult test(final RunData data, final int projectId) {

    final String projectName = data.getProjectName(projectId);

    // Get HTML report URL
    final String filename = String.format("%s-fastqscreen.html", projectName);

    final String url = "Project_" + projectName + "/" + filename;

    return new TestResult(9, url, "url");
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME,
        ProjectStatisticsCollector.COLLECTOR_NAME);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public FastQScreenReportProjectTest() {

    super("project.fastqscreen.report", "FastQ Screen Report",
        "FastQ Screen Report");
  }

}
