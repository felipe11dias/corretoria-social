package com.corretoria.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Execução assíncrona de trabalho disparado por evento de domínio.
 *
 * <p>O casamento de um anúncio com as buscas salvas varre a base e envia e-mail; fazê-lo na
 * thread da requisição faria o corretor esperar por trabalho que não é dele.
 *
 * <p>O executor é declarado explicitamente em vez de aceitar o padrão do Spring Boot porque o
 * padrão é ilimitado na fila: uma rajada de publicações acumularia tarefas até o processo
 * ficar sem memória. Aqui a fila é finita e, quando enche, a política é executar a tarefa na
 * própria thread que a submeteu — o produtor desacelera em vez de o sistema cair.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 500;
    private static final String THREAD_NAME_PREFIX = "corretoria-async-";

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
