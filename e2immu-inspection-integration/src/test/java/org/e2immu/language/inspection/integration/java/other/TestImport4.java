package org.e2immu.language.inspection.integration.java.other;

import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;

public class TestImport4 {
    interface BB extends Bool {

    }
    Bool fieldValue;
    Immutable fieldValueImpl;
    Independent independent = INDEPENDENT;
    //FAILS:  IndependentImpl independent2;
    //FAILS:  Value value;
    Independent i2 = from(3);
}
