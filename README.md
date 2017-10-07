# pestering-petabytes
Fundamental stock analysis using the http://commoncrawl.org/ petabyte-size database and the [Stanford NLP parser](https://nlp.stanford.edu/). The workload was crunched through with instances on Google Cloud Platform.

## Installation 
In src/main/resources/ create a file "api_key" containing only the api key for Alchemy API.

Install the gcloud command line tool and authenticate using `gcloud beta auth application-default login`. Use team email for access. For now only it can access the table we use in Google Datastore.

## Build

To build run `mvn clean install` in the top-level directory of the project.

## Run

To run use the `run-jar.sh` script for UNIX. For Windows adapt the script as needed. It's a simple jar execution.
The run log will be saved in `run-log.txt`
