FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/customer-0.0.1-SNAPSHOT-standalone.jar /customer/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/customer/app.jar"]
