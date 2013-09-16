

export name=C8096
export base=/cellon
export ANT_HOME=/usr/local/apache-ant-1.8.2
export JAVA_HOME=/usr/local/jdk1.6.0_25
export PATH=$PATH:$ANT_HOME/bin:$JAVA_HOME/bin
cd $base/server
export classpath=.

for jar in $ANT_HOME/lib/*.jar; do
	if [ -f $jar ]; then
		classpath=$classpath":"$jar
	fi
done

for jar in $base/server/lib/*.jar; do
	if [ -f $jar ]; then
		classpath=$classpath":"$jar
	fi
done

rm -rf $base/server/org/integration/roger/*.class
javac -cp $classpath -encoding GB2312 $base/server/org/integration/roger/*.java
echo -ne "\033]0;$name\007"
java -Xmx1024m -Dcurr=$base -cp $classpath org.integration.roger.Server
