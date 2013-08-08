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

package fr.ens.transcriptome.aozan.collectors.interop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an iterator on Illumina Metrics for reading binary files
 * from the InterOp directory. It allow to parse all records.
 * @author Sandrine Perrin
 * @since 1.1
 */
public abstract class AbstractBinaryFileReader<M> {

  protected String dirInterOpPath;

  // 2 bytes: 1 for file version number and 1 for length for each record
  protected static final int HEADER_SIZE = 2;

  /**
   * @return
   */
  abstract public String getName();

  /**
   * @return
   */
  abstract protected File getMetricsFile();

  /**
   * @return
   */
  abstract protected int getExpectedRecordSize();

  /**
   * @return
   */
  abstract protected int getExpectedVersion();

  public String getDirPathInterOP() {
    return this.dirInterOpPath;
  }

  /**
   * @return
   * @throws AozanException
   */
  public List<M> getSetIlluminaMetrics() throws AozanException {

    List<M> collection = Lists.newArrayList();

    final ByteBuffer buf;
    final byte[] header = new byte[HEADER_SIZE];

    final byte[] element = new byte[getExpectedRecordSize()];
    final ByteBuffer recordBuf = ByteBuffer.wrap(element);
    recordBuf.order(ByteOrder.LITTLE_ENDIAN);

    try {
      FileUtils.checkExistingFile(getMetricsFile(), "Error binary file "
          + getMetricsFile().getAbsolutePath() + " doesn't exist !");

      final FileInputStream is = new FileInputStream(getMetricsFile());
      final FileChannel channel = is.getChannel();
      final long fileSize = channel.size();

      // Copy binary file in buffer
      buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
      buf.order(ByteOrder.LITTLE_ENDIAN);
      channel.close();
      is.close();

    } catch (IOException e) {
      throw new AozanException(e.getMessage());
    }

    // check version file
    if (HEADER_SIZE > 0) {
      ByteBuffer b = ByteBuffer.allocate(HEADER_SIZE);
      b.order(ByteOrder.LITTLE_ENDIAN);
      b = buf.get(header);
      b.position(0);

      checkVersionFile(b);
    }

    // Build collection of illumina metrics
    while (buf.limit() - buf.position() >= getExpectedRecordSize()) {
      recordBuf.position(0);
      buf.get(element);
      recordBuf.position(0);

      // collection.add(new IlluminaMetrics(recordBuf));
      addIlluminaMetricsInCollection(collection, recordBuf);
    }

    return collection;
  }

  /**
   * Build a set of a type of illumina metrics (M) according to the interop file
   * reading
   * @param collection list of illumina metrics
   * @param bb ByteBuffer contains the value corresponding to one record
   */
  abstract protected void addIlluminaMetricsInCollection(
      final List<M> collection, final ByteBuffer bb);

  /**
   * @param header
   * @throws AozanException occurs if the checking fails
   */
  private void checkVersionFile(final ByteBuffer header) throws AozanException {

    // Get the version, should be EXPECTED_VERSION
    final int actualVersion = uByteToInt(header.get());
    if (actualVersion != getExpectedVersion()) {
      throw new AozanException(getName()
          + " expects the version number to be " + getExpectedVersion()
          + ".  Actual Version in Header( " + actualVersion + ")");
    }

    // Check the size record needed
    final int actualRecordSize = uByteToInt(header.get());
    if (getExpectedRecordSize() != actualRecordSize) {
      throw new AozanException(getName()
          + " expects the record size to be " + getExpectedRecordSize()
          + ".  Actual Record Size in Header( " + actualRecordSize + ")");
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param dirPath path to the interop directory for a run
   * @throws AozanException
   */
  AbstractBinaryFileReader(final String dirPath) throws AozanException {
    dirInterOpPath = dirPath;

    if (dirInterOpPath == null)
      throw new AozanException("None path to InterOp directory provided");

    if (!new File(dirInterOpPath).exists())
      throw new AozanException("Path to interOp directory doesn't exists "
          + dirInterOpPath);
  }

  /** Convert an unsigned byte to a signed int */
  public static final int uByteToInt(final byte unsignedByte) {
    return unsignedByte & 0xFF;
  }

  /** Convert an unsigned byte to a signed short */
  public static final int uByteToShort(final byte unsignedByte) {
    return (short) unsignedByte & 0xFF;
  }

  /** Convert an unsigned short to an int */
  public static final int uShortToInt(final short unsignedShort) {
    return unsignedShort & 0xFFFF;
  }

  /** Convert an unsigned int to a long */
  public static final long uIntToLong(final int unsignedInt) {
    return unsignedInt & 0xFFFFFFFFL;
  }

}
