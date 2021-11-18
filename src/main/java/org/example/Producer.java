package org.example;

import bitronix.tm.BitronixTransactionManager;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.transaction.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.sql.Statement;

public class Producer {
    private ActiveMQConnectionFactory connectionFactory;
    BitronixTransactionManager transactionManager;
    private Session sessionProducer;
    private Connection connectionProducer;
    private Destination destination;
    private String queue;
    private MessageProducer messageProducer;
    private int persistent; //1 or 2
    private MailSender mailSender;
    private final static int MAX_COUNT = 10;
    private long startTime;

    private int allFileCount;
    private int allFileXML;
    private int allFileTXT;
    private int allFileUNK;

    public Producer(ActiveMQConnectionFactory connectionFactory, BitronixTransactionManager transactionManager, int sessionCode, String queue, int persistent, MailSender mailSender) throws JMSException {
        this.connectionFactory = connectionFactory;
        this.transactionManager = transactionManager;
        this.connectionProducer = this.connectionFactory.createConnection();
        connectionProducer.start();
        if (sessionCode > 0) {
            this.sessionProducer = this.connectionProducer.createSession(false, sessionCode);
        } else {
            this.sessionProducer = this.connectionProducer.createSession(true, sessionCode);
        }
        this.queue = queue;
        this.persistent = persistent;
        this.mailSender = mailSender;
        this.startTime = System.currentTimeMillis();
    }

    public ActiveMQConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ActiveMQConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Session getSessionProducer() {
        return sessionProducer;
    }

    public void setSessionProducer(Session sessionProducer) {
        this.sessionProducer = sessionProducer;
    }

    public Connection getConnectionProducer() {
        return connectionProducer;
    }

    public void setConnectionProducer(Connection connectionProducer) {
        this.connectionProducer = connectionProducer;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public MessageProducer getMessageProducer() {
        return messageProducer;
    }

    public void setMessageProducer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public void send(String text, boolean writeToDB) throws JMSException {
        allFileCount++;
        this.destination = this.sessionProducer.createQueue(queue);
        try {
            messageProducer = sessionProducer.createProducer(destination);
            messageProducer.setDeliveryMode(persistent);
            TextMessage message = sessionProducer.createTextMessage(text);
            messageProducer.send(message);
            if (writeToDB) {
                allFileTXT++;
                try {
                    transactionManager.begin();
                    java.sql.Connection connectionToDB = ManagerDB.getCurrentConnection();
                    Statement statement = connectionToDB.createStatement();
                    String sqlCommand;
                    if (!ManagerDB.tableExist("messages_camel")) {
                        sqlCommand = "Create TABLE messages_camel(message_id INT PRIMARY KEY AUTO_INCREMENT, body_message VARCHAR(100));";
                        statement.executeUpdate(sqlCommand);
                    }
                    sqlCommand = "INSERT messages_camel (body_message) VALUE('" + text + "')";
                    statement.executeUpdate(sqlCommand);
                    statement.close();
                    transactionManager.commit();
                }
                catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException exception){
                    try {
                        transactionManager.rollback();
                    } catch (SystemException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                allFileXML++;
            }
        } catch (JMSException | SQLException exception) {
            exception.printStackTrace();
        }
        checkCount();
    }

    public void sendError(File file) throws JMSException {
        allFileCount++;
        allFileUNK++;
        this.destination = this.sessionProducer.createQueue("invalid_queue");
        try {
            messageProducer = sessionProducer.createProducer(destination);
            messageProducer.setDeliveryMode(persistent);
            BytesMessage bytesMessage = sessionProducer.createBytesMessage();
            bytesMessage.setStringProperty("fileName", file.getName());
            RandomAccessFile accessFile = new RandomAccessFile(file, "r");
            byte[] bytes = new byte[(int) accessFile.length()];
            accessFile.readFully(bytes);
            accessFile.close();
            bytesMessage.writeBytes(bytes);
            messageProducer.send(bytesMessage);
        } catch (JMSException | FileNotFoundException exception) {
            exception.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkCount();
    }

    public void checkCount(){
        if(allFileCount >= MAX_COUNT){
            long work_time = (startTime - System.currentTimeMillis())/1000;
            String textLetter = "Обработано " + MAX_COUNT + " сообщений."
                    + "\n Из них " +
                    + allFileXML + " формата XML, " +
                    + allFileTXT + " формата TXT, " +
                    + allFileUNK + " неизвестного формата"
                    + "\n Время обработки пачки сообщений - " + work_time + " с.";
            mailSender.send("romulfobos@gmail.com", "info", textLetter);
            startTime = System.currentTimeMillis();
            allFileCount = 0;
            allFileXML = 0;
            allFileTXT = 0;
            allFileUNK = 0;
        }
    }
}