# --- Estágio de build ---
# Usa a imagem oficial do Maven em vez do mvnw: o wrapper é um script shell e, clonado
# no Windows, pode chegar aqui com CRLF e falhar dentro do contêiner.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Camada de dependências: só é reconstruída quando o pom muda.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Estágio de runtime ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# A aplicação não roda como root.
RUN addgroup -S corretoria && adduser -S corretoria -G corretoria

COPY --from=build /build/target/*.jar app.jar
RUN chown corretoria:corretoria app.jar

USER corretoria

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
