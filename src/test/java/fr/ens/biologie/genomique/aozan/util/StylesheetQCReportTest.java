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
package fr.ens.biologie.genomique.aozan.util;

import java.io.StringWriter;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
// JAXP used for XSLT transformations
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.aozan.Globals;
// JUnit classes
import org.junit.Assert;

import org.junit.runners.Suite;
import org.junit.runner.RunWith;

//@RunWith(Suite.class)
//@Suite.SuiteClasses({StylesheetQCReportTest.class})

public class StylesheetQCReportTest {

  private TransformerFactory transFact;

  // /**
  // * All JUnit tests have a constructor that takes the test name.
  // */
  // public StylesheetQCReportTest(String name) {
  // super(name);
  // }

  /**
   * Initialization before each test[...] method is called.
   */
  @Before
  public void init() {
    this.transFact = TransformerFactory.newInstance();
  }

  /**
   * An individual unit test.
   */
  @Test
  public void testTransformReportQC() throws Exception {
    Assert.assertTrue(
        "Invalide syntax XSL " + Globals.EMBEDDED_QC_XSL + " for report QC ",
        isStylesheetValide(Globals.EMBEDDED_QC_XSL, "reportQCXML.xml"));
  }

  @Test
  public void testTransformReportFQS() throws Exception {
    Assert.assertTrue(
        "Invalide syntax XSL "
            + Globals.EMBEDDED_FASTQSCREEN_XSL + " for report fastqscreen",
        isStylesheetValide(Globals.EMBEDDED_FASTQSCREEN_XSL,
            "reportFastqscreenXML.xml"));
  }

  @Test
  public void testTransformReportUndetermined() throws Exception {
    Assert.assertTrue(
        "Invalide syntax XSL "
            + Globals.EMBEDDED_UNDETERMINED_XSL
            + " for report undetermined for lane ",
        isStylesheetValide(Globals.EMBEDDED_QC_XSL,
            "reportUndeterminedXML.xml"));
  }

  private boolean isStylesheetValide(final String stylesheetFile,
      final String reportFile) {
    try {

      Templates templates = this.transFact.newTemplates(
          new StreamSource(fr.ens.biologie.genomique.aozan.QC.class
              .getResourceAsStream(stylesheetFile)));

      Transformer trans = templates.newTransformer();

      // Create the String writer
      final StringWriter writer = new StringWriter();

      // Transform the document
      trans.transform(
          new StreamSource(this.getClass().getResourceAsStream(reportFile)),
          new StreamResult(writer));

      Assert.assertTrue("HTML page generate for " + reportFile + " fail ",
          writer.toString().length() > 0);

      return true;
    } catch (TransformerException ae) {
      return false;
    }

  }

  @RunWith(Suite.class)
  @Suite.SuiteClasses({StylesheetQCReportTest.class})
  public class TestSuite {
  }

}
