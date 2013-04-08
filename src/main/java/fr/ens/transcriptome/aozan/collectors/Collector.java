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

package fr.ens.transcriptome.aozan.collectors;

import java.util.List;
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;

/**
 * This interface define a Collector.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface Collector {

  /**
   * Get the name of the collector
   * @return the name of the collector
   */
  public String getName();

  /**
   * Get the name of the collectors required to run this collector.
   * @return a list of String with the name of the required collectors
   */
  public List<String> getCollectorsNamesRequiered();

  /**
   * Configure the collector with the path of the run data
   * @param properties object with the collector configuration
   */
  public void configure(Properties properties);

  /**
   * Collect data.
   * @param data result data object
   * @throws AozanException if an error occurs while collecting data
   */
  public void collect(RunData data) throws AozanException;

  /**
   * Remove temporary files
   */
  public void clear();

}
