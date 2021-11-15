# s3sync

*THIS PROJECT IS A WORK IN PROGRESS, DONT RUN IT EXCEPT FOR CONTRIBUTING ON TEST FOLDERS BECAUSE DATA LOSS IS VERY LIKELY.*

S3Sync is a server application that can be used to save data in the cloud (S3/DigitalOcean) and synchronize them in real time between machines which run S3Sync with the same configuration.
The application is not centralized. An instance of S3Sync is running on each machine, which acts as a client to the distributed system.
This local server uses AMQP for sending notification to other nodes, MongoDB to save all the informations necessary for proper operation and S3 as cloud storage.

S3Sync works in three different ways:
- A batch process that is automatically launched during the startup phase which aims to synchronize the changes made locally during the downtime of the application.
- A filesystem listener that records changes made to files and folders then notify them to others node in real time.
- A listener on AMQP that receives changes made by other nodes and synchronize them in real time.

### How to run S3Sync 
- Import as Maven project
- Copy the file example.properties to /etc/s3sync.conf (oh, aren't you on linux? sorry...)
- Set the values of all properties (there were free services for all SaaS)
- Run as "Spring boot project"
