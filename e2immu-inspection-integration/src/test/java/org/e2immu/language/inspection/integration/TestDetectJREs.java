package org.e2immu.language.inspection.integration;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDetectJREs {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDetectJREs.class);

    @Test
    public void test() {
        List<ToolChain.JRE> jres = DetectJREs.runSystemCommand();
        assertTrue(jres.size() > 1);
        boolean have17 = false;
        for (ToolChain.JRE jre : jres) {
            LOGGER.info("JRE = {}", jre);
            if(jre.mainVersion() == 17) have17 = true;
        }
        assertTrue(have17);
    }
}
