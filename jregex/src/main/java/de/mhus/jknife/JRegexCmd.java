package de.mhus.jknife;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "jregex", mixinStandardHelpOptions = true, version = "0.0.1", description = "Uses jdk regex", subcommands = {
        CommandLine.HelpCommand.class, MatchesSubCmd.class, ReplaceSubCmd.class })
public class JRegexCmd implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new JRegexCmd()).execute(args);
        System.exit(exitCode);
    }

}
