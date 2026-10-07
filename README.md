# Home Library

To run this application in the docker, run the following commands:

```docker
docker network create my_network
docker run --network my_network --name mysqldb -e MYSQL_ROOT_PASSWORD=root -d mysql
docker run --network my_network -e MYSQL_HOST=mysqldb -p 8080:8080 kacperillo/home-library
```

You can also clone the repository and run app locally, but first you need 
launched MySQL Server on the default port 3306.
If you don't have MySQL Server installed, you can build the docker image and start in container. 
To do that, go to the folder of the cloned repository, open terminal and run:

```docker
docker compose up
```


## Running locally

Backend (from the repository root, needs MySQL on `localhost:3306`):

```bash
mvn spring-boot:run
```

Frontend (in a second terminal):

```bash
cd frontend
npm install
npm run dev
```

Backend runs on `http://localhost:8080`, frontend on `http://localhost:5173`.

Desktop client (Swing, connects straight to the API — no CORS or proxy needed):

```bash
cd desktop
mvn package
java -jar target/home-library-desktop.jar          # or pass the API URL as the first argument
```

See [desktop/README.md](desktop/README.md) for details. A deliberately minimal variant of the same client lives in [desktop-simple/](desktop-simple/) (`cd desktop-simple && mvn package && java -jar target/home-library-desktop-simple.jar`).
