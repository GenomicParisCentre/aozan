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

package fr.ens.transcriptome.aozan;

/**
 * A nestable Aozan exception. This class came from from Biojava code.
 * @since 0.8
 * @author Laurent Jourdren
 * @author Matthew Pocock
 */
public class AozanException extends Exception {

  private static final long serialVersionUID = -749903788412172296L;
  
  private Exception exception;

  /**
   * Get the wrapped exception.
   * @return the wrapped exception
   */
  public Exception getWrappedException() {

    return exception;
  }

  //
  // Constructors
  //

  /**
   * Create a new AozanException from the message of another exception.
   * @param e exception with the message to use
   */
  public AozanException(final Exception e) {

    this(e.getMessage());
    this.exception = e;
  }

  /**
   * Create a new AozanException with a message.
   * @param message the message
   */
  public AozanException(final String message) {

    super(message);
  }

  /**
   * Create a new AozanException.
   */
  public AozanException() {

    super();
  }

}
