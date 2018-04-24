package com.example.shubhamvrkr.hyperledgersdktest;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import utils.SampleOrg;
import utils.SampleStore;
import utils.SampleUser;
import utils.TestConfig;

import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

public class MainActivity extends AppCompatActivity {

    private static CryptoSuite crypto;
    private SampleStore sampleStore;
    private HFCAClient client;
    private SampleUser admin;
    private static final String TEST_WITH_INTEGRATION_ORG = "peerOrg1";
    private static final String TEST_ADMIN_NAME = "admin";
    private static final String TEST_ADMIN_PW = "adminpw";
    private static final String TEST_ADMIN_ORG = "org1";
    SampleOrg org1;

    String password = "svpassword";
    private static TestConfig testConfig;

    String CHAIN_CODE_NAME = "chaincode";
    String CHAIN_CODE_PATH = "github.com/example_cc";
    String CHAIN_CODE_VERSION="1";

    Map<String, Properties> clientTLSProperties = new HashMap<>();

    File directory;
    File sampleStoreFile,fileStore ,adminOrg1PrivateKey,adminOrg1PublickKey,ordererCert,channelConfig,chaincode,peerRootCa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        upgradeSecurityProvider();
        setContentView(R.layout.activity_main);
        System.out.println("On create");
        try{

            testConfig = TestConfig.getConfig(getApplicationContext());
            crypto = CryptoSuite.Factory.getCryptoSuite();

            //store necessary files from asset folder to the internal storage of device
            //check the asset folder to view which files are copied
            createFiles();

            sampleStore = new SampleStore(sampleStoreFile);
            org1 = testConfig.getIntegrationTestsSampleOrg(TEST_WITH_INTEGRATION_ORG);

            System.out.println("Name: "+org1.getName());
            System.out.println("MSPID"+org1.getMSPID());

            //client = HFCAClient.createNewInstance(org1.getCALocation(), org1.getCAProperties());
            // client.setCryptoSuite(crypto);

            //admin = sampleStore.getMember(TEST_ADMIN_NAME, TEST_ADMIN_ORG);
            //new Task().execute();

            SampleUser peerOrgAdmin = sampleStore.getMember(org1.getName() + "Admin", org1.getName(), org1.getMSPID(),adminOrg1PrivateKey,adminOrg1PublickKey);
            org1.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode

            // change the default params in helper/Config.java.
            // need to increase the time duration for avoiding grpc timeout exception
            Config.setProperty(Config.ORDERER_WAIT_TIME,"1000000");
            Config.setProperty(Config.GENESISBLOCK_WAIT_TIME,"5000");


             //Note: running below function might giva networkonmainthread exception. however running it again will work.
             // Ideal way to run this function will be in Async task.

             // function to create a channel
             // createChannel(sampleStore);

             // function to join a channel. currently only one org admin intiates the request to join only one org peers.
             // joinchannel();

             // install chaincode on the org1 peers
             // installChaincode(sampleStore);

             // deploy a chaincode. currently balance transfer chaincode is deployed.
             // instantiateChaincode(sampleStore);

             // invoke a chaincode function to transfer 100 from a to b
             // invokeTransaction(sampleStore);

             // query chaincode to get value of b and print the result
             // queryTransaction(sampleStore);



        }catch(Exception e){

            e.printStackTrace();
        }
    }

    private void upgradeSecurityProvider() {

        ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {

            }

            @Override
            public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
                GooglePlayServicesUtil.showErrorNotification(errorCode, MainActivity.this);
            }
        });
    }

    public void createFiles(){

        try{
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            directory = cw.getDir("newartifacts", Context.MODE_PRIVATE);
            sampleStoreFile =new File(directory.getAbsolutePath() + File.separator +  "HFCSampletest.properties");
            if(sampleStoreFile.exists()){
                sampleStoreFile.delete();
            }
            sampleStoreFile.createNewFile();

            fileStore =new File(directory.getAbsolutePath() + File.separator +  "test.properties");
            if(fileStore.exists()){
                fileStore.delete();
            }
            fileStore.createNewFile();

            AssetManager assetManager = getAssets();
            InputStream publickey = assetManager.open("adminorg1/Admin@org1.example.com-cert.pem");
            InputStream privateKey = assetManager.open("adminorg1/e478ef0ddc475ae34a6826662268278f3ee5a23eeed9580fb3e9bb7af20523cc_sk");
            InputStream orderercert = assetManager.open("orderer/tlsca.example.com-cert.pem");
            InputStream channel = assetManager.open("artifacts/channel.tx");

            adminOrg1PublickKey = new File(directory.getAbsolutePath() + File.separator + "Admin@org1.example.com-cert.pem");
            OutputStream output = new FileOutputStream(adminOrg1PublickKey);
            writeToFile(publickey,output);

            adminOrg1PrivateKey = new File(directory.getAbsolutePath() + File.separator + "e478ef0ddc475ae34a6826662268278f3ee5a23eeed9580fb3e9bb7af20523cc_sk");
            OutputStream output1 = new FileOutputStream(adminOrg1PrivateKey);
            writeToFile(privateKey,output1);

            ordererCert = new File(directory.getAbsolutePath() + File.separator + "tlsca.example.com-cert.pem");
            OutputStream output2 = new FileOutputStream(ordererCert);
            writeToFile(orderercert,output2);

            channelConfig = new File(directory.getAbsolutePath() + File.separator + "channel.tx");
            OutputStream output3 = new FileOutputStream(channelConfig);
            writeToFile(channel,output3);

            InputStream chaincodefile = assetManager.open("chaincode/example.go");
            chaincode = new File(directory.getAbsolutePath() + File.separator +"tempfolder"+
                    File.separator+"src"+File.separator+"github.com"+File.separator+"example_cc"+File.separator+"chaincode.go");
            System.out.println("Creating dir: "+chaincode.getParentFile().mkdirs());
            OutputStream cc = new FileOutputStream(chaincode);
            writeToFile(chaincodefile,cc);

            InputStream peerca = assetManager.open("org1/tlsca.org1.example.com-cert.pem");

            peerRootCa =  new File(directory.getAbsolutePath() + File.separator + "tlsca.org1.example.com-cert.pem");
            System.out.println("peer root ca: "+peerRootCa.isFile());
            OutputStream prca = new FileOutputStream(peerRootCa);
            writeToFile(peerca,prca);

        }catch(Exception e){
            System.out.println("Exception (Create File): "+e.getMessage());
        }
    }

    private void joinchannel(){

        try {

            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg(TEST_WITH_INTEGRATION_ORG);
            client.setUserContext(sampleOrg.getPeerAdmin());
            Channel channel = client.newChannel("mychannel");
            for (String orderName : sampleOrg.getOrdererNames()) {

                Properties ordererProperties = testConfig.getOrdererProperties(orderName);
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});

                System.out.println("sampleOrg: "+sampleOrg.getOrdererLocation(orderName));
                Orderer orderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),ordererProperties);
                channel.addOrderer(orderer);
            }
            for (String peerName : sampleOrg.getPeerNames()) {

                System.out.println("Peer Name: " + peerName);
                String peerLocation = sampleOrg.getPeerLocation(peerName);
                System.out.println("Peer Location: " + peerLocation);
                Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
                if (peerProperties == null) {
                    peerProperties = new Properties();
                }
                //Example of setting specific options on grpc's NettyChannelBuilder
                peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

                Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
                channel.joinPeer(peer, createPeerOptions()); //Default is all roles.

            }
        }catch(Exception e){
                e.printStackTrace();
        }
    }

    public void writeToFile(InputStream input, OutputStream output){

        try {
            byte[] buffer = new byte[4 * 1024]; // or other buffer size
            int read;

            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }catch(Exception e){
            System.out.println("Exception in writing files: "+e.getMessage());
        } finally{
            try{
                output.close();
                input.close();
            }catch(IOException e){
                System.out.println("IOException in closing files: "+e.getMessage());
            }

        }
    }

    public void createChannel(final SampleStore sampleStore) throws Exception {


        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg(TEST_WITH_INTEGRATION_ORG);
        Channel fooChannel = constructChannel("mychannel", client, sampleOrg);
        try{
            sampleStore.saveChannel(fooChannel);
        }catch(Exception e){
                e.printStackTrace();
        }

    }

    Channel constructChannel(String name, HFClient client, SampleOrg sampleOrg) throws Exception {

        client.setUserContext(sampleOrg.getPeerAdmin());

        ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelConfig);
        System.out.println("channel Configuration: "+channelConfiguration.getChannelConfigurationAsBytes());

        Collection<Orderer> orderers = new LinkedList<>();
        for (String orderName : sampleOrg.getOrdererNames()) {

            System.out.println("Orderer name: "+orderName);
            Properties ordererProperties = testConfig.getOrdererProperties(orderName);

            if(ordererProperties == null){
                System.out.println("Orderer properties is null");
            }
            if (!clientTLSProperties.isEmpty()) {
                ordererProperties.putAll(clientTLSProperties.get(sampleOrg.getName()));
            }
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});

            System.out.println("sampleOrg: "+sampleOrg.getOrdererLocation(orderName));
            orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),ordererProperties));
        }

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);
        System.out.println("Orderer: "+anOrderer.getUrl());
        Channel newChannel = client.newChannel(name, anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, sampleOrg.getPeerAdmin()));
        System.out.println("Channel Created: "+ newChannel.getName());
        return newChannel;

    }

    public void installChaincode(final SampleStore sampleStore){

        System.out.println("****Installing chaincode*****");

        try{

            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg(TEST_WITH_INTEGRATION_ORG);
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            client.setUserContext(sampleOrg.getPeerAdmin());
            ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME).setVersion(CHAIN_CODE_VERSION).setPath(CHAIN_CODE_PATH).build();
            InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);
            File folder = new File(directory.getAbsolutePath() + File.separator +"tempfolder"+
                    File.separator+"src"+File.separator+"github.com"+File.separator+"example_cc");
            System.out.println("Example cc folder exits: "+folder.isDirectory());
            File file =  Paths.get(directory.getAbsolutePath() + File.separator +"tempfolder", "src", CHAIN_CODE_PATH).toFile();
            System.out.println("example cc file: "+file.isDirectory());

            InputStream inputStream = generateTarGzInputStream(file, Paths.get("src", CHAIN_CODE_PATH).toString());
            //installProposalRequest.setChaincodeSourceLocation(file);
            installProposalRequest.setChaincodeInputStream(inputStream);
            installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);

            int numInstallProposal = 0;
            Collection<Peer> peers = new ArrayList<Peer>();
            for (String peerName : sampleOrg.getPeerNames()) {

                System.out.println("Peer Name: "+peerName);
                String peerLocation = sampleOrg.getPeerLocation(peerName);
                System.out.println("Peer Location: "+peerLocation);
                Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
                if (peerProperties == null) {
                    peerProperties = new Properties();
                }
                //Example of setting specific options on grpc's NettyChannelBuilder
                peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

                Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
                peers.add(peer);

            }
            numInstallProposal = numInstallProposal + peers.size();
            Collection<ProposalResponse> responses = client.sendInstallProposal(installProposalRequest, peers);

            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            for (ProposalResponse response : responses) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    System.out.println("Sucessfully installed chaincode "+ response.getTransactionID()+" on "+response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            if (failed.size() > 0) {

                ProposalResponse first = failed.iterator().next();
                System.err.println("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static InputStream generateTarGzInputStream(File src, String pathPrefix) throws IOException {
        File sourceDirectory = src;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(500000);

        String sourcePath = sourceDirectory.getAbsolutePath();

        TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(bos)));
        archiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        try {
            Collection<File> childrenFiles = org.apache.commons.io.FileUtils.listFiles(sourceDirectory, null, true);

            ArchiveEntry archiveEntry;
            FileInputStream fileInputStream;
            for (File childFile : childrenFiles) {
                String childPath = childFile.getAbsolutePath();
                String relativePath = childPath.substring((sourcePath.length() + 1), childPath.length());

                if (pathPrefix != null) {
                    relativePath = Utils.combinePaths(pathPrefix, relativePath);
                }

                relativePath = FilenameUtils.separatorsToUnix(relativePath);

                archiveEntry = new TarArchiveEntry(childFile, relativePath);
                fileInputStream = new FileInputStream(childFile);
                archiveOutputStream.putArchiveEntry(archiveEntry);

                try {
                    IOUtils.copy(fileInputStream, archiveOutputStream);
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                    archiveOutputStream.closeArchiveEntry();
                }
            }
        } finally {
            IOUtils.closeQuietly(archiveOutputStream);
        }

        return new ByteArrayInputStream(bos.toByteArray());
    }

    public void instantiateChaincode(SampleStore sampleStore){


        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        try {

            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg(TEST_WITH_INTEGRATION_ORG);
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            client.setUserContext(sampleOrg.getPeerAdmin());
            Channel channel = client.newChannel("mychannel");
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(120000);
            ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                    .setVersion(CHAIN_CODE_VERSION)
                    .setPath(CHAIN_CODE_PATH).build();
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[] {"a", "500", "b", "1000" });
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes());
            tm.put("method", "InstantiateProposalRequest".getBytes());
            instantiateProposalRequest.setTransientMap(tm);

            Collection<Peer> peers = new ArrayList<Peer>();
            for (String peerName : sampleOrg.getPeerNames()) {

                System.out.println("Peer Name: "+peerName);
                String peerLocation = sampleOrg.getPeerLocation(peerName);
                System.out.println("Peer Location: "+peerLocation);
                Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
                if (peerProperties == null) {
                    peerProperties = new Properties();
                }
                //Example of setting specific options on grpc's NettyChannelBuilder
                peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

                Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
                channel.addPeer(peer);
                peers.add(peer);

            }
            System.out.println("Channel Peers: "+channel.getPeers());
            channel = channel.initialize();
            responses = channel.sendInstantiationProposal(instantiateProposalRequest, peers);
            for (ProposalResponse response : responses) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    System.out.println("Successful install proposal response Txid: "+response.getTransactionID() +" on "+response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                System.err.println("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
            }

            Collection<Orderer> orderers = new LinkedList<>();
            for (String orderName : sampleOrg.getOrdererNames()) {

                Properties ordererProperties = testConfig.getOrdererProperties(orderName);

                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});

                if (!clientTLSProperties.isEmpty()) {
                    ordererProperties.putAll(clientTLSProperties.get(sampleOrg.getName()));
                }
                Orderer orderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),ordererProperties);
                channel.addOrderer(orderer);
                orderers.add(orderer);
            }
            System.out.println("Sending intantiate transaction to orderer");
            channel.sendTransaction(successful, orderers).thenApply(transactionEvent -> {

                System.out.println("Transaction status from Orderer: "+transactionEvent.isValid());
                return null;
            });

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void invokeTransaction(SampleStore sampleStore){

        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        try{

            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg(TEST_WITH_INTEGRATION_ORG);
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            client.setUserContext(sampleOrg.getPeerAdmin());
            Channel channel = client.newChannel("mychannel");
            ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                    .setVersion(CHAIN_CODE_VERSION)
                    .setPath(CHAIN_CODE_PATH).build();

            Collection<Peer> peers = new ArrayList<Peer>();
            for (String peerName : sampleOrg.getPeerNames()) {

                System.out.println("Peer Name: "+peerName);
                String peerLocation = sampleOrg.getPeerLocation(peerName);
                System.out.println("Peer Location: "+peerLocation);
                Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
                if (peerProperties == null) {
                    peerProperties = new Properties();
                }
                //Example of setting specific options on grpc's NettyChannelBuilder
                peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

                Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
                channel.addPeer(peer);
                peers.add(peer);

            }
            System.out.println("Channel Peers: "+channel.getPeers());
            channel = channel.initialize();

            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            transactionProposalRequest.setFcn("move");
            transactionProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
            transactionProposalRequest.setArgs("a", "b", "100");

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes());
            tm2.put("method", "TransactionProposalRequest".getBytes());
            tm2.put("result", ":)".getBytes());

            transactionProposalRequest.setTransientMap(tm2);
            Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
            for (ProposalResponse response : transactionPropResp) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    System.out.println("Successful transaction proposal response Txid: "+response.getTransactionID() + " from peer " +response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
            if (proposalConsistencySets.size() != 1) {
                System.err.println("Expected only one set of consistent proposal responses but got "+proposalConsistencySets.size());
            }

            if (failed.size() > 0) {
                ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                System.err.println("Not enough endorsers for invoke (move a,b,100):" + failed.size() + " endorser error: " +
                        firstTransactionProposalResponse.getMessage() +
                        ". Was verified: " + firstTransactionProposalResponse.isVerified());
            }
            ProposalResponse resp = successful.iterator().next();
            byte[] x = resp.getChaincodeActionResponsePayload(); // This is the data returned by the chaincode.
            String resultAsString = null;
            if (x != null) {
                resultAsString = new String(x, "UTF-8");
            }
            System.out.println("Result returned from chaincode: "+resultAsString);
            System.out.println("Chaincode Status: "+resp.getChaincodeActionResponseStatus());
            System.out.println("Chaincode ID: "+resp.getChaincodeID());
            System.out.println("Chaincode PATH: "+resp.getChaincodeID().getPath());
            System.out.println("Chaincode NAME: "+resp.getChaincodeID().getName());
            System.out.println("Chaincode Version: "+resp.getChaincodeID().getVersion());

            Collection<Orderer> orderers = new LinkedList<>();
            for (String orderName : sampleOrg.getOrdererNames()) {

                Properties ordererProperties = testConfig.getOrdererProperties(orderName);

                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});

                if (!clientTLSProperties.isEmpty()) {
                    ordererProperties.putAll(clientTLSProperties.get(sampleOrg.getName()));
                }
                Orderer orderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),ordererProperties);
                channel.addOrderer(orderer);
                orderers.add(orderer);
            }

            channel.sendTransaction(successful,orderers).thenApply(transactionEvent -> {
                System.out.print("Response <transactionEvent>: "+transactionEvent.isValid());
                System.out.print("Response <transactionEvent TXID >: "+transactionEvent.getTransactionID());
                return null;
            });



        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void queryTransaction(SampleStore sampleStore){

        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        try {

            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg(TEST_WITH_INTEGRATION_ORG);
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            client.setUserContext(sampleOrg.getPeerAdmin());
            Channel channel = client.newChannel("mychannel");
            ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                    .setVersion(CHAIN_CODE_VERSION)
                    .setPath(CHAIN_CODE_PATH).build();

            Collection<Peer> peers = new ArrayList<Peer>();
            for (String peerName : sampleOrg.getPeerNames()) {

                System.out.println("Peer Name: " + peerName);
                String peerLocation = sampleOrg.getPeerLocation(peerName);
                System.out.println("Peer Location: " + peerLocation);
                Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
                if (peerProperties == null) {
                    peerProperties = new Properties();
                }
                //Example of setting specific options on grpc's NettyChannelBuilder
                peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

                Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
                channel.addPeer(peer);
                peers.add(peer);

            }
            System.out.println("Channel Peers: " + channel.getPeers());
            channel = channel.initialize();

            QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(new String[] {"b"});
            queryByChaincodeRequest.setFcn("query");
            queryByChaincodeRequest.setChaincodeID(chaincodeID);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes());
            tm2.put("method", "QueryByChaincodeRequest".getBytes());
            queryByChaincodeRequest.setTransientMap(tm2);

            Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
            for (ProposalResponse proposalResponse : queryProposals) {
                if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                    System.err.println("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                            ". Messages: " + proposalResponse.getMessage()
                            + ". Was verified : " + proposalResponse.isVerified());
                } else {

                    String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                    System.out.println("Query payload of b from peer "+proposalResponse.getPeer().getName()+ "is : "+payload);
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private class Task extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                if (!admin.isEnrolled()) {

                    // Preregistered admin only needs to be enrolled with Fabric CA.
                    System.out.println("Enrolling admin!!");
                    admin.setEnrollment(client.enroll(admin.getName(), TEST_ADMIN_PW));
                    admin.setMspId(org1.getMSPID());
                    System.out.println("Admin enrolled Sucessfully!!");

                }
                org1.setAdmin(admin);
                SampleUser user = new SampleUser("shubhamvrkr", TEST_ADMIN_ORG, sampleStore);
                RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
                rr.setSecret(password);
                user.setEnrollmentSecret(client.register(rr, admin));
                if (!user.getEnrollmentSecret().equals(password)) {
                    System.out.print("Secret returned from RegistrationRequest not match : " + user.getEnrollmentSecret());
                }
                user.setEnrollment(client.enroll(user.getName(), user.getEnrollmentSecret()));
                user.setMspId(org1.getMSPID());
                org1.addUser(user);
                System.out.println("Shubhamvrkr enrolled Sucessfully!!");

            } catch (Exception e) {

            }
            return null;
        }
    }

}
