package org.e2immu.language.inspection.integration.java.other;

import static org.e2immu.language.cst.impl.analysis.ValueImpl.*;

public class TestImport4b {

    Bool fieldValue;
    Immutable fieldValueImpl;
    Independent independent ;// FALSE = INDEPENDENT;
    //FAILS:  IndependentImpl independent2;
    //FAILS:  Value value;
    Independent i2 = null;//from(3);
}
