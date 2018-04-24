# Hyperledger SDK For Android
 **Note** - The jar file for fabric is fabric-sdk-java/target/fabric-sdk-java01.1.0-alpha.jar. This file includes all the dependendencies. This jar will work on only on API >26 since android support Java 8 from API 26 and above. To make compatible with lower version you might have to make following changes:

**1.** Java.util.Base64 class should be replaced with android Base64 package. <br />
**2.** Java.time.Instant class should be replaced with java.sql.Timestamp. +There might be other changes required as well.<br/>
 ________________________________________________________

 ### Sofwares

- **Latest Android Studio:**
- **SDK:** (with installed API 26 and above )
- **Maven** (if you want to make changes in fabric-sdk-java. To build jar file check [fabric-sdk-java] (https://github.com/hyperledger/fabric-sdk-java/tree/master/docs) official github repo)
- **Emulator or Android Device** (To run the appllication)
________________________________________________________

### Changes Made for Fabric-java-SDK
**1.** Shaded maven dependencies (org.apache.commons.codec, org.apache.http, javax.naming) and also included some dependencies in the jar. (check pom.xml for more details)<br />
**2.** Replaced grpc-netty package with grpc-okhttp package as grpc-netty is not suporrted by android. <br />
**2.** Replaced NettyChannelBuilder with OkHttpChannelBuilder class and added javax.net.ssl.SSLSocketFactory to OkHttpChannelBuilder for enabling tls communication. (Check Endpoint.java for more details) <br />
________________________________________________________

### Usage

**1.** Clone the project on your local machine.<br />
**2.** Import HyperledgerSDKTest project in you android studio <br />
**3.** Make sure Hyperledger network is up and running with orderer and peers + CA <br />
**4.** Edit config.properties to update the hyperldegr configuration settings <br />
**5.** Replace corret atrifacts in asset folder in HyperledgerSDKTest project directory  <br />
**5.** Run MainActivity.class as Android Application  <br />
**Note** MainActivity.class has many function like channelCreate, joinChannel , installChaincode , instantiateChaincode, invokeChaincode, queryChaincode. kindly uncomment the required one and run the application. The output is displayed in the console.<br />
