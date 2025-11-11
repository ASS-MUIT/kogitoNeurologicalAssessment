package us.dit.muit.hsa.neurologicalassessment.config;

import org.kie.kogito.process.Process;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone un alias de bean "assessment" para el proceso generado
 * cuyo bean real se registra como "neurologicalassessment.assessment".
 * Evitamos referenciar tipos generados en compile-time usando comodines.
 */
@Configuration
public class ProcessAliasesConfig {

    @Bean(name = "assessment")
    public Process<?> assessmentAlias(
            @Qualifier("neurologicalassessment.assessment") Process<?> process) {
        return process;
    }
}
