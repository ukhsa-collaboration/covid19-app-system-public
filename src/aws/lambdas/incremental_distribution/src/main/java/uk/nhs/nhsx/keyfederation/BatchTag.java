package uk.nhs.nhsx.keyfederation;

import uk.nhs.nhsx.core.ValueType;

public class BatchTag extends ValueType<BatchTag> {
        private BatchTag(String value) {
            super(value);
        }
        public static BatchTag of(String tag) {
            return new BatchTag(tag);
        }
}
