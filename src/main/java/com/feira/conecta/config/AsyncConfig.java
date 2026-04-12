package com.feira.conecta.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Habilita o suporte a métodos assíncronos (@Async) na aplicação e configura
 * o pool de threads dedicado ao processamento de matching.
 *
 * <p>Ao separar o executor de matching do executor padrão do Spring, evitamos
 * que picos de cadastro de anúncios congestionem outras tarefas assíncronas
 * que possam existir na aplicação no futuro.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor dedicado ao matching de anúncios × demandas.
     *
     * <p>Configuração conservadora para início:
     * <ul>
     *   <li>corePoolSize=2 — duas threads sempre disponíveis</li>
     *   <li>maxPoolSize=5 — até cinco em pico de carga</li>
     *   <li>queueCapacity=50 — fila de até 50 tarefas antes de expandir o pool</li>
     * </ul>
     * Ajuste conforme o volume de cadastros da plataforma crescer.</p>
     */
    @Bean(name = "matchingExecutor")
    public Executor matchingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("matching-");
        executor.initialize();
        return executor;
    }
}