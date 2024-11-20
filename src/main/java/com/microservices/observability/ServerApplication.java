package com.microservices.observability;

import java.util.Random;
import java.util.stream.StreamSupport;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

}

@RestController
class MyController {
	private static final Logger log = LoggerFactory.getLogger(MyController.class);
	private final MyUserService myUserService;

	MyController(MyUserService myUserService) {
		this.myUserService = myUserService;
	}

	@GetMapping("/users/{userId}")
	String userName(@PathVariable("userId") String userId) {
		log.info("Got a request");
		return myUserService.userName(userId);
	}
}

@Service
class MyUserService {

	private static final Logger log = LoggerFactory.getLogger(MyUserService.class);

	private final Random random = new Random();

	@Observed(
		name = "user.name",
		contextualName = "getUserName",
		lowCardinalityKeyValues = { "userType", "admin" })
	String userName(String userId) {
		log.info("Utilisateur <{}>", userId);
		try {
			Thread.sleep(random.nextLong(200L));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return "Id = " + userId;
	}
}

@Component
class MonHandler implements ObservationHandler<Observation.Context> {
	private static final Logger log = LoggerFactory.getLogger(MonHandler.class);

	@Override
	public void onStart(Observation.Context context) {
		log.info("Avant l'observation pour le contexte [{}], userType [{}]", context.getName(), getUserTypeFromContext(context));
	}

	@Override
	public void onStop(Observation.Context context) {
		log.info("Apres l'observation pour le contexte [{}], userType [{}]", context.getName(), getUserTypeFromContext(context));
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return true;
	}

	private String getUserTypeFromContext(Observation.Context context) {
		return StreamSupport.stream(context.getLowCardinalityKeyValues().spliterator(), false)
				.filter(keyValue -> "userType".equals(keyValue.getKey()))
				.map(KeyValue::getValue)
				.findFirst()
				.orElse("UNKNOWN");
	}
}
