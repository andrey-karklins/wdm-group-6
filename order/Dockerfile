# Build stage
FROM gradle:7.6.1-alpine AS TEMP_BUILD_IMAGE
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME
COPY --chown=gradle:gradle build.gradle settings.gradle $APP_HOME/
COPY --chown=gradle:gradle src $APP_HOME/src
RUN gradle build --no-daemon -x test

# Package stage
FROM openjdk:19-alpine
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/*.jar app.jar
EXPOSE 5000
CMD ["java", "-jar", "app.jar"]