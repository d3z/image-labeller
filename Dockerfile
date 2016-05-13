FROM instil/java-base

ADD build/libs/image-labeller-1.0.0-capsule.jar /app/image-labeller.jar
ADD src/main/resources/credentials.json /app/credentials.json
ADD src/main/resources/config.yml /app/config.yml

ENV GOOGLE_APPLICATION_CREDENTIALS "/app/credentials.json"
ENV GOOGLE_APPLICATION_NAME "PeeWee"

EXPOSE 8080

CMD [ "java", "-jar", "/app/image-labeller.jar", "server", "/app/config.yml"]
