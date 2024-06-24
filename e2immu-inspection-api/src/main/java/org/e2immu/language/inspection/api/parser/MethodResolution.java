package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Set;

public interface MethodResolution {

    GenericsHelper genericsHelper();

    HierarchyHelper hierarchyHelper();

    /*
    used for method call erasure,
     */
    Set<ParameterizedType> computeScope(Context context, String index,
                                        String methodName, Object unparsedScope, List<Object> unparsedArguments);

    Expression resolveMethod(Context context, List<Comment> comments, Source source, String index,
                             ForwardType forwardType,
                             String methodName, Object unparsedObject, List<Object> unparsedArguments);
}
