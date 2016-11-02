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

1. Watch the Kafka log

        heroku kafka:topics:tail RandomNumbers -a YOUR_APP

1. Check out the random numbers:

        heroku open -a YOUR_APP

## Local Setup

1. Clone the source:

        git clone https://github.com/jamesward/hello-play-kafka

1. Start Kafka:

        ./sbt startKafka

1. Run the web app:

        ./sbt ~run

1. Run the worker:

        ./sbt "runMain workers.RandomNumbers"

1. Check out the app: http://localhost:9000
