package uk.co.bristlecone.vpt.runner.console;

import uk.co.bristlecone.vpt.runner.SourceFileProcessor;
import uk.co.bristlecone.vpt.runner.SourceFileResultProcessor;
import uk.co.bristlecone.vpt.runner.console.impl.DirSourceFileProvider;
import uk.co.bristlecone.vpt.runner.impl.SourceFilePartitioner;

public class ConsoleWrapgenController {
  public ConsoleWrapgenController() {
    // intentionally empty
  }

  public void run(final DirSourceFileProvider provider, final SourceFileProcessor sourceProcessor,
      final SourceFileResultProcessor resultProcessor) {
    new SourceFilePartitioner(provider).freshStreamOfAllSourceFiles().map(sourceProcessor::process)
        .forEach(resultProcessor::process);
  }
}
