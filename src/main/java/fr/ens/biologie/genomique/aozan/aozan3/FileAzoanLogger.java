package fr.ens.biologie.genomique.aozan.aozan3;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

/**
 * This class implements an Aozan Logger based on a log file
 * @author Laurent Jourdren
 * @since 3.0
 */
public class FileAzoanLogger extends AbstractAzoanLogger {

  @Override
  protected Handler createHandler(Configuration conf) throws Aozan3Exception {

    // Get Log path
    String logPath = conf.get("aozan.log", "");
    if (logPath.isEmpty()) {
      throw new Aozan3Exception("No log file defined");
    }

    try {
      return new FileHandler(logPath, true);
    } catch (SecurityException | IOException e) {
      throw new Aozan3Exception(e);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param conf configuration
   * @throws Aozan3Exception if an error occurs while creating the logger
   */
  public FileAzoanLogger(Configuration conf) throws Aozan3Exception {
    super(conf);
  }

}
