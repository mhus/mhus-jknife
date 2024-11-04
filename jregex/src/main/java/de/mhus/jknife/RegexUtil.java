package de.mhus.jknife;

import de.mhus.commons.tools.MCollection;
import lombok.Getter;

import java.util.regex.Pattern;

public class RegexUtil {

    enum FLAG {
        CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE), MULTILINE(Pattern.MULTILINE), DOTALL(Pattern.DOTALL),
        UNICODE_CASE(Pattern.UNICODE_CASE), CANON_EQ(Pattern.CANON_EQ), UNIX_LINES(Pattern.UNIX_LINES),
        LITERAL(Pattern.LITERAL), UNICODE_CHARACTER_CLASS(Pattern.UNICODE_CHARACTER_CLASS), COMMENTS(Pattern.COMMENTS);

        @Getter
        private int flag;

        FLAG(int flag) {
            this.flag = flag;
        }
    }

    public static int getFlagsInteger(String str) {
        var parts = str.toUpperCase().split(",");
        int result = 0;
        for (FLAG flag : FLAG.values()) {
            if (MCollection.contains(parts, flag.name()))
                result = result + flag.getFlag();
        }
        return result;
    }

}
