FROM theasp/clojurescript-nodejs:shadow-cljs-alpine
RUN npm install http-server -g
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN npm install
RUN npm run release
EXPOSE 8677
WORKDIR /usr/src/app/resources/public
CMD ["http-server", "-p", "8677"]
