package com.pethotel.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication — составная аннотация, включающая три механизма:
//
//   1. @SpringBootConfiguration (@Configuration):
//      Помечает класс как источник Bean-определений.
//
//   2. @EnableAutoConfiguration:
//      Spring Boot сканирует classpath и автоматически настраивает компоненты.
//      Видит postgresql-driver → настраивает DataSource; видит spring-web → настраивает DispatcherServlet.
//
//   3. @ComponentScan:
//      Сканирует пакет com.pethotel.customer и все вложенные пакеты.
//      Находит @Service, @Repository, @Controller, @Component и регистрирует их в IoC-контейнере.
//
// Именно поэтому все классы сервиса находятся внутри пакета com.pethotel.customer.*.
@SpringBootApplication
public class CustomerServiceApplication {
    public static void main(String[] args) {
        // SpringApplication.run() запускает Spring контекст, поднимает встроенный Tomcat
        // на порту из application.yml (8081) и регистрирует все бины.
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
