package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.parser.VariableContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableContextImpl implements VariableContext {
    private final VariableContext parentContext;

    public VariableContextImpl() {
        this(null, new HashMap<>());
    }

    public VariableContextImpl(VariableContext parentContext) {
        this(parentContext, new HashMap<>());
    }

    private VariableContextImpl(VariableContext parentContext, Map<String, FieldReference> staticallyImportedFields) {
        this.fields = new HashMap<>(staticallyImportedFields);
        this.parentContext = parentContext;
    }

    private final Map<String, FieldReference> fields;
    private final Map<String, LocalVariable> localVars = new HashMap<>();
    private final Map<String, ParameterInfo> parameters = new HashMap<>();

    @Override
    public Variable get(String name, boolean complain) {
        Variable variable = localVars.get(name);
        if (variable != null) return variable;
        variable = parameters.get(name);
        if (variable != null) return variable;
        variable = fields.get(name);
        if (variable != null) return variable;
        if (parentContext != null) {
            variable = parentContext.get(name, false);
        }
        if (variable == null && complain) {
            throw new RuntimeException("Unknown variable in context: '" + name + "'");
        }
        return variable;
    }

    /**
     * we'll add them in the correct order, so no overwriting!
     *
     * @param variable the variable to be added
     */
    @Override
    public void add(FieldReference variable) {
        String name = variable.simpleName();
        if (!fields.containsKey(name)) {
            fields.put(name, variable);
        }
    }

    @Override
    public void add(ParameterInfo variable) {
        parameters.put(variable.simpleName(), variable);
    }

    @Override
    public void add(LocalVariable variable) {
        localVars.put(variable.simpleName(), variable);
    }

    @Override
    public VariableContext newEmpty() {
        return new VariableContextImpl();
    }

    public void addAll(List<LocalVariable> localVariables) {
        localVariables.forEach(lvr -> {
            localVars.put(lvr.simpleName(), lvr);
        });
    }

    @Override
    public VariableContext newVariableContext() {
        return new VariableContextImpl(this);
    }

    @Override
    public String toString() {
        return "VariableContext{" +
               parentContext +
               ", local " + localVars.keySet() + ", fields " + fields.keySet() +
               '}';
    }
}
