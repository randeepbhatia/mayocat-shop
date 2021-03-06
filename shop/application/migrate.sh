
if [ ! -d "target" ]; then
  echo "You must build first: 'mvn install'"
  exit
fi

JAR_NAME=`ls target | grep "^mayocat-shop" | grep -v postgre`

if [ -z $JAR_NAME ]; then
  echo "You must build first: 'mvn install'"
  exit
fi

DEBUG=""
DEBUG_PORT=5005
# DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=$DEBUG_PORT"

java $DEBUG -jar target/$JAR_NAME flyway-migrate mayocat.yml
