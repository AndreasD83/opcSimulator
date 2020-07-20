package de.winkhaus.opcSimulator;

import de.winkhaus.opcSimulator.thread.IncrementCounterThread;
import de.winkhaus.opcSimulator.controller.MachineBuilderService;
import de.winkhaus.opcSimulator.jpa.MachineRepository;
import de.winkhaus.opcSimulator.opc.MiloServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class OpcSimulatorApplication {
	private final Logger logger = LoggerFactory.getLogger(getClass());


	protected OpcSimulatorApplication(){
	}

	@Autowired
	MachineBuilderService machineBuilderService;

	@Autowired
	MiloServer server;

	@Autowired
	IncrementCounterThread thread;

	@Bean
	public CommandLineRunner create(MachineRepository repository) {
		return (args) -> {
			opcServer();
		};
	}

	private void opcServer(){
		try {
			thread.start();
			logger.info("start opc server...");
			server.startup();
			final CompletableFuture<Void> future = new CompletableFuture<>();
			Runtime.getRuntime().addShutdownHook(new Thread(() ->
					future.complete(null)));
			future.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	public static void main(String[] args) throws Exception {
		//IncrementCounterThread thread = new IncrementCounterThread();

		SpringApplication.run(OpcSimulatorApplication.class, args);


	}

}
