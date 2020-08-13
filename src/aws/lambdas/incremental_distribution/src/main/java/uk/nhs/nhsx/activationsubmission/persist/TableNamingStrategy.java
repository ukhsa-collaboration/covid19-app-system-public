package uk.nhs.nhsx.activationsubmission.persist;

import java.util.function.Function;

public interface TableNamingStrategy {
    String apply(String tableName);

    TableNamingStrategy DIRECT = (t) -> t;
    Function<Environment, TableNamingStrategy> ENVIRONMENTAL = (e) -> (t) -> String.format("%s-%s", e.access.required("WORKSPACE"), t);
    Function<String, TableNamingStrategy> PREFIX = (p) -> (t) -> String.format("%s-%s", p, t);
}
