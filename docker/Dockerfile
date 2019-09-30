FROM openjdk:11

COPY --from=builder /builder /app

EXPOSE 80

CMD ["ls", "/app"]
CMD ["java", "-jar", "/app/target/app-jar-with-dependencies.jar"]