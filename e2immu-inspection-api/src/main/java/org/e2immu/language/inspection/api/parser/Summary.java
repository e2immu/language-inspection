package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Summary {

    boolean haveErrors();

    class ParseException extends RuntimeException {
        private final Object where;

        public ParseException(Object where, String msg) {
            super(msg + " in " + where);
            this.where = where;
        }

        public Object where() {
            return where;
        }
    }

    class FailFastException extends RuntimeException {
        public FailFastException(String msg) {
            super(msg);
        }
    }

    void addMethod(boolean success);

    void addType(TypeInfo typeInfo, boolean success);

    void addParserError(Throwable parserError);

    int methodsSuccess();

    int methodsWithErrors();

    int typesSuccess();

    int typesWithErrors();

    List<Throwable> parserErrors();

    Set<TypeInfo> types();

    TypeInfo firstType();

    TypeInfo getTypeByFqn(String canonicalName);

}
