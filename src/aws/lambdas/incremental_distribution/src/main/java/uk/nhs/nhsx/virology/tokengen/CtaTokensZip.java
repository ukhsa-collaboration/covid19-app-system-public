package uk.nhs.nhsx.virology.tokengen;

import java.io.File;
import java.util.Objects;

public class CtaTokensZip {

    public final String filename;
    public final File content;

    public CtaTokensZip(String filename, File content) {
        this.filename = filename.endsWith(".zip") ? filename : filename + ".zip";
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CtaTokensZip that = (CtaTokensZip) o;
        return Objects.equals(filename, that.filename) &&
            Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, content);
    }
}
