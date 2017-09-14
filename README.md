Hello Play Kafka
----------------

## Cloud Setup

1. [![Deploy on Heroku](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
1. Install the Kafka plugin into the Heroku CLI

        heroku plugins:install heroku-kafka

1. Wait for Kafka to be provisioned:

        heroku kafka:wait -a YOUR_APP

1. Add a new Kafka topic:

        heroku kafka:topics:create RandomNumbers --partitions 32 -a YOUR_APP

1. Add a new Kafka consumer group:

        heroku kafka:consumer-groups:create main

1. Watch the Kafka log

        heroku kafka:topics:tail RandomNumbers -a YOUR_APP

1. Check out the random numbers:

        heroku open -a YOUR_APP

## Local Setup

> This uses the same Kafka system as above.

1. Clone the source:

        git clone https://github.com/jamesward/hello-play-kafka

1. Associate the local source with your Heroku app:

        heroku git:remote -a YOUR_APP

1. Setup a `.env` file with the necessary info:

        heroku config -s > .env

1. Run the web app:

        set -o allexport
        source .env
        set +o allexport
        ./sbt ~run

1. Run the worker:

         set -o allexport
         source .env
         set +o allexport
        ./sbt "runMain workers.RandomNumbers"

1. Check out the app: http://localhost:9000
