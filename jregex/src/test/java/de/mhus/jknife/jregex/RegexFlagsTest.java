/*

    Copyright (C) 2002 Mike Hummel (mh@mhus.de)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

*/
package de.mhus.jknife.jregex;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegexFlagsTest {

    @Test
    void empty() {
        assertThat(RegexFlags.parse(null)).isEqualTo(0);
        assertThat(RegexFlags.parse("")).isEqualTo(0);
        assertThat(RegexFlags.parse("  ")).isEqualTo(0);
        assertThat(RegexFlags.parse(", ,")).isEqualTo(0);
    }

    @Test
    void singleAndCombined() {
        assertThat(RegexFlags.parse("CASE_INSENSITIVE")).isEqualTo(Pattern.CASE_INSENSITIVE);
        assertThat(RegexFlags.parse("case_insensitive")).isEqualTo(Pattern.CASE_INSENSITIVE);
        assertThat(RegexFlags.parse("MULTILINE,DOTALL")).isEqualTo(Pattern.MULTILINE | Pattern.DOTALL);
        assertThat(RegexFlags.parse(" MULTILINE , DOTALL ")).isEqualTo(Pattern.MULTILINE | Pattern.DOTALL);
    }

    @Test
    void unknownFlag() {
        assertThatThrownBy(() -> RegexFlags.parse("NOPE")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NOPE");
    }
}
