package com.pethotel.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication — составная аннотация, которая включает три механизма:
//
//   1. @SpringBootConfiguration (@Configuration):
//      Помечает класс как источник Bean-определений.
//
//   2. @EnableAutoConfiguration:
//      Spring Boot сканирует classpath и автоматически настраивает компоненты.
//      Например, видит postgresql-driver → настраивает DataSource;
//      видит spring-web → настраивает DispatcherServlet; и т.д.
//      Это избавляет от сотен строк XML-конфигурации (как в Spring без Boot).
//
//   3. @ComponentScan:
//      Сканирует пакет com.pethotel.support и все вложенные пакеты.
//      Находит классы с @Service, @Repository, @Controller, @Component
//      и регистрирует их в Spring Application Context (IoC-контейнер).
//      Важно: работает только внутри пакета, в котором стоит этот класс.
//      Именно поэтому все классы сервиса находятся в com.pethotel.support.*.
@SpringBootApplication
public class SupportApplication {

    public static void main(String[] args) {
        // SpringApplication.run() — точка входа:
        //   1. Создаёт Spring ApplicationContext
        //   2. Запускает auto-configuration
        //   3. Поднимает встроенный Tomcat (порт из application.yml: 8087)
        //   4. Регистрирует все @Bean, @Service, @Repository, @Controller
        //   5. Выполняет CommandLineRunner / ApplicationRunner (если есть)
        //
        // args — аргументы командной строки; Spring Boot умеет читать из них настройки,
        // например: java -jar app.jar --server.port=9090
        SpringApplication.run(SupportApplication.class, args);
    }
}
