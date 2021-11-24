# repo-tracker

generated using Luminus version "4.24"

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Building and Running

    npm install
    lein uberjar
    export DATABASE_URL=jdbc:sqlite:repo_tracker_dev.db
    java -jar target/uberjar/repo-tracker.jar

API listing at [http://localhost:3000/swagger-ui/index.html](http://localhost:3000/swagger-ui/index.html)
