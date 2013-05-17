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

import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeNames;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader;
import fr.ens.transcriptome.aozan.collectors.interopfile.ErrorMetricsOutReader;
import fr.ens.transcriptome.aozan.collectors.interopfile.ExtractionMetricsOutReader;
import fr.ens.transcriptome.aozan.collectors.interopfile.TileMetricsOutReader;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class define a Collector for read?.xml files.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ReadCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "read";

  private String RTAOutputDirPath;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final Properties properties) {

    if (properties == null)
      return;

    this.RTAOutputDirPath = properties.getProperty(QC.RTA_OUTPUT_DIR);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Collect read info
    final int readCount = data.getInt("run.info.read.count");

    if (new File(this.RTAOutputDirPath + "/Data/reports/Summary", "read10.xml")
        .exists()) {

      for (int i = 1; i <= readCount; i++) {
        File readInfoFile =
            new File(this.RTAOutputDirPath + "/Data/reports/Summary", "read"
                + i + ".xml");
        collectRead(data, readInfoFile);
      }
    } else {

      // File read1.xml doesn't exist, reading binary files in InterOp directory
      collectRead(data);
    }

  }

  private void collectRead(final RunData data) throws AozanException {
    // Collect
    System.out.println("read interop file");

    AbstractBinaryIteratorReader.setDirectory(this.RTAOutputDirPath
        + "/InterOp/");
    new TileMetricsOutReader().collect(data);
    new ErrorMetricsOutReader().collect(data);
    new ExtractionMetricsOutReader().collect(data);
  }

  /**
   * Collect data for a read?.xml file
   * @param data RunData object
   * @param readInfoFile the read?.xml file
   * @throws AozanException if an error occurs while collecting data
   */
  private void collectRead(final RunData data, final File readInfoFile)
      throws AozanException {

    if (data == null)
      return;

    try {

      // Create the input stream
      final InputStream is = new FileInputStream(readInfoFile);

      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Parse the document
      parse(doc, data);

      is.close();
    } catch (IOException e) {

      throw new AozanException(e);

    } catch (SAXException e) {

      throw new AozanException(e);
    } catch (ParserConfigurationException e) {

      throw new AozanException(e);
    }
  }

  /**
   * Parse the XML document.
   * @param document Document DOM object
   * @param data the result
   */
  private void parse(final Document document, final RunData data) {

    for (Element e : XMLUtils.getElementsByTagName(document, "Summary")) {

      int readNumber = -1;
      double densityRatio = -1.0;
      String readType = "";

      // Parse Summary tag attributes
      for (String attributeName : getAttributeNames(e)) {

        if ("Read".equals(attributeName))
          readNumber = Integer.parseInt(getAttributeValue(e, attributeName));
        else if ("ReadType".equals(attributeName))
          readType = getAttributeValue(e, attributeName);
        else if ("densityRatio".equals(attributeName))
          densityRatio =
              Double.parseDouble(getAttributeValue(e, attributeName));
      }

      final String prefix = "read" + readNumber;
      data.put(prefix + ".density.ratio", densityRatio);
      data.put(prefix + ".type", readType);

      // Parse Lane tag
      for (Element laneElement : XMLUtils.getElementsByTagName(e, "Lane")) {

        int lane = -1;
        Map<String, String> map = Maps.newLinkedHashMap();

        for (String key : XMLUtils.getAttributeNames(laneElement)) {

          final String value = laneElement.getAttribute(key);

          if ("key".equals(key))
            lane = Integer.parseInt(value);
          else
            map.put(convertKey(key), value);
        }

        for (Map.Entry<String, String> entry : map.entrySet())
          data.put(prefix + ".lane" + lane + "." + entry.getKey(),
              entry.getValue());
      }
    }
  }

  /**
   * Convert automatically attribute names to keys in RunData keys format.
   * @param key key to convert
   * @return the converted key
   */
  private static String convertKey(final String key) {

    if (key == null)
      return null;

    if (key.length() == 1)
      return key.toLowerCase();

    String k = key;
    if (k.endsWith("SD"))
      k = k.substring(0, k.length() - 2) + ".sd";

    k = k.replace("PhiX", ".phix");

    final StringBuilder sb = new StringBuilder();
    final int len = k.length();

    int lastUp = 0;

    boolean lastDigit = false;
    int startWord = 0;

    for (int i = 1; i < len; i++) {

      final char c = k.charAt(i);
      final boolean up = Character.isUpperCase(c);
      final boolean digit = Character.isDigit(c);

      if (up && lastUp > 0) {
        lastUp++;
        continue;
      }

      if (digit && lastDigit)
        continue;

      if (lastUp > 0 && !up) {

        if (i > 1 && lastUp > 2) {
          sb.append(k.substring(startWord, i - 1));
          sb.append('.');
          startWord = i - 1;
        }

        lastUp = 0;
        continue;
      }

      if (up || digit) {
        sb.append(k.substring(startWord, i));
        sb.append('.');
        startWord = i;

        lastUp = up ? 1 : 0;
        lastDigit = digit;
      }

    }
    sb.append(k.substring(startWord));

    return sb.toString();
  }

  @Override
  public void clear() {
  }
}
