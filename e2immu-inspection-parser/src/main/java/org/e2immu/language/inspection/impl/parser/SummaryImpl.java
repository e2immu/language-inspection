package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SummaryImpl implements Summary {
    private final static Logger LOGGER = LoggerFactory.getLogger(SummaryImpl.class);

    private final LinkedHashMap<TypeInfo, Boolean> types = new LinkedHashMap<>();
    private final boolean failFast;
    private final List<Throwable> parserErrors = new LinkedList<>();

    private int methodsSuccess;
    private int methodsWithErrors;

    public SummaryImpl(boolean failFast) {
        this.failFast = failFast;
    }

    @Override
    public Set<TypeInfo> types() {
        return types.keySet();
    }

    @Override
    public ParseResult parseResult() {
        if (haveErrors()) {
            throw new UnsupportedOperationException("Can only switch to ParseResult when there are no parse errors");
        }
        return new ParseResultImpl(types.keySet());
    }

    @Override
    public void addMethod(boolean success) {
        if (success) ++methodsSuccess;
        else ++methodsWithErrors;
    }

    @Override
    public void addType(TypeInfo typeInfo, boolean success) {
        types.merge(typeInfo, success, (b1, b2) -> b1 && b2);
    }

    @Override
    public void addParserError(Throwable parserError) {
        LOGGER.error("Register parser error", parserError);
        this.parserErrors.add(parserError);
        if (failFast) {
            throw new Summary.FailFastException("Failing with parser error: " + parserError.getMessage());
        }
    }

    @Override
    public int methodsSuccess() {
        return methodsSuccess;
    }

    @Override
    public int methodsWithErrors() {
        return methodsWithErrors;
    }

    @Override
    public int typesSuccess() {
        return (int) types.values().stream().filter(b -> b).count();
    }

    @Override
    public int typesWithErrors() {
        return (int) types.values().stream().filter(b -> !b).count();
    }

    @Override
    public List<Throwable> parserErrors() {
        return parserErrors;
    }

    @Override
    public boolean haveErrors() {
        if (typesWithErrors() == 0) {
            assert methodsWithErrors == 0;
            assert parserErrors.isEmpty();
            return false;
        }
        assert !parserErrors.isEmpty();
        return true;
    }
}
