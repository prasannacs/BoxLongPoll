package com.box.longpoll;

import java.util.concurrent.CompletableFuture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BoxLongPollApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(BoxLongPollApplication.class, args);
		if( args[0] == null )	{
			System.out.println("Please feed in an Authorization Developer Token from Box ..");
			System.exit(0);
		}
		String token = args[0].substring(6);
		System.out.println("Authorization Token -- "+token);
		callBoxAPI(token);
	}
	
	private static void callBoxAPI(String token) throws Exception	{
		BoxAPIFactory boxApi = new BoxAPIFactory();
		String current_stream_position = boxApi.getCurrentStreamPosition(token);
		String long_poll_url = boxApi.getLongPollURL(token);
//		String message = boxApi.startLongPoll(current_stream_position, long_poll_url, token);
		CompletableFuture<String> message = boxApi.startLongPoll(current_stream_position, long_poll_url, token);
		System.out.println(message.get());
		if(message != null && message.equals("reconnect"))
			callBoxAPI(token);
		String eventDetails = boxApi.getEventDetails(current_stream_position, token);
		System.out.println(eventDetails);
		if(message != null)
			callBoxAPI(token);
		
	}
}
