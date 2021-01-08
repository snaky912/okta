#!/bin/bash
# Installing Java, OPP agent and create a self-signed cert
# Authored by Karmen Lei
set -u

usage()
{
     echo "usage: java11.sh -u <java version, e.g. 11>"
}

while getopts u: OPCAO; do
     case "${OPCAO}" in
        u) u="${OPTARG}";;
     esac
done

if [ -z ${u+x} ];then
   echo "ERROR: Java version not provided. Aborting."
   exit 1
fi

JAVA_VERSION=${u} # 11

echo "Installing: OpenJDK$JAVA_VERSION..."
sudo yum -y install java-$JAVA_VERSION-openjdk-devel
if [ $? -ne 0 ]; then
  echo "Error installing Java."
  exit 1
fi

echo "Done. Setting variables..."

echo "Installing unzip..."
sudo yum -y install unzip

echo "Installing wget..."
sudo yum -y install wget

echo "Downloading Apache Tomcat..."
sudo wget https://www-eu.apache.org/dist/tomcat/tomcat-9/v9.0.41/bin/apache-tomcat-9.0.41.tar.gz

if [ $? -ne 0 ]; then
  echo "Error downloading Tomcat, aborting..."
  exit 1
fi

tar -xf apache-tomcat-9.0.41.tar.gz
sudo sh -c 'chmod +x apache-tomcat-9.0.41/bin/*.sh'

PUBLIC_IP=$(curl http://169.254.169.254/2009-04-04/meta-data/public-ipv4)

export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which javac)))))
export PATH=$PATH:$JAVA_HOME/bin
export CLASSPATH=.:$JAVA_HOME/jre/lib:$JAVA_HOME/lib:$JAVA_HOME/lib/tools.jar

echo "Creating a self signed cert and enable HTTPS in Tomcat..."

local_openssl_config="
[ req ]
prompt = no
distinguished_name = req_distinguished_name
x509_extensions = san_self_signed
[ req_distinguished_name ]
CN=$PUBLIC_IP
[ san_self_signed ]
subjectAltName = IP.1:$PUBLIC_IP
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = CA:true
keyUsage = nonRepudiation, digitalSignature, keyEncipherment, dataEncipherment, keyCertSign, cRLSign
extendedKeyUsage = serverAuth, clientAuth, timeStamping
"

keytool -genkey -alias scim_tom -dname "cn=${PUBLIC_IP}, ou=IT, o=AnyCompany, l=sf, s=ca, c=us" -ext san=ip:$PUBLIC_IP -keyalg RSA -keystore scim_tomcat_keystore -storepass Passw0rd!! -keypass Passw0rd!! -noprompt

keytool -export -keystore scim_tomcat_keystore -alias scim_tom -file scim_tomcat.cert -storepass Passw0rd!! -noprompt

#openssl req -newkey rsa:2048 -nodes -keyout "scim_tom.key.pem" -x509 -sha256 -days 3650 -config <(echo "$local_openssl_config") -out "scim_tom.cert"

#openssl x509 -noout -text -in "scim_tom.cert"

#openssl pkcs12 -export -name scim_tom -in scim_tom.cert -inkey scim_tom.key.pem -out scim_tom_keystore.p12 -passin pass:Passw0rd!! -passout pass:Passw0rd!!

#keytool -importkeystore -destkeystore scim_tomcat_keystore -srckeystore scim_tom_keystore.p12 -srcstoretype pkcs12 -alias scim_tom -srcstorepass Passw0rd!! -deststorepass Passw0rd!! -noprompt

echo "Done creating the self-signed cert and keystore, adding HTTPS support in Apache Tomcat..."

CONTENT="<Connector port=\"8443\" protocol=\"HTTP/1.1\" SSLEnabled=\"true\"\nmaxThreads=\"150\" scheme=\"https\" secure=\"true\"\nclientAuth=\"false\" sslProtocol=\"TLS\"\nkeystoreFile=\"/home/ec2-user/scim_tomcat_keystore\"\n keystorePass=\"Passw0rd!!\" />"

C=$(echo $CONTENT | sed 's/\//\\\//g')
sudo sed -i "/<\/Service>/ s/.*/${C}\n&/" apache-tomcat-9.0.41/conf/server.xml

echo "Done adding HTTPS support in Tomcat, deploying the generic MySQL scim connector..."

cp scim-server-mysql-1.0.0-SNAPSHOT.war apache-tomcat-9.0.41/webapps

echo "Done deploying the scim connector, starting Tomcat..."

sudo apache-tomcat-9.0.41/bin/startup.sh

echo "Done starting Tomcat, adding cert into OPP agent and starting the agent..."

sudo /opt/OktaProvisioningAgent/jre/bin/keytool -import -file scim_tomcat.cert -alias scim_tom -keystore /opt/OktaProvisioningAgent/jre/lib/security/cacerts -storepass changeit -noprompt

sudo sed -i "s/-Xmx4096m/-Xmx4096m -Dhttps.protocols=TLSv1.2/" /opt/OktaProvisioningAgent/conf/settings.conf

sudo service OktaProvisioningAgent start
