package org.e2immu.language.inspection.resource;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestResourceImpl {
    @Test
    public void testReplaceSlashDollar() {
        assertEquals("org.e2immu.sequence", ResourcesImpl.replaceSlashDollar("org/e2immu/sequence"));
        assertEquals("org.e2immu.sequence", ResourcesImpl.replaceSlashDollar("org/e2immu$sequence"));
        assertEquals("org.e2immu.$sequence", ResourcesImpl.replaceSlashDollar("org/e2immu$$sequence"));
    }
}
