# export MAVEN_OPTS="-Xmx2048m"
# export MAVEN_OPTS="-Xms256m -Xmx1024m"
mvn compile exec:java -Dexec.mainClass=com.dc.App
