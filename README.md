Hello Play Kafka
----------------

## Cloud Setup

1. [Sign up for the Heroku Kafka preview](https://www.heroku.com/kafka)
1. [![Deploy on Heroku](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
1. Install the Kafka plugin into the Heroku CLI

        heroku plugins:install heroku-kafka

1. Wait for Kafka to be provisioned:

        heroku kafka:wait -a YOUR_APP

1. Add a new Kafka topic:

        heroku kafka:create RandomNumbers -a YOUR_APP

1. Watch the Kafka log

        heroku kafka:tail RandomNumbers -a YOUR_APP


## Local Setup

> This uses the same Kafka system as above.

1. Clone the source:

        git clone https://github.com/jamesward/hello-play-kafka

1. Setup a `.env` file with the necessary info:

        heroku config -s > .env
        set -o allexport
        source .env
        set +o allexport

1. Setup the Kafka certs:

        bin/setup_certs.sh

1. Run the web app:

        ./sbt ~run

1. Run the worker:

        ./sbt runMain workers.RandomNumbers

1. Check out the app: http://localhost:9000