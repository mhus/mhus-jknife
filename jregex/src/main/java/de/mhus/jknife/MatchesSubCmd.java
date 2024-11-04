package de.mhus.jknife;

import de.mhus.commons.M;
import de.mhus.commons.tools.MFile;
import lombok.extern.java.Log;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
@CommandLine.Command(name = "matches", description = "Regex Matches", subcommands = CommandLine.HelpCommand.class)
public class MatchesSubCmd implements Callable<Integer> {

    @Parameters(index = "0")
    private String text;

    @Parameters(index = "1")
    private String regex;

    @CommandLine.Option(names = { "-v", "--verbose" })
    private boolean verbose = false;

    @CommandLine.Option(names = { "-o", "--output" })
    private boolean output = false;

    @CommandLine.Option(names = { "-t", "--text" }, description = "Text file or '-' for stdin")
    private String textFile;

    @CommandLine.Option(names = { "-c", "--charset" }, description = "Text file charset")
    private String charSet = M.CHARSET_UTF_8;

    @CommandLine.Option(names = { "-f",
            "--flags" }, description = "Flags comma separated: CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS and COMMENTS")
    private String flagsStr = "";

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
        int flags = RegexUtil.getFlagsInteger(flagsStr);
        if (verbose) {
            LOGGER.info("String: " + text);
            LOGGER.info("Pattern: " + regex);
            LOGGER.info("Flags: " + flags + " (" + flagsStr + ")");
        }
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(text);
        var result = matcher.matches();
        if (verbose) {
            LOGGER.info("Result: " + result);
        }
        if (output)
            System.out.println(String.valueOf(result));
        return result ? 0 : 1;
    }
}
