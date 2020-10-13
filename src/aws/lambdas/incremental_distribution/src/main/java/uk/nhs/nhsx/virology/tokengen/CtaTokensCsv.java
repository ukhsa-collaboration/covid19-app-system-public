package uk.nhs.nhsx.virology.tokengen;

import java.util.Objects;

public class CtaTokensCsv {

    public final String filename;
    public final String content;

    public CtaTokensCsv(String filename, String content) {
        this.filename = filename.endsWith(".csv") ? filename : filename + ".csv";
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CtaTokensCsv that = (CtaTokensCsv) o;
        return Objects.equals(filename, that.filename) &&
            Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, content);
    }
}
