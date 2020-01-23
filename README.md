
### Run docker containers

    docker run -d --name ftp1 \
        -p 3333:21 -p 30000-30009:30000-30009 \
        -e "PUBLICHOST=localhost" \
        -e FTP_USER_NAME=testuser \
        -e FTP_USER_PASS=password \
        -e FTP_USER_HOME=/home/testuser \
        stilliard/pure-ftpd

    docker run -d --name ftp2 \
        -e FTP_PASSIVE_PORTS=10000:10009 --expose=10000-10009 \
        -p 3334:21 -p 10000-10009:10000-10009 \
        -e "PUBLICHOST=localhost" \
        -e FTP_USER_NAME=testuser \
        -e FTP_USER_PASS=password \
        -e FTP_USER_HOME=/home/testuser \
        stilliard/pure-ftpd

    docker run -d --name ftp3 \
        -e FTP_PASSIVE_PORTS=20000:20009 --expose=20000-20009 \
        -p 3335:21 -p 20000-20009:20000-20009 \
        -e "PUBLICHOST=localhost" \
        -e FTP_USER_NAME=testuser \
        -e FTP_USER_PASS=password \
        -e FTP_USER_HOME=/home/testuser \
        stilliard/pure-ftpd

### Connect to container

    docker exec -it ftp /bin/bash