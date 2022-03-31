FROM openjdk:14-jdk-alpine3.10
ENV PLANTUML_VERSION=1.2022.2
ENV LANG en_US.UTF-8
RUN \
  apk add --no-cache graphviz wget ca-certificates && \
  apk add --no-cache graphviz wget ca-certificates ttf-dejavu fontconfig && \
  wget "https://github.com/plantuml/plantuml/releases/download/v1.2022.2/plantuml-${PLANTUML_VERSION}.jar" -O plantuml.jar && \
  apk del wget ca-certificates
RUN ["java", "-Djava.awt.headless=true", "-jar", "plantuml.jar", "-version"]
RUN ["dot", "-version"]
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "plantuml.jar", "-p"]
CMD ["-tsvg"]
