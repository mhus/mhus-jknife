package de.mhus.jknife;

import de.mhus.commons.M;
import de.mhus.commons.tools.MFile;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.Console;
import java.io.File;
import java.util.concurrent.Callable;

@Log
@CommandLine.Command(name = "matches", description = "Regex Matches", subcommands = CommandLine.HelpCommand.class)
public class MatchesSubCmd implements Callable<Integer> {

    @Parameters(index = "0")
    private String text;

    @Parameters(index = "1")
    private String pattern;

    @CommandLine.Option(names = { "-v", "--verbose" })
    private boolean verbose = false;

    @CommandLine.Option(names = { "-o", "--output" })
    private boolean output = false;

    @CommandLine.Option(names = { "-t", "--text" }, description = "Text file or '-' for stdin")
    private String textFile;

    @CommandLine.Option(names = { "-c", "--charset" }, description = "Text file charset")
    private String charSet = M.CHARSET_UTF_8;

    @Override
    public Integer call() throws Exception {
        if (textFile != null) {
            if (textFile.equals("-")) {
                var is = System.in;
                text = MFile.readFile(is, charSet);
            } else {
                var file = new File(textFile);
                if (verbose)
                    LOGGER.info("Load from file: " + file.getAbsolutePath());
                text = MFile.readFile(file, charSet);
            }
        }
        if (verbose) {
            LOGGER.info("String: " + text);
            LOGGER.info("Pattern: " + pattern);
        }
        var result = text.matches(pattern);
        if (verbose) {
            LOGGER.info("Result: " + result);
        }
        if (output)
            System.out.println(String.valueOf(result));
        return result ? 0 : 1;
    }
}
