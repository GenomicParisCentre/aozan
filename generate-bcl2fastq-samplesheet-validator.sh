#!/bin/bash

# Script can be used with GWT 2.4 or 2.5
PROJECT_NAME=SamplesheetValidator
GIT_REVISION=$(git log -n 1 --pretty='%h (%ad)' --date=short )

# Check environment variable
if [ -z "$GWT_HOME" ]; then
  echo "Error: GWT_HOME environment variable is not set"
  exit 1
fi

BASEDIR=$(dirname $0)
if [ ! -d $BASEDIR/target ]; then
  mkdir $BASEDIR/target
fi

if [ -d "$BASEDIR/target" ]; then
  rm -rf "$BASEDIR/target"
fi

mkdir "$BASEDIR/target"
cd "$BASEDIR/target"

AOZAN_DIR=..
AOZAN_PACKAGE=fr.ens.biologie.genomique.aozan
AOZAN_PACKAGE_PATH=$(echo $AOZAN_PACKAGE | sed 's/\./\//g')
PACKAGE=fr.ens.biologie.genomique.samplesheetvalidator
PACKAGE_PATH=$(echo $PACKAGE | sed 's/\./\//g')

HELP_TEXT_PATH=design-validator-help-page.txt


rm -rf $PROJECT_NAME
$GWT_HOME/webAppCreator -out $PROJECT_NAME $PACKAGE.$PROJECT_NAME
rm  $PROJECT_NAME/src/$PACKAGE_PATH/client/*
rm  $PROJECT_NAME/src/$PACKAGE_PATH/server/*
rm  $PROJECT_NAME/src/$PACKAGE_PATH/shared/*

cp $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml.ori
head -n 20 $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml > $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml.tmp

cat >> $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml.tmp << EOF
  <inherits name='com.google.gwt.http.HTTP' />
EOF

tail -n +21 $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml >> $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml.tmp

rm $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml
mv $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml.tmp $PROJECT_NAME/src/$PACKAGE_PATH/$PROJECT_NAME.gwt.xml

for f in $(echo AozanException.java AozanRuntimeException.java)
do
    sed "s/package $AOZAN_PACKAGE/package $PACKAGE.client/" $AOZAN_DIR/src/main/java/$AOZAN_PACKAGE_PATH/$f > $PROJECT_NAME/src/$PACKAGE_PATH/client/$f
done

for f in $(echo SampleSheet.java Sample.java SampleSheetUtils.java SampleSheetCheck.java)
do
    sed "s/package $AOZAN_PACKAGE.illumina.samplesheet/package $PACKAGE.client/" $AOZAN_DIR/src/main/java/$AOZAN_PACKAGE_PATH/illumina/samplesheet/$f | sed "s/import $AOZAN_PACKAGE.illumina.samplesheet.io/import $PACKAGE.client/" | sed "s/import $AOZAN_PACKAGE.illumina.samplesheet/import $PACKAGE.client/" |  sed "s/import $AOZAN_PACKAGE/import $PACKAGE.client/" |  sed "s/import static $AOZAN_PACKAGE.illumina.samplesheet/import static $PACKAGE.client/"  > $PROJECT_NAME/src/$PACKAGE_PATH/client/$f
done

for f in $(echo SampleSheetReader.java SampleSheetDiscoverFormatParser.java SampleSheetReaderUtils.java SampleSheetParser.java SampleSheetV1Parser.java SampleSheetV2Parser.java)
do
    sed "s/package $AOZAN_PACKAGE.illumina.samplesheet.io/package $PACKAGE.client/" $AOZAN_DIR/src/main/java/$AOZAN_PACKAGE_PATH/illumina/samplesheet/io/$f | sed "s/import $AOZAN_PACKAGE.illumina.samplesheet.io/import $PACKAGE.client/" | sed "s/import $AOZAN_PACKAGE.illumina.samplesheet/import $PACKAGE.client/" |  sed "s/import $AOZAN_PACKAGE/import $PACKAGE.client/" | sed "s/import static $AOZAN_PACKAGE.illumina.samplesheet.io/import static $PACKAGE.client/"  > $PROJECT_NAME/src/$PACKAGE_PATH/client/$f
done

cat > $PROJECT_NAME/war/$HELP_TEXT_PATH << EOF
Help page for validator sample-sheet for bcl2fastq-2.x tool (Illumina: http://support.illumina.com/downloads/bcl2fastq-conversion-software-v217.html):

The bcl2fastq 2.x sample sheet validator helps users to check their run design. This tool uses only HTML and Javascript. No data are sent to our servers when you use this tool.
This tool generates a sample sheet only in CSV format with a very strict syntax.
It ensures you that the demultiplexing will not fail because the sample sheet is not correct.

You can use the validator in association with Aozan, a tool that automatically handles data transfer, demultiplexing conversion and quality control once an Illumina sequencer run is over (developed by the genomics facility of the Institut de biologie de l'École normale supérieure and available at http://outils.genomique.biologie.ens.fr/aozan/).

The validator is very simple to use:
1/ copy the sample sheet from your spreadsheet or CSV/TSV format in the first tab of the left panel with the column headers;
2/ click on the "Check bcl2fastq samplesheet" to check and convert to CSV (the CSV output appears in the second tab).

The verification step opens an alert window in two cases :
- error message : the conversion is stopped, the sample sheet format is invalid;
- warning message : the conversion has been performed and some warnings exits about your sample sheet.

The sample sheet input format is strict. Some fields can't be null or empty.
- a line containing only '[Data]' must be included in the header your sample sheet.
- Some column must have specific values:
        - Sample_ID: name used in the FASTQ file (THIS COLUMN IS MANDATORY);
        - Lane: lane number between 1 and 8;
        - Sample_Name: descriptive name used in the FASTQ file;
        - Sample_Ref: reference name. You can use aliases defined in the "Reference aliases" tab. You can put a key-value list  (e.g. mm10=Mus musculus) in this tab to define the allowed aliases. If the value does not exists in the list, a warning message will be thrown;
        - index: sequence of the first index or an alias. You can use aliases defined in the "Index aliases" tab. You must put a key-value list (e.g. B1=CGATGT) in this tab to define the allowed aliases.
        - index2: sequence of the second index or an alias, like the index field (Optional);
        - Description (Optional);
        - Sample_Project: sample project;

Possible reasons for error/warning messages :
- missing [Data] line in the header;
- column headers missing in the line next to [Data] line;
- invalid mandatory header name;
- missing or empty mandatory field;
- lane number not in expected range [1,8];
- same sample_ID defined with different indexes;
- same sample_ID defined in several projects;
- same sample_ID defined in several lanes;
- misspelled fields : Sample_ID, Sample_Ref, Sample_Name or Sample_Project values should be written only with letters, digits, '-' or '_';
- misspelled index : the index sequence should be written only with the letters A, T, C and G;
- same sample_ID defined in several lanes, but not using the same index.

Using the aliases tab (optional) :
There are two tabs that allow you to copy a key-value list used to check the sample-sheet for the following fields : index, index2 and Sample_Ref.
For the index, you can use the aliases to replace them by the real value in the CSV output.
For the Sample_Ref, you can use the aliases to check that one reference is always written the same way. It is not required by bcl2fastq 2.x. If a Sample_Ref is not found in the list, the program prints a warning message, sometimes with a proposition to fix it.


NB: the code of this tool is available on GitHub at:
https://github.com/GenomicParisCentre/aozan/blob/master/generate-bcl2fastq-samplesheet-validator.sh

EOF

#
# Add main class
#

cat > $PROJECT_NAME/src/$PACKAGE_PATH/client/$PROJECT_NAME.java << EOF
package $PACKAGE.client;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.core.client.GWT;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class $PROJECT_NAME implements EntryPoint {

  private static final String DEFAULT_INDEXES = "#Index id, Sequence\n"
      + "I1=ATCACG\n" + "I2=CGATGT\n" + "I3=TTAGGC\n" + "I4=TGACCA\n"
      + "I5=ACAGTG\n" + "I6=GCCAAT\n" + "I7=CAGATC\n" + "I8=ACTTGA\n"
      + "I9=GATCAG\n" + "I10=TAGCTT\n" + "I11=GGCTAC\n" + "I12=CTTGTA\n"
      + "I13=AGTCAA\n" + "I14=AGTTCC\n" + "I15=ATGTCA\n" + "I16=CCGTCC\n"
      + "I17=GTAGAG\n" + "I18=GTCCGC\n" + "I19=GTGAAA\n" + "I20=GTGGCC\n"
      + "I21=GTTTCG\n" + "I22=CGTACG\n" + "I23=GAGTGG\n" + "I24=GGTAGC\n"
      + "I25=ACTGAT\n" + "I26=ATGAGC\n" + "I27=ATTCCT\n" + "I28=CAAAAG\n"
      + "I29=CAACTA\n" + "I30=CACCGG\n" + "I31=CACGAT\n" + "I32=CACTCA\n"
      + "I33=CAGGCG\n" + "I34=CATGGC\n" + "I35=CATTTT\n" + "I36=CCAACA\n"
      + "I37=CGGAAT\n" + "I38=CTAGCT\n" + "I39=CTATAC\n" + "I40=CTCAGA\n"
      + "I41=GACGAC\n" + "I42=TAATCG\n" + "I43=TACAGC\n" + "I44=TATAAT\n"
      + "I45=TCATTC\n" + "I46=TCCCGA\n" + "I47=TCGAAG\n" + "I48=TCGGCA\n"
      + "B1=CGATGT\n"
      + "B2=TGACCA\n" + "B3=ACAGTG\n" + "B4=GCCAAT\n" + "B5=CAGATC\n"
      + "B6=CTTGTA\n" + "B7=ATCACG\n" + "B8=TTAGGC\n" + "B9=ACTTGA\n"
      + "B10=GATCAG\n" + "B11=TAGCTT\n" + "B12=GGCTAC\n" + "B13=AGTCAA\n"
      + "B14=AGTTCC\n" + "B15=ATGTCA\n" + "B16=CCGTCC\n" + "B17=GTAGAG\n"
      + "B18=GTCCGC\n" + "B19=GTGAAA\n" + "B20=GTGGCC\n" + "B21=GTTTCG\n"
      + "B22=CGTACG\n" + "B23=GAGTGG\n" + "B24=GGTAGC\n" + "B25=ACTGAT\n"
      + "B26=ATGAGC\n" + "B27=ATTCCT\n" + "B28=CAAAAG\n" + "B29=CAACTA\n"
      + "B30=CACCGG\n" + "B31=CACGAT\n" + "B32=CACTCA\n" + "B33=CAGGCG\n"
      + "B34=CATGGC\n" + "B35=CATTTT\n" + "B36=CCAACA\n" + "B37=CGGAAT\n"
      + "B38=CTAGCT\n" + "B39=CTATAC\n" + "B40=CTCAGA\n" + "B41=GCGCTA\n"
      + "B42=TAATCG\n" + "B43=TACAGC\n" + "B44=TATAAT\n" + "B45=TCATTC\n"
      + "B46=TCCCGA\n" + "B47=TCGAAG\n" + "B48=TCGGCA\n" + "E1=ATCACG\n"
      + "E2=CGATGT\n" + "E3=TTAGGC\n" + "E4=TGACCA\n" + "E5=ACAGTG\n"
      + "E6=GCCAAT\n" + "E7=CAGATC\n" + "E8=ACTTGA\n" + "E9=GATCAG\n"
      + "E10=TAGCTT\n" + "E11=GGCTAC\n" + "E12=CTTGTA\n"
      + "M1=AACCAG\n" + "M2=TGGTGA\n" + "M3=AGTGAG\n" + "M4=GCACTA\n"
      + "M5=ACCTCA\n" + "M6=GTGCTT\n" + "M7=AAGCCT\n" + "M8=GTCGTA\n"
      + "M9=AAGAGG\n" + "M10=GGAGAA\n" + "M11=AGCATG\n" + "M12=GAGTCA\n"
      + "M13=CGTAGA\n" + "M14=TCAGAG\n" + "M15=CACAGT\n" + "M16=TTGGCA\n";


  private static String DEFAULT_RESULT_MSG = "<pre>No valid samplesheet entered.</pre>";
  // if none
  private static String DEFAULT_GENOMES_MSG = "[No genomes list received.]";

  private final TextArea inputTextarea = new TextArea();
  private final TextArea indexesTextarea = new TextArea();
  private final TextArea genomesTextarea = new TextArea();
  private final TextArea helpTextarea = new TextArea();
  private final HTML outputHTML = new HTML();
  private final TextBox flowcellTextBox = new TextBox();
  private final Button button = new Button("Check the bcl2fastq samplesheet");

  private List<String> currentProjects = new ArrayList<String>();

  private boolean first = true;

  public static final void updateDesignWithIndexes(final SampleSheet design,
      final String indexesAvailable) {

    if (design == null || indexesAvailable == null)
      return;

    final Map<String, String> map = new HashMap<String, String>();

    String[] lines = indexesAvailable.split("\n");

    for (String line : lines) {

      if (line.trim().startsWith("#"))
        continue;

      String[] fields = line.split("=");
      if (fields.length != 2)
        continue;
      map.put(fields[0].trim(), fields[1].trim());
    }

    for (Sample sample : design) {

      final String index1 = sample.getIndex1();
      final String index2 = sample.getIndex2();

      if (index1 != null) {
        if (map.containsKey(index1)) {
          sample.setIndex1(map.get(index1));
        }
      }

      if (index2 != null) {
        if (map.containsKey(index2)) {
          sample.setIndex1(map.get(index2));
        }
      }

      /*
      // Case multi-indexes
      final String[] indexes = sample.getIndex().split("-");
      String res = null;

      for (int i=0; i < indexes.length; i++){
        if (map.containsKey(indexes[i])){

          if (res == null){
            // First index
            res = map.get(indexes[i]);
          } else {
            res += "-" + map.get(indexes[i]);
          }
        }
      }

      // Update sample index
      if (res != null){
        sample.setIndex(res);
      }*/
    }

  }

  private String getFlowcellId(final String s) {

    if (s == null || s.trim().length() == 0)
      return null;

    if (s.indexOf('_') == -1)
      return s.trim();

    String[] fields = s.split("_");

    if (fields==null || fields.length!=4)
      return null;

    String flowcellId = fields[3];
    if (flowcellId==null)
      return null;

    flowcellId = flowcellId.trim();
    if (flowcellId.length()<2)
      return null;

    return flowcellId.substring(1);
  }

  public final List<String> checkProjectCasavaDesign(final SampleSheet design) {

    if (currentProjects.isEmpty())
      return Collections.EMPTY_LIST;

    boolean firstLine = true;

    List<String> warnings = new ArrayList<String>();

    // Build second project list without case
    Map<String, String> projectsNameWithoutCase = new HashMap<String, String>();

    for (String project : currentProjects){
      projectsNameWithoutCase.put(removeCaseNameProject(project), project);
    }


    // Build projects list from sample sheet
    Set<String> projectsDesign = new HashSet<String>();
    for (Sample sample : design) {
      projectsDesign.add(sample.getSampleProject());
    }

    // Compare each project to current projects
    for (String projectDesign : projectsDesign) {
      // Not found in current project
      if (! currentProjects.contains(projectDesign)){

        // Check in second list
        String projectDesignWithoutCase = removeCaseNameProject(projectDesign);

        // Skip PhiX control
        if (projectDesignWithoutCase.startsWith("control"))
          continue;

        if (projectsNameWithoutCase.containsKey(projectDesignWithoutCase)){
          warnings.add("Project "+ projectDesign + ": good name may be "+ projectsNameWithoutCase.get(projectDesignWithoutCase) + " ?");
        } else {
          warnings.add("Check project: " + projectDesign + " not found in projects");
        }
      }
    }

    return warnings;
  }

  private static String removeCaseNameProject(final String project){
    return project.replaceAll("-","_").trim().toLowerCase();
  }

  public final List<String> checkGenomesCasavaDesign(final SampleSheet design, final String genomesList) {

    if (genomesList == null){
      return Collections.emptyList();
    }

    List<String> availableGenomes = new ArrayList<String>();
    List<String> warnings = new ArrayList<String>();
    boolean firstLine = true;

    for (String line : genomesList.trim().split("\n")) {
      if (line.indexOf("=") > -1) {
        String[] l = line.split("=");
        availableGenomes.add(trimSpecificString(l[0]));
        availableGenomes.add(trimSpecificString(l[1]));
      }
    }

    Set<String> genomesDesign = new HashSet<String>();
     for (Sample sample : design) {
      genomesDesign.add(sample.getSampleRef());
     }

    for (String genomeSample : genomesDesign) {
      if (genomeSample != null && !(availableGenomes.contains(trimSpecificString(genomeSample)))){
        warnings.add("No genome found for '" + genomeSample + "' (optional for Fastq Screen)");
      }
    }

    return warnings;
  }

  private static String trimSpecificString(final String s) {

    String trimmed = s.trim().toLowerCase();
    String carToReplace = ".,;:/-_'";

    for (int i = 0; i < carToReplace.length(); i++) {
      // Check present character to replace by space
      if (trimmed.indexOf(carToReplace.charAt(i)) != -1)
        trimmed = trimmed.replace(carToReplace.charAt(i),' ');
    }

    return trimmed.toString();
  }

  private String createWarningMessage(List<String> warnings) {

    final StringBuilder sb = new StringBuilder();
    sb.append("Warnings:\n");

    for (String warn : warnings) {
      sb.append("  - ");
      sb.append(warn);
      sb.append('\n');
    }

    sb.append("\nAre you sure that your samplesheet is correct?");

    return sb.toString();
  }


  private void retrieveGenomesList(final String list, final String url){

    final String txt = (list == null || list.trim().length() == 0) ? DEFAULT_GENOMES_MSG : list.trim();

    if (url == null || url.trim().length() == 0){
      genomesTextarea.setText(txt);

    } else {
      try {

       loadGenomesFile(url);

      } catch(Exception e){
        // Fail load file, used list send by param genomes
        Window.alert("Couldn't retrieve list: " + e.getMessage());
        genomesTextarea.setText(txt);
      }
    }
  }


  private void loadGenomesFile(final String url) throws Exception {

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));

    Request request = builder.sendRequest(null, new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        Window.alert("Couldn't retrieve list url (" + url + ")");

      }

      public void onResponseReceived(Request request, Response response) {
        if (200 == response.getStatusCode()) {

          genomesTextarea.setText(response.getText());

        } else {
          Window.alert("Couldn't retrieve list status (" + response.getStatusText() + ")");
        }
      }
    });
  }

  private void retrieveIndexesList(final String list, final String url){

    final String txt = list == null || list.trim().length() == 0 ? DEFAULT_INDEXES : list.trim();

    if (url == null || url.trim().length() == 0){
      indexesTextarea.setText(txt);

    } else {
      try {

        loadIndexesFile(url);

      } catch(Exception e){
        // Fail load file, used list send by param genomes
        Window.alert("Couldn't retrieve list: " + e.getMessage());
        indexesTextarea.setText(txt);
      }
    }
  }

  private void loadIndexesFile(final String url) throws Exception {

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
    final String responseText;

    Request request = builder.sendRequest(null, new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        Window.alert("Couldn't retrieve list url (" + url + ")");

      }

      public void onResponseReceived(Request request, Response response) {
        if (200 == response.getStatusCode()) {

          indexesTextarea.setText(response.getText());

        } else {
          Window.alert("Couldn't retrieve list status (" + response.getStatusText() + ")");
        }
      }
    });
  }


  private void retrieveCurrentProjectList(final String url){

    if (!(url == null || url.trim().length() == 0)){
      try {

        loadProjectList(url);

      } catch(Exception e){
        // Fail load file, used list send by param genomes
        Window.alert("Couldn't retrieve current project list: " + e.getMessage());
      }
    }
  }

  private void loadProjectList(final String url) throws Exception {

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
    final String responseText;

    Request request = builder.sendRequest(null, new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        Window.alert("Couldn't retrieve list url (" + url + ")");

      }

      public void onResponseReceived(Request request, Response response) {
        if (200 == response.getStatusCode()) {
          String s = response.getText();
          if (s.length() > 0)
            currentProjects = Arrays.asList(s.split(","));

        } else {
          Window.alert("Couldn't retrieve list status (" + response.getStatusText() + ")");
        }
      }
    });

  }


  // Load help text file from url
  public void loadTextHelp() {

    final String defaultTxt = "Help for the validator samplesheet coming soon.";
    final String url = "$HELP_TEXT_PATH";

    try {
      RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));

      Request request = builder.sendRequest(null, new RequestCallback() {

        public void onError(Request request, Throwable exception) {
          helpTextarea.setText(defaultTxt+" 1");
        }

        public void onResponseReceived(Request request, Response response) {
          // Success
          if (200 == response.getStatusCode()) {
            helpTextarea.setText(response.getText());
          } else {
            // Fail
            helpTextarea.setText(defaultTxt+" 2");
          }
        }
      });
    } catch (Exception e){
      helpTextarea.setText(defaultTxt+" 3");
    }
  }


  public void onModuleLoad() {

    // Set the layouts
    final TabLayoutPanel tp = new TabLayoutPanel(1.5, Unit.EM);
    tp.add(new ScrollPanel(inputTextarea), "[Input Samplesheet]");
    tp.add(new ScrollPanel(indexesTextarea), "[Index Aliases]");
    tp.add(new ScrollPanel(genomesTextarea), "[Reference Aliases]");


    final TabLayoutPanel tpo = new TabLayoutPanel(1.5, Unit.EM);
    tpo.add(new ScrollPanel(helpTextarea), "[Help]");
    tpo.add(outputHTML, "[Output Samplesheet (CSV)]");

    tp.setHeight("100%");
    tp.setWidth("100%");
    tpo.setHeight("100%");
    tpo.setWidth("100%");

    //RootLayoutPanel rp = RootLayoutPanel.get();
    //rp.add(dlp);

    //RootPanel.get("flowcellidFieldContainer").add(flowcellTextBox);
    RootPanel.get("sendButtonContainer").add(button);
    RootPanel.get("tabsContainer").add(tp);
    RootPanel.get("tabsContainerOutput").add(tpo);

    retrieveIndexesList(Window.Location.getParameter("indexesaliaseslist"), Window.Location.getParameter("indexesaliasesurl"));

    // Initialize widget values
    // indexesTextarea.setText(DEFAULT_INDEXES);
    indexesTextarea.setVisibleLines(40);
    indexesTextarea.setSize("99%","100%");
    //indexesTextarea.setCharacterWidth(150);

    retrieveGenomesList(Window.Location.getParameter("genomesaliaseslist"), Window.Location.getParameter("genomesaliasesurl"));

    genomesTextarea.setVisibleLines(40);
    genomesTextarea.setSize("99%","100%");
    //genomesTextarea.setCharacterWidth(150);


    retrieveCurrentProjectList(Window.Location.getParameter("projectnamesurl"));

    helpTextarea.setVisibleLines(40);
    helpTextarea.setSize("99%","100%");
    loadTextHelp();

    // flowcellTextBox.setText(Window.Location.getParameter("id"));

    inputTextarea.setText("[Paste here your bcl2fastq samplesheet]");
    //inputTextarea.setCharacterWidth(150);
    inputTextarea.setVisibleLines(40);
    inputTextarea.setSize("99%","100%");

    outputHTML.setHTML(DEFAULT_RESULT_MSG);

    // Set the action on button click
    this.button.addClickHandler(new ClickHandler() {

      public void onClick(ClickEvent event) {

        // Get input text
        String inputText = inputTextarea.getText();

        // Clear ouput
        outputHTML.setHTML(DEFAULT_RESULT_MSG);

        tp.selectTab(0);

        try {
          final SampleSheet design;

          if (inputText.indexOf('\t')!=-1)
            design = SampleSheetUtils.parseTabulatedSamplesheet(inputText);
          else
            design = SampleSheetUtils.parseCSVSamplesheet(inputText);

          updateDesignWithIndexes(design,
              indexesTextarea.getText());

          // Get the flowcell id
          final String flowcellId = getFlowcellId(flowcellTextBox.getText());
          //if (flowcellId == null)
          //  throw new EoulsanException("Invalid run id: " + flowcellTextBox.getText());

          // Check Casava design
          final List<String> warnings =
            SampleSheetCheck.checkSampleSheet(design, flowcellId);

          // Check genomes Casava design
          warnings.addAll(checkGenomesCasavaDesign(design, genomesTextarea.getText()));

          // Check project names Casava design
          warnings.addAll(checkProjectCasavaDesign(design));

          if (warnings.size()==0 || Window.confirm(createWarningMessage(warnings))) {

            outputHTML.setHTML("<pre>"
                + SampleSheetUtils.toSampleSheetV2CSV(design) + "</pre>");
            tpo.selectTab(1);
          }
        } catch (IOException e) {
          Window.alert("Invalid samplesheet: " + e.getMessage());
        } catch (AozanException e) {
          Window.alert("Invalid samplesheet: " + e.getMessage());
        }

      }

    });

    // Clear tip message in input text area
    this.inputTextarea.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {

        if (first) {

          inputTextarea.setText("");
          first = false;
        }
      }
    });

  }
}
EOF

cat > $PROJECT_NAME/war/$PROJECT_NAME.html.tmp << EOF
<!doctype html>
<!-- The DOCTYPE declaration above will set the     -->
<!-- browser's rendering engine into                -->
<!-- "Standards Mode". Replacing this declaration   -->
<!-- with a "Quirks Mode" doctype is not supported. -->

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">

    <!--                                                               -->
    <!-- Consider inlining CSS to reduce the number of requested files -->
    <!--                                                               -->
    <link type="text/css" rel="stylesheet" href="$PROJECT_NAME.css">

    <!--                                           -->
    <!-- Any title is fine                         -->
    <!--                                           -->


<style type="text/css">
    body {
        width: 1280px;
    }
    div.header {
	background-color: #C1C4CA;
        padding: 15px;
        height: 90px;
        font-size: 120%;
        font-weight: bold;
    }
    <title>Bcl2Fastq version 2 Samplesheet Validator</title>
  </style>
    <!--                                           -->
    <!-- This script loads your compiled module.   -->
    <!-- If you add any GWT meta tags, they must   -->
    <!-- be added before this line.                -->
    <!--                                           -->
    <script language="javascript">
      /*document.domain = "outils.genomique.biologie.ens.fr";*/
      document.domain = "idunn.ens.fr";

      var loc = window.location.href+'';
      if (loc.indexOf('http://')==0){
          window.location.href = loc.replace('http://','https://');
      }

    </script>
    <script type="text/javascript" language="javascript" src="samplesheetvalidator/samplesheetvalidator.nocache.js"></script>
  </head>

  <!--                                           -->
  <!-- The body can have arbitrary html, or      -->
  <!-- you can leave the body empty if you want  -->
  <!-- to create a completely dynamic UI.        -->
  <!--                                           -->
  <body style="margin:auto;">

    <!-- OPTIONAL: include this if you want history support -->
    <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>

    <!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
    <noscript>
      <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
      </div>
    </noscript>


    <div class="header">
      <a href="https://www.genomique.biologie.ens.fr" ><img src="https://outils.genomique.biologie.ens.fr/aozan/images/logo_genomicpariscentre-90pxh.png" alt="logo genomic paris centre" align="left"/></a>
      <a href="http://outils.genomique.biologie.ens.fr/aozan/" ><img src="http://outils.genomique.biologie.ens.fr/aozan/images/aozan_qc_logo.png" alt="logo aozan" align="right" height="70px" style="padding-top:10px;"/></a>
      <h1 align="center">Bcl2fastq 2 samplesheet validator</h1>
    </div>
    <div>
    <table align="center">
      <!--tr>
        <td colspan="2" style="font-weight:bold;">Please enter your name:</td>
      </tr-->
      <tr>
        <!--td>Flow cell id or run id (optional):</td>
        <td id="flowcellidFieldContainer"></td-->
        <!--td id="nameFieldContainer"></td-->
        <td id="sendButtonContainer"></td>
      </tr>
      <tr>
        <!td colspan="2" style="color:red;" id="errorLabelContainer"></td-->
      </tr>
    </table>

    <!--p/-->

    <table align="center" width="90%" >
      <tr><td id="tabsContainer" height="700px"/><td id="tabsContainerOutput" height="700px"/></tr>
    </table>

    <!--p/-->

<h4 align="left">No data is sent to our servers when you use this tool.</h4>
<h4 align="center">__VERSION__</h4>
    <!--table align="center" >
      <tr><td id="sendButtonContainer/></tr>
    </table-->
    <div>


  </body>
</html>
EOF

if [ -z "$GIT_REVISION" ]; then
    GIT_REVISION=""
else
    GIT_REVISION="Revision $GIT_REVISION"
fi
sed "s/__VERSION__/$GIT_REVISION/" $PROJECT_NAME/war/$PROJECT_NAME.html.tmp   > $PROJECT_NAME/war/$PROJECT_NAME.html
rm $PROJECT_NAME/war/$PROJECT_NAME.html.tmp

# Compile

cd $PROJECT_NAME
ant build
if [ $? -ne 0 ]; then
  exit 1
fi

mv war/$PROJECT_NAME.html war/index.html
mv war ../$PROJECT_NAME-tmp
rm -rf ../$PROJECT_NAME-tmp/WEB-INF
cd ..
rm -rf $PROJECT_NAME
mv $PROJECT_NAME-tmp `echo $PROJECT_NAME | tr '[A-Z]' '[a-z]'`

