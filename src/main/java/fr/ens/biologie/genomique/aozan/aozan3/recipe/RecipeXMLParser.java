package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedAttributes;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getAttribute;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.DummyAzoanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.EmptyRunConfigurationProvider;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProvider;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProviderService;

/**
 * This class define a recipe XML parser.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RecipeXMLParser {

  private final Configuration conf;
  private AozanLogger logger = new DummyAzoanLogger();

  /** Version of the format of the workflow file. */
  private static final String FORMAT_VERSION = "0.1";

  private static final String ROOT_TAG_NAME = "recipe";
  private static final String RECIPE_FORMAT_VERSION_TAG_NAME = "formatversion";
  private static final String RECIPE_NAME_TAG_NAME = "name";
  private static final String RECIPE_CONF_TAG_NAME = "configuration";
  static final String RECIPE_STORAGES_TAG_NAME = "storages";
  private static final String RECIPE_RUN_CONFIGURATION_TAG_NAME =
      "runconfiguration";
  private static final String RECIPE_DATASOURCE_TAG_NAME = "datasource";
  static final String RECIPE_STEPS_TAG_NAME = "steps";

  private static final String RUNCONF_PROVIDER_TAG_NAME = "provider";
  private static final String RUNCONF_CONF_TAG_NAME = "configuration";

  private static final String DATASOURCE_PROVIDER_TAG_NAME = "provider";
  private static final String DATASOURCE_STORAGE_TAG_NAME = "storage";
  private static final String DATASOURCE_CONF_TAG_NAME = "configuration";
  private static final String DATASOURCE_INPROGRESS_ATTR_NAME = "inprogress";

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @return a Recipe object
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public Recipe parse(Path path) throws Aozan3Exception {

    try {
      return parse(Files.newInputStream(path));
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while reading recipe file: " + e.getMessage(), e);
    }
  }

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @return a Recipe object
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public Recipe parse(InputStream is) throws Aozan3Exception {

    Objects.requireNonNull(is);

    this.logger.info("Start parsing the recipe file");

    // Parse file into a Document object
    try {
      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Create Recipe object
      return parse(doc);

    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new Aozan3Exception(
          "Error while parsing recipe file: " + e.getMessage(), e);
    }
  }

  /**
   * Parse XML document.
   * @param doc XML document
   * @return a new Recipe Object
   * @throws Aozan3Exception if an error occurs while parsing the XML document
   */
  public Recipe parse(Document doc) throws Aozan3Exception {

    final NodeList nAnalysisList = doc.getElementsByTagName(ROOT_TAG_NAME);

    Recipe result = null;

    for (int i = 0; i < nAnalysisList.getLength(); i++) {

      if (result != null) {
        throw new Aozan3Exception("Found several recipe in the recipe file");
      }

      Node nNode = nAnalysisList.item(i);
      if (nNode.getNodeType() == Node.ELEMENT_NODE) {
        result = parseRecipeTag((Element) nNode);
      }

    }

    return result;
  }

  /**
   * Parse a recipe tag.
   * @param element element to parse
   * @return a new recipe object
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private Recipe parseRecipeTag(Element element) throws Aozan3Exception {

    // Check allowed child tags of the root tag
    checkAllowedChildTags(element, RECIPE_FORMAT_VERSION_TAG_NAME,
        RECIPE_NAME_TAG_NAME, RECIPE_CONF_TAG_NAME, RECIPE_STORAGES_TAG_NAME,
        RECIPE_RUN_CONFIGURATION_TAG_NAME, RECIPE_DATASOURCE_TAG_NAME,
        RECIPE_STEPS_TAG_NAME);

    // Check version of the XML file
    final String formatVersion =
        nullToEmpty(getTagValue(RECIPE_FORMAT_VERSION_TAG_NAME, element));
    if (!FORMAT_VERSION.equals(formatVersion)) {
      throw new Aozan3Exception(
          "Invalid version of the format of the recipe file.");
    }

    // Get the recipe name
    String recipeName = nullToEmpty(getTagValue(RECIPE_NAME_TAG_NAME, element));

    // Get the recipe configuration
    Configuration recipeConf = new ConfigurationXMLParser(RECIPE_CONF_TAG_NAME,
        "recipe configuration", this.conf, this.logger).parse(element);

    // Create a new recipe
    Recipe recipe = new Recipe(recipeName, recipeConf);

    // Parse storages
    new StoragesXMLParser(recipe).parse(element);

    // Parse run configuration provider
    RunConfigurationProvider runConfProvider =
        parseRunConfigurationTag(recipe, element);

    // Parse the datasource tag
    parseDataSourceTag(recipe, element);

    // Parse steps
    recipe.addSteps(new StepsXMLParser(recipe, runConfProvider).parse(element));

    return recipe;
  }

  /**
   * Parse a run configuration XML tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @return a new RunConfigurationProvider object
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private static RunConfigurationProvider parseRunConfigurationTag(
      final Recipe recipe, final Element rootElement) throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    RunConfigurationProvider result = new EmptyRunConfigurationProvider();

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_RUN_CONFIGURATION_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      if (i > 1) {
        throw new Aozan3Exception("Found more than one \""
            + RECIPE_RUN_CONFIGURATION_TAG_NAME + "\" tag");
      }

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, RUNCONF_PROVIDER_TAG_NAME,
            RUNCONF_CONF_TAG_NAME);

        String providerName =
            nullToEmpty(getTagValue(RUNCONF_PROVIDER_TAG_NAME, element));

        Configuration conf = new ConfigurationXMLParser(RUNCONF_CONF_TAG_NAME,
            "run configuration", recipe.getConfiguration(), recipe.getLogger())
                .parse(element);

        result = RunConfigurationProviderService.getInstance()
            .newService(providerName);
        result.init(conf, recipe.getLogger());
      }
    }

    return result;
  }

  /**
   * Parse a run data source XML tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private void parseDataSourceTag(final Recipe recipe,
      final Element rootElement) throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_DATASOURCE_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      if (i > 0) {
        throw new Aozan3Exception(
            "Found more than one \"" + RECIPE_DATASOURCE_TAG_NAME + "\" tag");
      }

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed attributes for the "storages" tag
        checkAllowedAttributes(element, DATASOURCE_INPROGRESS_ATTR_NAME);

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, DATASOURCE_PROVIDER_TAG_NAME,
            DATASOURCE_STORAGE_TAG_NAME, DATASOURCE_CONF_TAG_NAME);

        final boolean inProgress = parseBoolean(
            nullToEmpty(getAttribute(element, DATASOURCE_INPROGRESS_ATTR_NAME)
                .trim().toLowerCase()));

        String providerName =
            nullToEmpty(getTagValue(DATASOURCE_PROVIDER_TAG_NAME, element));
        String storageName =
            nullToEmpty(getTagValue(DATASOURCE_STORAGE_TAG_NAME, element));

        Configuration conf =
            new ConfigurationXMLParser(DATASOURCE_CONF_TAG_NAME,
                RECIPE_DATASOURCE_TAG_NAME, recipe.getConfiguration(),
                recipe.getLogger()).parse(element);

        recipe.setDataProvider(providerName, storageName, conf);
        recipe.setUseInProgressData(inProgress);
      }
    }
  }

  //
  // Utility parsing methods
  //

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param conf initial configuration
   * @param logger default logger
   */
  public RecipeXMLParser(Configuration conf, AozanLogger logger) {

    requireNonNull(conf);
    this.conf = conf;

    // Set logger
    if (logger != null) {
      this.logger = logger;
    }
  }

}
