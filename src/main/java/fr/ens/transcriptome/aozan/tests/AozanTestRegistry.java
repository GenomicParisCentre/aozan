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

package fr.ens.transcriptome.aozan.tests;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanRuntimeException;

/**
 * This class define a registry for Aozan tests.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class AozanTestRegistry {

  private static AozanTestRegistry instance;
  private Map<String, AozanTest> tests = Maps.newHashMap();

  /**
   * Get a the test instance.
   * @param testName name of the test.
   * @return an AozanTest or null is the test does not exists
   */
  public AozanTest get(final String testName) {

    if (testName == null)
      return null;

    return this.tests.get(testName.trim().toLowerCase());
  }

  private void register(final AozanTest test) {

    if (test == null)
      throw new NullPointerException("The AozanTest to register is null");

    if (test.getName() == null)
      throw new AozanRuntimeException("The name of the AozanTest ("
          + test.getClass().getName() + ") to register is null");

    if (this.tests.containsKey(test.getName()))
      throw new AozanRuntimeException("The AozanTest ("
          + test.getName() + ") is already registred");

    this.tests.put(test.getName(), test);
  }

  //
  // Static method
  //

  /**
   * Get the singleton instance of AozanTestRegistry
   * @return the AozanTestRegistry singleton
   */
  public static AozanTestRegistry getInstance() {

    if (instance == null)
      instance = new AozanTestRegistry();

    return instance;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private AozanTestRegistry() {

    final Iterator<AozanTest> it =
        ServiceLoader.load(AozanTest.class).iterator();

    while (it.hasNext()) {

      register(it.next());
    }

  }

}
