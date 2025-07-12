package org.e2immu.language.inspection.integration.java.other;

import static org.e2immu.language.cst.api.analysis.Value.*;

public class TestImport4c {

    Bool fieldValue;
    Immutable fieldValueImpl;
    Independent independent ;// FALSE = INDEPENDENT;
    //FAILS:  IndependentImpl independent2;
    //FAILS:  Value value;
    Independent i2 = null;//from(3);
}
