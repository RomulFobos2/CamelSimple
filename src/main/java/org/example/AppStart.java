package org.example;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

public class AppStart {
    public static void main(String[] args) throws Exception{
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.getPropertiesComponent().setLocation("classpath:application.properties");

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
        Producer producer = applicationContext.getBean("producerBean", Producer.class);
        ManagerDB managerDB = applicationContext.getBean("managerDBBean", ManagerDB.class);

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:{{from}}")
                        .routeId("My route")
                        .choice()
                        .when(exchange -> exchange.getIn().getHeader("CamelFileName").toString().contains(".xml"))
                        .convertBodyTo(String.class)
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                producer.send((String) exchange.getIn().getBody(), false);
                            }
                        })
                        .when(exchange -> exchange.getIn().getHeader("CamelFileName").toString().contains(".txt"))
                        .convertBodyTo(String.class)
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                producer.send((String) exchange.getIn().getBody(), true);
                            }
                        })
                        .otherwise()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                File currentFile = exchange.getIn().getBody(File.class);
                                System.out.println("Ошибка. Отправка файла - " + currentFile.getAbsolutePath() + " ; " + currentFile.getName());
                                producer.sendError(currentFile);
                            }
                        });
            }
        });
        camelContext.start();
    }
}
