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

import java.util.List;
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.eoulsan.util.SystemUtils;

public class AozanCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "aozaninfo";

  /** Prefix for run data */
  public static final String PREFIX = "aozan.info";

  private Properties properties;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return null;
  }

  @Override
  public boolean isStatisticCollector() {
    return false;
  }

  @Override
  public void configure(final Properties properties) {
    // Nothing to do

    this.properties = properties;
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Add main data on sequencing
    data.put(PREFIX + ".version", Globals.APP_VERSION_STRING);
    data.put(PREFIX + ".build.commit", Globals.APP_BUILD_COMMIT);
    data.put(PREFIX + ".build.date", Globals.APP_BUILD_DATE);
    data.put(PREFIX + ".build.host", Globals.APP_BUILD_HOST);
    data.put(PREFIX + ".build.number", Globals.APP_BUILD_NUMBER);
    data.put(PREFIX + ".build.year", Globals.APP_BUILD_YEAR);

    for (Object o : properties.keySet()) {
      final String key = (String) o;
      final String value =
          properties.getProperty(key).toLowerCase(Globals.DEFAULT_LOCALE);

      data.put(PREFIX + ".conf." + key, value);
    }

    data.put(PREFIX + ".host.name", SystemUtils.getHostName());

    data.put(PREFIX + ".operating.system.version",
        System.getProperty("os.version"));

    data.put(PREFIX + ".operating.system.arch", System.getProperty("os.arch"));

    // User information
    data.put(PREFIX + ".user.name", System.getProperty("user.name"));
    data.put(PREFIX + ".user.home", System.getProperty("user.home"));
    data.put(PREFIX + ".user.current.directory", System.getProperty("user.dir"));

    // Java version
    data.put(PREFIX + ".java.vendor", System.getProperty("java.vendor"));
    data.put(PREFIX + ".java.vm.name", System.getProperty("java.vm.name"));
    data.put(PREFIX + ".java.version", System.getProperty("java.version"));

    data.put(PREFIX + ".logger.path",
        Settings.getConfigurationFilePathOnAozanConfiguration());
    data.put(PREFIX + ".logger.path",
        Settings.getLoggerLevelFromAozanConfiguration());
    data.put(PREFIX + ".logger.level",
        Settings.getLoggerPathFromAozanConfiguration());

  }

  @Override
  public void clear() {

  }

}
