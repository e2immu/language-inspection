package org.e2immu.analyzer.shallow.aapi;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotatedApiParser implements AnnotationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotatedApiParser.class);

    private record Data(List<AnnotationExpression> annotations) {
    }

    private final Map<Info, Data> infoMap = new HashMap<>();
    private final JavaInspector javaInspector;

    public AnnotatedApiParser() {
        javaInspector = new JavaInspectorImpl();
    }

    public void initialize(List<String> sourceDirs, List<String> packageList) throws IOException {
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_CLASSPATH);
        sourceDirs.forEach(builder::addSources);
        packageList.forEach(builder::addRestrictSourceToPackages);
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.sourceTypes().visit(new String[]{}, (parts, list) -> {
            list.forEach(this::load);
        });
    }

    private void load(TypeInfo typeInfo) {
        javaInspector.parse(typeInfo);
    }

    // for testing
    public SourceTypes sourceTypes() {
        return javaInspector.sourceTypes();
    }

    @Override
    public List<AnnotationExpression> annotations(Info info) {
        return List.of();
    }
}
