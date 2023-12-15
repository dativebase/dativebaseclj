FROM clojure:temurin-11-alpine
COPY . /usr/src/app
WORKDIR /usr/src/app
CMD ["clojure", "-X:run"]