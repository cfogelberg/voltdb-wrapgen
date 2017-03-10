package uk.co.bristlecone.voltdb.wrapgen.console;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.bristlecone.voltdb.wrapgen.WrapgenRuntimeException;

/**
 * Represents the configuration of a console-wrapgen invocation. Constructed based on the command line arguments passed
 * to the tool.
 *
 * @author christo
 */
public class Configuration {
  private static Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

  private final boolean showHelp;
  private final boolean showVersion;
  private final Path sourceDir;
  private final Path destDir;
  private final String packageBase;
  private final Pattern regexSuffix;

  public Configuration(final String[] commandLineArgs) {
    try {
      final CommandLine checkIfHelpOrVersion = new DefaultParser().parse(getHelpVersionOptions(), commandLineArgs);
      if(checkIfHelpOrVersion.hasOption("h")) {
        showHelp = true;
        showVersion = false;
        sourceDir = null;
        destDir = null;
        packageBase = null;
        regexSuffix = null;
        LOGGER.debug("Configuration loaded - showHelp:    {}", showHelp);
        LOGGER.debug("Configuration loaded - showVersion: {}", showVersion);
      } else if(checkIfHelpOrVersion.hasOption("V")) {
        showHelp = false;
        showVersion = true;
        sourceDir = null;
        destDir = null;
        packageBase = null;
        regexSuffix = null;
        LOGGER.debug("Configuration loaded - showHelp:    {}", showHelp);
        LOGGER.debug("Configuration loaded - showVersion: {}", showVersion);
      } else {
        final CommandLine executionOptions = new DefaultParser().parse(getExecutionOptions(), commandLineArgs);
        showVersion = false;
        showHelp = false;
        sourceDir = Paths.get(executionOptions.getOptionValue("source"));
        destDir = Paths.get(executionOptions.getOptionValue("destination"));
        packageBase = executionOptions.getOptionValue("packagebase");
        regexSuffix = Pattern.compile(executionOptions.getOptionValue("regexsuffix"));
        checkArgument(Files.isDirectory(sourceDir), "source must be a valid directory");
        checkArgument(Files.isDirectory(destDir) || !Files.exists(destDir), "dest must be a valid dir (if it exists)");
        LOGGER.debug("Configuration loaded - source:      {}", sourceDir);
        LOGGER.debug("Configuration loaded - destination: {}", destDir);
        LOGGER.debug("Configuration loaded - packagebase: {}", packageBase);
        LOGGER.debug("Configuration loaded - regexsuffix: {}", regexSuffix);
      }
    } catch (final ParseException e) {
      printHelp();
      throw new WrapgenRuntimeException(e);
    }
  }

  public void printHelp() {
    final HelpFormatter help = new HelpFormatter();
    help.printHelp("console-wrapgen", getExecutionOptions());
  }

  public void printVersion() {
    LOGGER.info("Requested version information: NOT-YET-IMPLEMENTED");
  }

  private Options getHelpVersionOptions() {
    return new Options().addOption(Option.builder("h").longOpt("help").build())
        .addOption(Option.builder("V").longOpt("version").build())
        .addOption(Option.builder("s").longOpt("source").hasArg().build())
        .addOption(Option.builder("d").longOpt("destination").hasArg().build())
        .addOption(Option.builder("p").longOpt("packagebase").hasArg().build())
        .addOption(Option.builder("r").longOpt("regexsuffix").hasArg().build());
  }

  private Options getExecutionOptions() {
    return new Options()
        .addOption(Option.builder("s").longOpt("source").desc("common root directory of stored procs").hasArg()
            .required().build())
        .addOption(Option.builder("d").longOpt("destination").desc("root output directory for runners").hasArg()
            .required().build())
        .addOption(Option.builder("p").longOpt("packagebase").desc("base runner package name of runners").hasArg()
            .required().build())
        .addOption(Option.builder("r").longOpt("regexsuffix").desc("runner package regexp suffix selector").hasArg()
            .required().build());
  }

  public boolean showHelp() {
    return showHelp;
  }

  public boolean showVersion() {
    return showVersion;
  }

  public Path sourceDir() {
    return sourceDir;
  }

  public Path destDir() {
    return destDir;
  }

  public String packageBase() {
    return packageBase;
  }

  public Pattern regexSuffix() {
    return regexSuffix;
  }
}
