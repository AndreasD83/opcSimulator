package de.winkhaus.opcSimulator.opc;

import de.winkhaus.opcSimulator.jpa.MachineRepository;
import de.winkhaus.opcSimulator.model.Machine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedHttpsCertificateBuilder;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.*;

@Service
public class MiloServer {
    private static String SUBURL = "server";
    private static String SUBURLDISCOVERY = "discovery";
    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }


    private MachineRepository repository;

    //private OpcUaServer server;
    private List<OpcUaServer> serverList;

    private OpcUaServer build(Machine machine) throws Exception {

        File securityTempDir = new File(System.getProperty
                ("java.io.tmpdir"), "security");
        if (!securityTempDir.exists() && !securityTempDir.mkdirs()) {
            throw new Exception("unable to create security temp dir: " +
                    securityTempDir);
        }
        LoggerFactory.getLogger(getClass()).info("security temp dir: {}",
                securityTempDir.getAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        DefaultCertificateManager certificateManager = new
                DefaultCertificateManager(
                loader.getServerKeyPair(),
                loader.getServerCertificateChain()
        );
        File pkiDir = securityTempDir.toPath().resolve("pki").toFile();
        DefaultTrustListManager trustListManager = new
                DefaultTrustListManager(pkiDir);
        LoggerFactory.getLogger(getClass()).info("pki dir: {}",
                pkiDir.getAbsolutePath());

        DefaultCertificateValidator certificateValidator = new
                DefaultCertificateValidator(trustListManager);

        KeyPair httpsKeyPair =
                SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

        SelfSignedHttpsCertificateBuilder httpsCertificateBuilder = new
                SelfSignedHttpsCertificateBuilder(httpsKeyPair);
        httpsCertificateBuilder.setCommonName(HostnameUtil.getHostname());
        HostnameUtil.getHostnames("0.0.0.0").forEach
                (httpsCertificateBuilder::addDnsName);
        X509Certificate httpsCertificate = httpsCertificateBuilder.build();

        UsernameIdentityValidator identityValidator = new
                UsernameIdentityValidator(
                true,
                authChallenge -> {
                    String username = authChallenge.getUsername();
                    String password = authChallenge.getPassword();

                    boolean userOk = "user".equals(username) &&
                            "password1".equals(password);
                    boolean adminOk = "admin".equals(username) &&
                            "password2".equals(password);

                    return userOk || adminOk;
                }
        );

        X509IdentityValidator x509IdentityValidator = new
                X509IdentityValidator(c -> true);

        // If you need to use multiple certificates you'll have to be
        //smarter than this.
        X509Certificate certificate = certificateManager.getCertificates()
                .stream()
                .findFirst()
                .orElseThrow(() -> new UaRuntimeException
                        (StatusCodes.Bad_ConfigurationError, "no certificate found"));

        // The configured application URI must match the one in the
        //certificate(s)
        String applicationUri = CertificateUtil
                .getSanUri(certificate)
                .orElseThrow(() -> new UaRuntimeException(
                        StatusCodes.Bad_ConfigurationError,
                        "certificate is missing the application URI"));




        Set<EndpointConfiguration> endpointConfigurations =
                createEndpointConfigurations(certificate, machine);

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
                .setApplicationUri(applicationUri)
                .setApplicationName(LocalizedText.english(String.format("machine {}",machine)))
                .setEndpoints(endpointConfigurations)
                .setBuildInfo(
                        new BuildInfo(
                                "urn:eclipse:milo:opc",
                                "winkhaus",
                                "eclipse milo opc server",
                                OpcUaServer.SDK_VERSION,
                                "", DateTime.now()))
                .setCertificateManager(certificateManager)
                .setTrustListManager(trustListManager)
                .setCertificateValidator(certificateValidator)
                .setHttpsKeyPair(httpsKeyPair)
                .setHttpsCertificate(httpsCertificate)
                .setIdentityValidator(new CompositeValidator(identityValidator,
                        x509IdentityValidator))
                .setProductUri("urn:eclipse:milo:opc")
                .build();

        OpcUaServer server = new OpcUaServer(serverConfig);

        SpsPlantNamespace spsMuensterNamespace = new SpsPlantNamespace(server, repository, machine.getMachineId());
        spsMuensterNamespace.startup();

        return server;
    }

    @Autowired
    public MiloServer(MachineRepository repository) throws Exception {
        this.repository = repository;
        this.serverList = new ArrayList<OpcUaServer>();
        this.repository.findAll().forEach( machine -> {
            try {
                this.serverList.add(this.build(machine));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


    }
    public void setRepository(MachineRepository repository) {
        this.repository = repository;
    }

    private Set<EndpointConfiguration> createEndpointConfigurations
            (X509Certificate certificate, Machine machine) {
        Set<EndpointConfiguration> endpointConfigurations = new
                LinkedHashSet<>();

        List<String> bindAddresses = newArrayList();
        bindAddresses.add("0.0.0.0");

        Set<String> hostnames = new LinkedHashSet<>();
        hostnames.add(HostnameUtil.getHostname());
        hostnames.addAll(HostnameUtil.getHostnames("0.0.0.0"));

        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder =
                        EndpointConfiguration.newBuilder()
                                .setBindAddress(bindAddress)
                                .setHostname(hostname)
                                .setPath(String.format("%s/%s", SUBURL, machine.getMachineId()))
                                .setCertificate(certificate)
                                .addTokenPolicies(
                                        USER_TOKEN_POLICY_ANONYMOUS,
                                        USER_TOKEN_POLICY_USERNAME,
                                        USER_TOKEN_POLICY_X509);


                EndpointConfiguration.Builder noSecurityBuilder =
                        builder.copy()
                                .setSecurityPolicy(SecurityPolicy.None)
                                .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint
                        (noSecurityBuilder, machine.getPort()));
                endpointConfigurations.add(buildHttpsEndpoint
                        (noSecurityBuilder, machine.getHttpsPort()));

                // TCP Basic256Sha256 / SignAndEncrypt
                endpointConfigurations.add(buildTcpEndpoint(
                        builder.copy()
                                .setSecurityPolicy(SecurityPolicy.Basic256Sha256)
                                .setSecurityMode
                                        (MessageSecurityMode.SignAndEncrypt), machine.getPort())
                );

                // HTTPS Basic256Sha256 / Sign (SignAndEncrypt not allowed for HTTPS)
                endpointConfigurations.add(buildHttpsEndpoint(
                        builder.copy()
                                .setSecurityPolicy(SecurityPolicy.Basic256Sha256)
                                .setSecurityMode(MessageSecurityMode.Sign), machine.getHttpsPort())
                );

                EndpointConfiguration.Builder discoveryBuilder =
                        builder.copy()
                                .setPath(String.format("%s/%s", SUBURLDISCOVERY, machine.getMachineId()))
                                .setSecurityPolicy(SecurityPolicy.None)
                                .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint
                        (discoveryBuilder, machine.getPort()));
                endpointConfigurations.add(buildHttpsEndpoint
                        (discoveryBuilder, machine.getHttpsPort()));
            }
        }

        return endpointConfigurations;
    }

    private static EndpointConfiguration buildTcpEndpoint
            (EndpointConfiguration.Builder base, Integer port) {
        return base.copy()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindPort(port.intValue())
                .build();
    }

    private static EndpointConfiguration buildHttpsEndpoint
            (EndpointConfiguration.Builder base, Integer httpsPort) {
        return base.copy()
                .setTransportProfile(TransportProfile.HTTPS_UABINARY)
                .setBindPort(httpsPort.intValue())
                .build();
    }

    public OpcUaServer getServer() {
        return this.serverList.get(0);
    }

       public void startup() {
        this.serverList.forEach(server -> server.startup());
    }

    public void shutdown() {
        this.serverList.forEach(server -> server.shutdown());
    }
}


