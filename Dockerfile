FROM openjdk:14-alpine
COPY build/libs/mn-oci-final-*-all.jar mn-oci-final.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "mn-oci-final.jar"]